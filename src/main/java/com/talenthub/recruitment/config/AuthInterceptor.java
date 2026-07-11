package com.talenthub.recruitment.config;

import com.talenthub.recruitment.entity.enums.UserRole;
import com.talenthub.recruitment.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {
        String path = request.getRequestURI();
        // Các đường dẫn công khai — không cần đăng nhập
        if (isPublicPath(path)) {
            return true; // cho đi tiếp
        }
        HttpSession session = request.getSession(false);
        User currentUser = (session != null)
                ? (User) session.getAttribute("currentUser")
                : null;
        // Chưa đăng nhập → về trang login
        if (currentUser == null) {
            response.sendRedirect("/login");
            return false;
        }
        // Kiểm tra phân quyền theo prefix URL
        String roleName = currentUser.getRole().getName();
        if (!hasPermission(UserRole.valueOf(roleName), path)) {
            response.sendError(403, "Forbidden");
            return false;
        }
        return true; // có quyền → cho đi tiếp
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/login")
                || path.startsWith("/logout")
                || path.startsWith("/register")
                || path.startsWith("/forgot-password")
                || path.startsWith("/verify-otp")
                || path.startsWith("/resend-otp")
                || path.startsWith("/reset-password")
                || path.startsWith("/error")
                || path.startsWith("/jobs") // Public Job List SCR-13
                || path.startsWith("/css")
                || path.startsWith("/js")
                || path.startsWith("/images");
    }

    private boolean hasPermission(UserRole role, String path) {
        switch (role) {
            case ADMIN:
                return true; // Admin truy cập mọi thứ
            case HR_MANAGER:
                return path.startsWith("/hr") || path.startsWith("/jobs");
            case INTERVIEWER:
                return path.startsWith("/interviewer");
            case CANDIDATE:
                return path.startsWith("/candidate") || path.startsWith("/jobs");
            default:
                return false;
        }
    }
}