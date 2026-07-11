package com.talenthub.recruitment.dto;

import com.talenthub.recruitment.entity.enums.AccountStatus;
import com.talenthub.recruitment.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class UserForm {

    private Long id;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 150, message = "Họ và tên tối đa 150 ký tự")
    private String fullName;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,50}$", message = "Tên đăng nhập phải từ 4-50 ký tự và chỉ chứa chữ cái, chữ số và dấu gạch dưới")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String email;

    private String password; // optional on edit, required on create

    @NotNull(message = "Vui lòng chọn vai trò")
    private Integer roleId;

    @NotNull(message = "Vui lòng chọn trạng thái")
    private AccountStatus status;

    @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
    private String phone;

    public UserForm() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
