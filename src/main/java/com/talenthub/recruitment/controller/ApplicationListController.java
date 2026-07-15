package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.dto.ApplicationListDTO;
import com.talenthub.recruitment.dto.PipelineCountsDTO;
import com.talenthub.recruitment.entity.JobPosting;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.service.ApplicationManagementService;
import com.talenthub.recruitment.service.JobPostingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ApplicationListController {

    private static final int PAGE_SIZE = 10;

    private final ApplicationManagementService applicationManagementService;
    private final JobPostingService jobPostingService;

    public ApplicationListController(
            ApplicationManagementService applicationManagementService,
            JobPostingService jobPostingService) {
        this.applicationManagementService = applicationManagementService;
        this.jobPostingService = jobPostingService;
    }

    @GetMapping("/hr/applications")
    public String listAllApplications(
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpSession session,
            Model model) {

        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");

        Long hrManagerId = "HR_MANAGER".equalsIgnoreCase(currentRole) ? currentUser.getId() : null;

        List<JobPosting> jobsForFilter = jobPostingService.search(hrManagerId, null, null, null);
        Page<ApplicationListDTO> applicationPage = applicationManagementService.getApplicationList(jobId, status, page, PAGE_SIZE, hrManagerId);
        PipelineCountsDTO counts = applicationManagementService.getPipelineCounts(jobId, hrManagerId);

        model.addAttribute("applicationPage", applicationPage);
        model.addAttribute("jobs", jobsForFilter);
        model.addAttribute("selectedJobId", jobId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("counts", counts);
        model.addAttribute("title", "Recruitment Pipeline");
        model.addAttribute("activeTab", "applications");

        if (jobId != null) {
            JobPosting job = jobPostingService.findById(jobId);
            model.addAttribute("jobContext", job);
        }

        if (hxRequest != null) {
            return "hr/application-list :: application-list-container";
        }

        return "hr/application-list";
    }

    @GetMapping("/hr/jobs/{jobId}/applications")
    public String listJobApplications(
            @PathVariable Long jobId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpSession session,
            Model model) {
        return listAllApplications(jobId, status, page, hxRequest, session, model);
    }
}
