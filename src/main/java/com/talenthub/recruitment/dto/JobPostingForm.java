package com.talenthub.recruitment.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class JobPostingForm {

    private Long id;

    @NotBlank(message = "Tiêu đề công việc không được để trống.")
    @Size(max = 200, message = "Tiêu đề công việc tối đa 200 ký tự.")
    private String title;

    @NotBlank(message = "Phòng ban không được để trống.")
    @Size(max = 100, message = "Phòng ban tối đa 100 ký tự.")
    private String department;

    @NotBlank(message = "Địa điểm làm việc không được để trống.")
    @Size(max = 100, message = "Địa điểm tối đa 100 ký tự.")
    private String location;

    @NotBlank(message = "Mô tả công việc không được để trống.")
    private String description;

    private String requirements;

    @Size(max = 100, message = "Mức lương tối đa 100 ký tự.")
    private String salaryRange;

    @FutureOrPresent(message = "Hạn nộp hồ sơ phải là ngày hiện tại hoặc trong tương lai.")
    private LocalDate applicationDeadline;

    public JobPostingForm() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(LocalDate applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }
}
