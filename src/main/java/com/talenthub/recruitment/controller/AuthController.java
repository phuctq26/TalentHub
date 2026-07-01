package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.dto.UserRegisterDto;
import com.talenthub.recruitment.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
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
