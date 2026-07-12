package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.entity.JobPosting;
import com.talenthub.recruitment.service.PublicJobService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PublicJobController {

    private final PublicJobService publicJobService;

    public PublicJobController(PublicJobService publicJobService) {
        this.publicJobService = publicJobService;
    }

    @GetMapping("/jobs")
    public String publicJobList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        Page<JobPosting> jobPage = publicJobService.getPublicJobs(keyword, department, location, page, 5);

        model.addAttribute("jobPage", jobPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("department", department);
        model.addAttribute("location", location);

        return isLoggedIn(session)
                ? "candidate/public-job-list-app"
                : "candidate/public-job-list";
    }

    @GetMapping("/jobs/{id}")
    public String publicJobDetail(
            @PathVariable Long id,
            HttpSession session,
            Model model
    ) {
        model.addAttribute("job", publicJobService.getPublicJobDetail(id));
        return isLoggedIn(session)
                ? "candidate/public-job-detail-app"
                : "candidate/public-job-detail";
    }

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("currentUser") != null;
    }
}
