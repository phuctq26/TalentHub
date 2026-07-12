package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.dto.UserRegisterDto;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.service.AuthService;
import com.talenthub.recruitment.service.PasswordResetService;
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
    private final PasswordResetService passwordResetService;

    public AuthController(UserService userService,
            AuthService authService,
            PasswordResetService passwordResetService) {
        this.userService = userService;
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    // ──────────────────────────────────────────────
    // Login / Logout
    // ──────────────────────────────────────────────

    @GetMapping("/login")
    public String showLoginPage(HttpSession session, Model model) {
        if (session.getAttribute("currentUser") != null) {
            return redirectToDashboard((User) session.getAttribute("currentUser"));
        }
        return "auth/login";
    }

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
                session.setMaxInactiveInterval(30 * 60);
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
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("successMessage", "You have been signed out.");
        return "redirect:/login";
    }

    // ──────────────────────────────────────────────
    // Bước 1: Nhập Email — Gửi OTP
    // ──────────────────────────────────────────────

    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam("email") String email,
            Model model,
            RedirectAttributes redirectAttributes) {

        PasswordResetService.SendOtpResult result = passwordResetService.sendOtp(email.trim().toLowerCase());

        if (result == PasswordResetService.SendOtpResult.EMAIL_NOT_FOUND) {
            model.addAttribute("errorMessage", "Email này chưa được đăng ký trong hệ thống.");
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }

        if (result == PasswordResetService.SendOtpResult.ACCOUNT_INACTIVE) {
            model.addAttribute("errorMessage",
                    "Tài khoản này đã bị vô hiệu hoá. Vui lòng liên hệ quản trị viên để được hỗ trợ.");
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }

        // OTP đã được gửi — chuyển sang trang nhập OTP
        redirectAttributes.addFlashAttribute("infoMessage",
                "Mã OTP đã được gửi đến " + email + ". Vui lòng kiểm tra hộp thư.");
        return "redirect:/verify-otp?email=" + email.trim().toLowerCase();
    }

    @GetMapping("/resend-otp")
    public String resendOtp(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {

        PasswordResetService.SendOtpResult result = passwordResetService.sendOtp(email.trim().toLowerCase());

        if (result == PasswordResetService.SendOtpResult.EMAIL_NOT_FOUND) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email này chưa được đăng ký trong hệ thống.");
            return "redirect:/forgot-password";
        }

        if (result == PasswordResetService.SendOtpResult.ACCOUNT_INACTIVE) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Tài khoản này đã bị vô hiệu hoá. Vui lòng liên hệ quản trị viên.");
            return "redirect:/forgot-password";
        }

        redirectAttributes.addFlashAttribute("infoMessage",
                "Mã OTP mới đã được gửi lại đến " + email + ". Vui lòng kiểm tra hộp thư.");
        return "redirect:/verify-otp?email=" + email.trim().toLowerCase();
    }


    // ──────────────────────────────────────────────
    // Bước 2: Nhập OTP — Xác minh
    // ──────────────────────────────────────────────

    @GetMapping("/verify-otp")
    public String showVerifyOtp(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String processVerifyOtp(
            @RequestParam("email") String email,
            @RequestParam("otp") String otp,
            HttpSession session,
            Model model) {

        PasswordResetService.VerifyOtpResult result = passwordResetService.verifyOtp(email, otp);

        switch (result) {
            case SUCCESS:
                // Lưu cờ vào session: người dùng đã xác minh OTP thành công
                session.setAttribute("resetPasswordEmail", email);
                return "redirect:/reset-password";

            case EXPIRED_OTP:
                model.addAttribute("errorMessage",
                        "Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại.");
                model.addAttribute("email", email);
                return "auth/verify-otp";

            case INVALID_OTP:
            case EMAIL_NOT_FOUND:
            default:
                model.addAttribute("errorMessage",
                        "Mã OTP không đúng. Vui lòng kiểm tra lại.");
                model.addAttribute("email", email);
                return "auth/verify-otp";
        }
    }

    // ──────────────────────────────────────────────
    // Bước 3: Đặt mật khẩu mới
    // ──────────────────────────────────────────────

    @GetMapping("/reset-password")
    public String showResetPassword(HttpSession session) {
        // Bảo vệ trang: phải đi qua xác minh OTP trước
        if (session.getAttribute("resetPasswordEmail") == null) {
            return "redirect:/forgot-password";
        }
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        String email = (String) session.getAttribute("resetPasswordEmail");

        // Kiểm tra session flag
        if (email == null) {
            return "redirect:/forgot-password";
        }

        // Kiểm tra hai mật khẩu có khớp không
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Mật khẩu xác nhận không khớp.");
            return "auth/reset-password";
        }

        // Kiểm tra độ dài tối thiểu
        if (newPassword.length() < 8) {
            model.addAttribute("errorMessage", "Mật khẩu phải có ít nhất 8 ký tự.");
            return "auth/reset-password";
        }

        // Hash mật khẩu mới + xóa OTP trong DB
        authService.resetPassword(email, newPassword);

        // Hủy toàn bộ session hiện tại (xóa cả currentUser nếu đang đăng nhập)
        // để đảm bảo người dùng được chuyển về trang đăng nhập, không tự đăng nhập lại
        session.invalidate();

        // Thông báo thành công và về trang đăng nhập
        redirectAttributes.addFlashAttribute("successMessage",
                "Đổi mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới.");
        return "redirect:/login";
    }

    // ──────────────────────────────────────────────
    // Register
    // ──────────────────────────────────────────────

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
                bindingResult.rejectValue("username", "error.userRegisterDto", "This username is already taken.");
            } else if (e.getMessage().contains("Email")) {
                bindingResult.rejectValue("email", "error.userRegisterDto",
                        "This email address is already registered.");
            } else {
                model.addAttribute("errorMessage", e.getMessage());
            }
            return "auth/register";
        }
    }

    // ──────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────

    private String redirectToDashboard(User user) {
        String roleName = user.getRole().getName().toUpperCase();
        switch (roleName) {
            case "ADMIN":
                return "redirect:/admin/dashboard"; // SCR-07
            case "HR_MANAGER":
                return "redirect:/hr/dashboard"; // SCR-06
            case "INTERVIEWER":
                return "redirect:/interviewer/applications"; // SCR-17
            case "CANDIDATE":
                return "redirect:/candidate/applications"; // SCR-15
            default:
                return "redirect:/login";
        }
    }
}
