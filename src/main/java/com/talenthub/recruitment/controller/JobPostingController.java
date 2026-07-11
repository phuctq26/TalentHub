package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.dto.JobPostingForm;
import com.talenthub.recruitment.entity.JobPosting;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.JobStatus;
import com.talenthub.recruitment.repository.ApplicationRepository;
import com.talenthub.recruitment.service.JobPostingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/jobs/manage")
public class JobPostingController {

    private final JobPostingService jobPostingService;
    private final ApplicationRepository applicationRepository;

    public JobPostingController(JobPostingService jobPostingService, ApplicationRepository applicationRepository) {
        this.jobPostingService = jobPostingService;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping
    public String listJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String department,
            HttpSession session,
            Model model) {

        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");
        Long hrManagerId = null;
        if ("HR_MANAGER".equalsIgnoreCase(currentRole)) {
            hrManagerId = currentUser.getId();
        }

        // 1. Fetch all jobs in user's scope to compute counts and distinct departments
        List<JobPosting> allJobsInScope = jobPostingService.search(hrManagerId, null, null, null);
        long allCount = allJobsInScope.size();
        long draftCount = allJobsInScope.stream().filter(j -> j.getStatus() == JobStatus.DRAFT).count();
        long activeCount = allJobsInScope.stream().filter(j -> j.getStatus() == JobStatus.ACTIVE).count();
        long closedCount = allJobsInScope.stream().filter(j -> j.getStatus() == JobStatus.CLOSED).count();

        List<String> departments = allJobsInScope.stream()
                .map(JobPosting::getDepartment)
                .filter(dept -> dept != null && !dept.trim().isEmpty())
                .distinct()
                .sorted()
                .toList();

        // 2. Fetch filtered jobs for display
        List<JobPosting> jobs = jobPostingService.search(
                hrManagerId,
                keyword,
                status,
                department
        );

        // 3. Compute application counts for each job
        Map<Long, Long> applicationCounts = new HashMap<>();
        for (JobPosting job : jobs) {
            long count = applicationRepository.countByJobId(job.getId());
            applicationCounts.put(job.getId(), count);
        }

        model.addAttribute("jobs", jobs);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("department", department);
        model.addAttribute("departments", departments);
        model.addAttribute("applicationCounts", applicationCounts);

        model.addAttribute("allCount", allCount);
        model.addAttribute("draftCount", draftCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("closedCount", closedCount);

        return "job/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("job", new JobPostingForm());
        model.addAttribute("isEdit", false);
        model.addAttribute("jobStatus", JobStatus.DRAFT);
        model.addAttribute("isClosed", false);

        return "job/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");
        JobPosting jobPosting = jobPostingService.findById(id);

        // Security check: HR Manager can only edit their own postings
        if ("HR_MANAGER".equalsIgnoreCase(currentRole) && !jobPosting.getCreatedBy().getId().equals(currentUser.getId())) {
            return "redirect:/jobs/manage";
        }

        model.addAttribute("job", jobPostingService.getFormById(id));
        model.addAttribute("isEdit", true);
        model.addAttribute("jobStatus", jobPosting.getStatus());
        model.addAttribute("isClosed", jobPosting.getStatus() == JobStatus.CLOSED);

        return "job/form";
    }

    @PostMapping("/save")
    public String saveDraft(
            @Valid @ModelAttribute("job") JobPostingForm form,
            BindingResult result,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            JobStatus jobStatus = JobStatus.DRAFT;

            if (form.getId() != null) {
                jobStatus = jobPostingService.findById(form.getId()).getStatus();
            }

            model.addAttribute("isEdit", form.getId() != null);
            model.addAttribute("jobStatus", jobStatus);
            model.addAttribute("isClosed", jobStatus == JobStatus.CLOSED);

            return "job/form";
        }

        User currentUser = (User) session.getAttribute("currentUser");
        JobPosting savedJob = jobPostingService.saveDraft(form, currentUser);

        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu nháp tin tuyển dụng thành công.");
        return "redirect:/jobs/manage/" + savedJob.getId();
    }

    @PostMapping("/publish-from-form")
    public String publishFromForm(
            @Valid @ModelAttribute("job") JobPostingForm form,
            BindingResult result,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("isEdit", form.getId() != null);
            model.addAttribute("jobStatus", JobStatus.DRAFT);
            model.addAttribute("isClosed", false);

            return "job/form";
        }

        User currentUser = (User) session.getAttribute("currentUser");
        JobPosting savedJob = jobPostingService.saveAndPublish(form, currentUser);

        redirectAttributes.addFlashAttribute("successMessage", "Đăng tin tuyển dụng thành công.");
        return "redirect:/jobs/manage/" + savedJob.getId();
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        jobPostingService.publish(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đăng tin tuyển dụng thành công.");
        return "redirect:/jobs/manage";
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        jobPostingService.close(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đóng tin tuyển dụng thành công.");
        return "redirect:/jobs/manage";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            jobPostingService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tin tuyển dụng thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/jobs/manage";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("job", jobPostingService.findById(id));
        return "job/detail";
    }
}
