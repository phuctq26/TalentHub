package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.dto.ChangePasswordForm;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChangePasswordController {

    private final UserService userService;

    public ChangePasswordController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/change-password")
    public String changePasswordPage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        ChangePasswordForm form = new ChangePasswordForm();
        form.setUsername(currentUser.getUsername());
        model.addAttribute("changePasswordForm", form);
        model.addAttribute("currentUsername", currentUser.getUsername());
        return "admin/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
            HttpSession session,
            @Valid @ModelAttribute("changePasswordForm") ChangePasswordForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Luôn dùng username từ session, không tin vào input từ form
        form.setUsername(currentUser.getUsername());
        model.addAttribute("currentUsername", currentUser.getUsername());

        if (result.hasErrors()) {
            return "admin/change-password";
        }

        try {
            userService.changePassword(form);
            redirectAttributes.addFlashAttribute("successMessage", "Mật khẩu đã được thay đổi thành công.");
            return "redirect:/change-password?success=true";
        } catch (Exception e) {
            String msg = e.getMessage();
            if ("Mật khẩu hiện tại không chính xác.".equals(msg)) {
                result.rejectValue("oldPassword", "error.invalid", msg);
            } else if ("Mật khẩu xác nhận không khớp.".equals(msg)) {
                result.rejectValue("confirmPassword", "error.mismatch", msg);
            } else if ("Mật khẩu mới phải khác mật khẩu hiện tại.".equals(msg)) {
                result.rejectValue("newPassword", "error.same", msg);
            } else {
                result.reject(null, msg);
            }
            return "admin/change-password";
        }
    }
}
