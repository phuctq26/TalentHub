package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.entity.enums.UserRole;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── GET /login ── Hiển thị form đăng nhập
    @GetMapping("/login")
    public String showLoginPage(HttpSession session) {
        // Nếu đã đăng nhập rồi → redirect về dashboard
        if (session.getAttribute("currentUser") != null) {
            return redirectToDashboard((User) session.getAttribute("currentUser"));
        }
        return "auth/login"; // templates/auth/login.html
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
                // Lưu user vào session — đây là "đăng nhập" của chúng ta
                session.setAttribute("currentUser", loggedInUser);
                session.setAttribute("currentRole", loggedInUser.getRole().name());
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

    // ── GET /logout ──
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // xóa toàn bộ session
        return "redirect:/login";
    }

    // ── Phân quyền redirect theo Role ──
    private String redirectToDashboard(User user) {
        switch (user.getRole()) {
            case ADMIN:        return "redirect:/admin/dashboard";      // SCR-07
            case HR_MANAGER:   return "redirect:/hr/dashboard";         // SCR-06
            case INTERVIEWER:  return "redirect:/interviewer/applications"; // SCR-17
            case CANDIDATE:    return "redirect:/candidate/applications";   // SCR-15
            default:           return "redirect:/login";
        }
    }
}