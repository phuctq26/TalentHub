package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.dto.UserRegisterDto;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.service.AuthService;
import com.talenthub.recruitment.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/login")
    public String showLoginPage(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return redirectToDashboard((User) session.getAttribute("currentUser"));
        }
        return "auth/login";
    }
    // ── POST /login ── Xử lý đăng nhập
    @PostMapping("/login")
    public String processLogin(
            @RequestParam("usernameOrEmail") String usernameOrEmail,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {
        AuthService.UserHolder holder = new AuthService.UserHolder();
        AuthService.LoginResult result = authService.login(usernameOrEmail, password, holder);
        switch (result) {
            case SUCCESS:
                User loggedInUser = holder.getUser();
                session.setAttribute("currentUser", loggedInUser);
                session.setAttribute("currentRole", loggedInUser.getRole().getName());
                session.setMaxInactiveInterval(30 * 60); // 30 phút timeout
                return redirectToDashboard(loggedInUser);
            case ACCOUNT_LOCKED:
                model.addAttribute("lockoutError", true);
                return "auth/login";
            case INVALID_CREDENTIALS:
            case ACCOUNT_INACTIVE:
            default:
                model.addAttribute("genericError", true);
                return "auth/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private String redirectToDashboard(User user) {
        String roleName = user.getRole().getName().toUpperCase();
        switch (roleName) {
            case "ADMIN":        return "redirect:/admin/dashboard";      // SCR-07
            case "HR_MANAGER":   return "redirect:/hr/dashboard";         // SCR-06
            case "INTERVIEWER":  return "redirect:/interviewer/applications"; // SCR-17
            case "CANDIDATE":    return "redirect:/candidate/applications";   // SCR-15
            default:           return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("userRegisterDto")) {
            model.addAttribute("userRegisterDto", new UserRegisterDto());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("userRegisterDto") UserRegisterDto dto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.userRegisterDto", "Passwords do not match.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerCandidate(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Account created successfully. Please login.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Username")) {
                bindingResult.rejectValue("username", "error.userRegisterDto", "This username is already taken. Please choose another.");
            } else if (e.getMessage().contains("Email")) {
                bindingResult.rejectValue("email", "error.userRegisterDto", "This email address is already registered.");
            } else {
                model.addAttribute("errorMessage", e.getMessage());
            }
            return "auth/register";
        }
    }
}
