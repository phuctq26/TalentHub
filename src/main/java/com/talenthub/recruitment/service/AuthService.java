package com.talenthub.recruitment.service;

import com.talenthub.recruitment.entity.enums.AccountStatus;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 10;

    private final UserRepository userRepository;

    // Constructor Injection
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Kết quả trả về sau khi login
     */
    public enum LoginResult {
        SUCCESS,
        INVALID_CREDENTIALS,  // sai username/password
        ACCOUNT_LOCKED,       // tài khoản đang bị khóa
        ACCOUNT_INACTIVE      // tài khoản bị vô hiệu hóa bởi Admin
    }

    /**
     * Xử lý đăng nhập
     */
    @Transactional
    public LoginResult login(String usernameOrEmail, String rawPassword, UserHolder userHolder) {

        // 1. Tìm user bằng cả username lẫn email
        Optional<User> optUser = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);

        if (optUser.isEmpty()) {
            return LoginResult.INVALID_CREDENTIALS;
        }

        User user = optUser.get();

        // 2. Kiểm tra tài khoản có bị vô hiệu hóa không (Dựa vào trường status)
        if (user.getStatus() != AccountStatus.ACTIVE) {
            return LoginResult.ACCOUNT_INACTIVE;
        }

        // 3. Kiểm tra xem tài khoản có đang trong thời gian bị khóa không (So sánh lockedUntil với hiện tại)
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            return LoginResult.ACCOUNT_LOCKED;
        }

        // 4. Verify mật khẩu bằng jBCrypt
        if (!BCrypt.checkpw(rawPassword, user.getPasswordHash())) {
            handleFailedAttempt(user);

            // Kiểm tra lại xem sau lần nhập sai này tài khoản có bị khóa luôn không
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
                return LoginResult.ACCOUNT_LOCKED;
            }
            return LoginResult.INVALID_CREDENTIALS;
        }

        // 5. Đăng nhập thành công → Reset failed attempts, gỡ khóa và cập nhật giờ đăng nhập
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now()); // Thêm dòng này để tận dụng trường lastLoginAt trong Entity
        userRepository.save(user);

        userHolder.setUser(user);
        return LoginResult.SUCCESS;
    }

    private void handleFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        // Nếu sai quá số lần quy định -> Khóa tài khoản bằng Instant
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
        }
        userRepository.save(user);
    }

    /**
     * Helper class để truyền User ra ngoài
     */
    public static class UserHolder {
        private User user;
        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
    }
}