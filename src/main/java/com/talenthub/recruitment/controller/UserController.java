package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.dto.UserForm;
import com.talenthub.recruitment.entity.Role;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.AccountStatus;
import com.talenthub.recruitment.repository.RoleRepository;
import com.talenthub.recruitment.service.AuthService;
import com.talenthub.recruitment.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class UserController {
    private final UserService userService;
    private final RoleRepository roleRepository;

    public UserController(UserService userService, RoleRepository roleRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    private List<Role> getManageableRoles() {
        return roleRepository.findAll().stream()
                .filter(r -> "HR_MANAGER".equalsIgnoreCase(r.getName()) || "INTERVIEWER".equalsIgnoreCase(r.getName()))
                .toList();
    }

    @GetMapping
    public String listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Pageable pageable = PageRequest.of(page, 10);
        Page<User> userPage = userService.search(keyword, roleId, status, pageable);

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("roleId", roleId);
        model.addAttribute("status", status);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("statuses", AccountStatus.values());
        model.addAttribute("isOnlyOneAdminRemaining", userService.isOnlyOneAdminRemaining());

        return "admin/user-list";
    }

    @GetMapping("/new")
    public String createUserForm(Model model) {
        UserForm form = new UserForm();
        form.setStatus(AccountStatus.ACTIVE);
        model.addAttribute("userForm", form);
        model.addAttribute("roles", getManageableRoles());
        model.addAttribute("statuses", AccountStatus.values());
        model.addAttribute("isEdit", false);
        return "admin/user-form";
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        UserForm form = userService.getFormById(id);
        model.addAttribute("userForm", form);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("statuses", AccountStatus.values());
        model.addAttribute("isEdit", true);
        return "admin/user-form";
    }

    @PostMapping("/save")
    public String saveUser(
            @Valid @ModelAttribute("userForm") UserForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validate password on Create (Required + Complexity)
        if (form.getId() == null) {
            if (form.getPassword() == null || form.getPassword().trim().isEmpty()) {
                result.rejectValue("password", "NotEmpty", "Mật khẩu không được để trống khi tạo mới.");
            } else if (!form.getPassword().matches("^(?=.*[0-9])(?=.*[A-Z]).{8,}$")) {
                result.rejectValue("password", "Pattern", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm ít nhất 1 chữ hoa và 1 chữ số.");
            }
        } else {
            // Validate password on Edit (Optional + Complexity if not blank)
            if (form.getPassword() != null && !form.getPassword().trim().isEmpty()) {
                if (!form.getPassword().matches("^(?=.*[0-9])(?=.*[A-Z]).{8,}$")) {
                    result.rejectValue("password", "Pattern", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm ít nhất 1 chữ hoa và 1 chữ số.");
                }
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("roles", form.getId() != null ? roleRepository.findAll() : getManageableRoles());
            model.addAttribute("statuses", AccountStatus.values());
            model.addAttribute("isEdit", form.getId() != null);
            return "admin/user-form";
        }

        try {
            userService.save(form);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin người dùng thành công!");
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Email")) {
                result.rejectValue("email", "Duplicate", msg);
            } else {
                result.rejectValue("username", "Duplicate", msg);
            }
            model.addAttribute("roles", form.getId() != null ? roleRepository.findAll() : getManageableRoles());
            model.addAttribute("statuses", AccountStatus.values());
            model.addAttribute("isEdit", form.getId() != null);
            return "admin/user-form";
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/unlock")
    public String unlockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.unlock(id);
            redirectAttributes.addFlashAttribute("successMessage", "Mở khóa tài khoản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể mở khóa tài khoản: " + e.getMessage());
        }
        return "redirect:/admin/users?status=ACTIVE";
    }

    @PostMapping("/{id}/activate")
    public String activateUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.activate(id);
            redirectAttributes.addFlashAttribute("successMessage", "Kích hoạt tài khoản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể kích hoạt tài khoản: " + e.getMessage());
        }
        return "redirect:/admin/users?status=ACTIVE";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deactivate(id);
            redirectAttributes.addFlashAttribute("successMessage", "Vô hiệu hóa tài khoản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể vô hiệu hóa tài khoản: " + e.getMessage());
        }
        return "redirect:/admin/users?status=INACTIVE";
    }

    @GetMapping("/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id));
        return "admin/user-detail";
    }
}
