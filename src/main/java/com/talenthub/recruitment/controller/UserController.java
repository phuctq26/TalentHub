package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.dto.UserForm;
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

    @GetMapping
    public String listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) AccountStatus status,
            Model model) {

        List<User> users = userService.search(keyword, roleId, status);

        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        model.addAttribute("roleId", roleId);
        model.addAttribute("status", status);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("statuses", AccountStatus.values());

        return "admin/user-list";
    }

    @GetMapping("/new")
    public String createUserForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        model.addAttribute("roles", roleRepository.findAll());
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

        // Validate password required on create
        if (form.getId() == null && (form.getPassword() == null || form.getPassword().trim().isEmpty())) {
            result.rejectValue("password", "NotEmpty", "Mật khẩu không được để trống khi tạo mới.");
        }

        if (result.hasErrors()) {
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("statuses", AccountStatus.values());
            model.addAttribute("isEdit", form.getId() != null);
            return "admin/user-form";
        }

        try {
            userService.save(form);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin người dùng thành công!");
        } catch (Exception e) {
            result.rejectValue("username", "Duplicate", e.getMessage());
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("statuses", AccountStatus.values());
            model.addAttribute("isEdit", form.getId() != null);
            return "admin/user-form";
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa người dùng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa người dùng: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/activate")
    public String activateUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.activate(id);
            redirectAttributes.addFlashAttribute("successMessage", "Kích hoạt tài khoản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể kích hoạt tài khoản: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deactivate(id);
            redirectAttributes.addFlashAttribute("successMessage", "Vô hiệu hóa tài khoản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể vô hiệu hóa tài khoản: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id));
        return "admin/user-detail";
    }
}
