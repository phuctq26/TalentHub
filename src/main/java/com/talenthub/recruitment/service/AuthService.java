package com.talenthub.recruitment.service;

import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.AccountStatus;
import com.talenthub.recruitment.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 10;

    // Dùng BCrypt để hash/verify password (không cần Spring Security)
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserRepository userRepository;

    // Constructor Injection (không dùng @Autowired trực tiếp trên field)
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Kết quả trả về sau khi login
     */
    public enum LoginResult {
        SUCCESS,
        INVALID_CREDENTIALS, // sai username/password
        ACCOUNT_LOCKED, // tài khoản đang bị khóa
        ACCOUNT_INACTIVE // tài khoản bị vô hiệu hóa bởi Admin
    }

    /**
     * Xử lý đăng nhập:
     * 1. Tìm user theo username hoặc email
     * 2. Kiểm tra khóa tài khoản
     * 3. Verify password
     * 4. Reset/tăng failed attempts
     */
    @Transactional
    public LoginResult login(String usernameOrEmail, String rawPassword,
            UserHolder userHolder) {
        // Tìm user bằng cả username lẫn email
        Optional<User> optUser = userRepository
                .findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);

        if (optUser.isEmpty()) {
            return LoginResult.INVALID_CREDENTIALS;
        }

        User user = optUser.get();
        userHolder.setUser(user);

        // Kiểm tra trạng thái tài khoản
        if (user.getStatus() == AccountStatus.LOCKED) {
            return LoginResult.ACCOUNT_LOCKED;
        }
        if (user.getStatus() == AccountStatus.INACTIVE) {
            return LoginResult.ACCOUNT_INACTIVE;
        }

        // Kiểm tra đang bị khóa tạm thời
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            return LoginResult.ACCOUNT_LOCKED;
        }

        // Verify mật khẩu
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            handleFailedAttempt(user);
            // Sau khi tăng failed attempts, kiểm tra xem có vừa bị khóa không
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
                return LoginResult.ACCOUNT_LOCKED;
            }
            return LoginResult.INVALID_CREDENTIALS;
        }

        // Đăng nhập thành công → reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        userHolder.setUser(user); // trả user ra ngoài để Controller lưu vào Session
        return LoginResult.SUCCESS;
    }

    private void handleFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            // Khóa tài khoản 10 phút
            user.setLockedUntil(Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
        }
        userRepository.save(user);
    }

    /**
     * Helper class để truyền User ra ngoài từ login()
     * (Java không có out/ref params như C#)
     */
    public static class UserHolder {
        private User user;

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }
    }

    /**
     * Bước 3 của forgot-password flow:
     * Hash mật khẩu mới bằng BCrypt, lưu DB, xóa sạch OTP.
     */
    @Transactional
    public void resetPassword(String email, String newRawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user: " + email));

        // Dùng lại BCrypt encoder có sẵn để hash mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));

        // Xóa OTP để không thể dùng lại
        user.setOtp(null);
        user.setExpTime(null);

        userRepository.save(user);
    }
}