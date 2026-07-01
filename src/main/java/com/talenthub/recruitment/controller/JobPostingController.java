package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.dto.JobPostingForm;
import com.talenthub.recruitment.entity.JobPosting;
import com.talenthub.recruitment.entity.enums.JobStatus;
import com.talenthub.recruitment.service.JobPostingService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/jobs/manage")
public class JobPostingController {

    private final JobPostingService jobPostingService;

    public JobPostingController(JobPostingService jobPostingService) {
        this.jobPostingService = jobPostingService;
    }

    @GetMapping
    public String listJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String department,
            Model model) {

        List<JobPosting> jobs = jobPostingService.search(
                null,
                keyword,
                status,
                department
        );

        model.addAttribute("jobs", jobs);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("department", department);
        model.addAttribute("statuses", JobStatus.values());

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
    public String editForm(@PathVariable Long id, Model model) {
        JobPosting jobPosting = jobPostingService.findById(id);

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

        JobPosting savedJob = jobPostingService.saveDraft(form);

        return "redirect:/jobs/manage/" + savedJob.getId();
    }

    @PostMapping("/publish-from-form")
    public String publishFromForm(
            @Valid @ModelAttribute("job") JobPostingForm form,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("isEdit", form.getId() != null);
            model.addAttribute("jobStatus", JobStatus.DRAFT);
            model.addAttribute("isClosed", false);

            return "job/form";
        }

        JobPosting savedJob = jobPostingService.saveAndPublish(form);

        return "redirect:/jobs/manage/" + savedJob.getId();
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id) {
        jobPostingService.publish(id);
        return "redirect:/jobs/manage";
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id) {
        jobPostingService.close(id);
        return "redirect:/jobs/manage";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        jobPostingService.delete(id);
        return "redirect:/jobs/manage";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("job", jobPostingService.findById(id));
        return "job/detail";
    }
}
