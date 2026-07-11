package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.dto.PipelineReportResponse;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.service.PipelineReportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class PipelineReportController {

    private static final int PAGE_SIZE = 10;

    private final PipelineReportService pipelineReportService;

    public PipelineReportController(PipelineReportService pipelineReportService) {
        this.pipelineReportService = pipelineReportService;
    }

    @GetMapping("/hr/reports")
    public String viewPipelineReportEntry(
            @RequestParam(required = false) Long jobId,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");
        if (!canViewReport(currentUser, currentRole)) {
            return "redirect:/login";
        }

        List<PipelineReportResponse.JobOption> jobOptions =
                pipelineReportService.getAvailableJobs(currentUser, currentRole);
        model.addAttribute("jobOptions", jobOptions);
        model.addAttribute("showJobSelector", true);
        model.addAttribute("activeTab", "reports");
        model.addAttribute("title", "Pipeline Report");

        if (jobOptions.isEmpty()) {
            return "hr/pipeline-report";
        }

        Long selectedJobId = jobId != null ? jobId : jobOptions.get(0).getId();
        PipelineReportResponse report = pipelineReportService.getPipelineReport(
                selectedJobId,
                page,
                PAGE_SIZE,
                currentUser,
                currentRole
        );
        model.addAttribute("report", report);
        return "hr/pipeline-report";
    }

    @GetMapping("/hr/jobs/{jobId}/pipeline-report")
    public String viewPipelineReport(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");
        if (!canViewReport(currentUser, currentRole)) {
            return "redirect:/login";
        }

        PipelineReportResponse report = pipelineReportService.getPipelineReport(
                jobId,
                page,
                PAGE_SIZE,
                currentUser,
                currentRole
        );
        model.addAttribute("report", report);
        model.addAttribute("jobOptions", List.of());
        model.addAttribute("showJobSelector", false);
        model.addAttribute("title", "Pipeline Report");
        model.addAttribute("activeTab", "reports");
        return "hr/pipeline-report";
    }

    private boolean canViewReport(User currentUser, String currentRole) {
        return currentUser != null
                && ("ADMIN".equals(currentRole) || "HR_MANAGER".equals(currentRole));
    }
}
