package com.talenthub.recruitment.service;

import com.talenthub.recruitment.dto.ChangePasswordForm;
import com.talenthub.recruitment.dto.UserForm;
import com.talenthub.recruitment.dto.UserRegisterDto;
import com.talenthub.recruitment.entity.Role;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.AccountStatus;
import com.talenthub.recruitment.repository.RoleRepository;
import com.talenthub.recruitment.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public User registerCandidate(UserRegisterDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email address is already registered");
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        Role candidateRole = roleRepository.findByName("CANDIDATE")
                .orElseThrow(() -> new RuntimeException("Candidate role not found"));
        user.setRole(candidateRole);
        user.setStatus(AccountStatus.ACTIVE);

        return userRepository.save(user);
    }

    public Page<User> search(String keyword, Integer roleId, AccountStatus status, Pageable pageable) {
        String statusStr = (status == null) ? null : status.name();
        return userRepository.search(keyword, roleId, statusStr, pageable);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    public UserForm getFormById(Long id) {
        User user = findById(id);
        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setFullName(user.getFullName());
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        if (user.getRole() != null) {
            form.setRoleId(user.getRole().getId());
        }
        form.setStatus(user.getStatus());
        form.setPhone(user.getPhone());
        return form;
    }

    @Transactional
    public User save(UserForm form) {
        User user;
        if (form.getId() == null) {
            // Create new
            if (form.getPassword() == null || form.getPassword().trim().isEmpty()) {
                throw new RuntimeException("Mật khẩu không được để trống khi tạo mới.");
            }
            if (userRepository.existsByUsername(form.getUsername())) {
                throw new RuntimeException("Tên đăng nhập đã tồn tại.");
            }
            if (userRepository.existsByEmail(form.getEmail())) {
                throw new RuntimeException("Email đã tồn tại.");
            }
            user = new User();
            user.setUsername(form.getUsername());
            user.setPasswordHash(passwordEncoder.encode(form.getPassword())); // BCrypt hashed
        } else {
            // Edit existing
            user = findById(form.getId());
            
            // Check username unique if changed
            if (!user.getUsername().equalsIgnoreCase(form.getUsername()) 
                    && userRepository.existsByUsername(form.getUsername())) {
                throw new RuntimeException("Tên đăng nhập đã tồn tại.");
            }
            // Check email unique if changed
            if (!user.getEmail().equalsIgnoreCase(form.getEmail()) 
                    && userRepository.existsByEmail(form.getEmail())) {
                throw new RuntimeException("Email đã tồn tại.");
            }
            
            user.setUsername(form.getUsername());
            
            // If password is provided, update it
            if (form.getPassword() != null && !form.getPassword().trim().isEmpty()) {
                user.setPasswordHash(passwordEncoder.encode(form.getPassword())); // BCrypt hashed
            }
        }

        Role role = roleRepository.findById(form.getRoleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò đã chọn."));

        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());
        user.setRole(role);
        user.setStatus(form.getStatus());
        user.setPhone(form.getPhone());

        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    @Transactional
    public void activate(Long id) {
        User user = findById(id);
        user.setStatus(AccountStatus.ACTIVE);
        user.setDeactivatedAt(null);
        user.setDeactivatedBy(null);
        userRepository.save(user);
    }

    @Transactional
    public void deactivate(Long id) {
        User user = findById(id);
        user.setStatus(AccountStatus.INACTIVE);
        user.setDeactivatedAt(java.time.Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public void unlock(Long id) {
        User user = findById(id);
        user.setStatus(AccountStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    public boolean isOnlyOneAdminRemaining() {
        return userRepository.countActiveOrLockedAdmins(List.of("ACTIVE", "LOCKED")) <= 1;
    }


    @Transactional
    public void changePassword(ChangePasswordForm form) {
        // Find user by username
        User user = userRepository.findByUsername(form.getUsername())
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập không tồn tại."));

        // Verify new password confirmation
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }

        // Verify old password (supporting both BCrypt and raw text as fallback for initial seed data)
        String storedHash = user.getPasswordHash();
        boolean matches = passwordEncoder.matches(form.getOldPassword(), storedHash)
                || form.getOldPassword().equals(storedHash);

        if (!matches) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }

        // Verify new password is not same as current
        boolean sameAsCurrent = passwordEncoder.matches(form.getNewPassword(), storedHash)
                || form.getNewPassword().equals(form.getOldPassword());
        if (sameAsCurrent) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }

        // Encode and save new password
        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(user);
    }
}
