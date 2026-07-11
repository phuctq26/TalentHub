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

@Controller
public class PipelineReportController {

    private static final int PAGE_SIZE = 10;

    private final PipelineReportService pipelineReportService;

    public PipelineReportController(PipelineReportService pipelineReportService) {
        this.pipelineReportService = pipelineReportService;
    }

    @GetMapping("/hr/reports")
    public String viewPipelineReportEntry() {
        return "redirect:/hr/dashboard";
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
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!"ADMIN".equals(currentRole) && !"HR_MANAGER".equals(currentRole)) {
            return "redirect:/login";
        }

        PipelineReportResponse report = pipelineReportService.getPipelineReport(jobId, page, PAGE_SIZE);
        model.addAttribute("report", report);
        model.addAttribute("title", "Pipeline Report");
        model.addAttribute("activeTab", "reports");
        return "hr/pipeline-report";
    }
}
