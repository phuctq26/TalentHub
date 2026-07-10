package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.service.ApplicationService;
import com.talenthub.recruitment.service.PublicJobService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ApplyJobController {

    private final ApplicationService applicationService;
    private final PublicJobService publicJobService;

    public ApplyJobController(
            ApplicationService applicationService,
            PublicJobService publicJobService
    ) {
        this.applicationService = applicationService;
        this.publicJobService = publicJobService;
    }

    @GetMapping("/jobs/{id}/apply")
    public String showApplyForm(@PathVariable Long id, HttpSession session, Model model) {
        User candidate = resolveCandidate(session);
        if (candidate == null) {
            return "redirect:/login";
        }

        model.addAttribute("job", publicJobService.getPublicJobDetail(id));
        model.addAttribute("candidate", candidate);
        return "candidate/apply-job";
    }

    @PostMapping("/jobs/{id}/apply")
    public String apply(
            @PathVariable Long id,
            @RequestParam("cvFile") MultipartFile cvFile,
            @RequestParam(required = false) String coverLetter,
            HttpSession session,
            Model model
    ) {
        User candidate = resolveCandidate(session);
        if (candidate == null) {
            return "redirect:/login";
        }

        try {
            applicationService.apply(id, candidate, cvFile, coverLetter);
            return "redirect:/jobs/" + id + "?applied=true";
        } catch (RuntimeException ex) {
            model.addAttribute("job", publicJobService.getPublicJobDetail(id));
            model.addAttribute("candidate", candidate);
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("coverLetter", coverLetter);
            return "candidate/apply-job";
        }
    }

    private User resolveCandidate(HttpSession session) {
        User user = session != null ? (User) session.getAttribute("currentUser") : null;
        if (user == null || user.getRole() == null || !"CANDIDATE".equals(user.getRole().getName())) {
            return null;
        }
        return user;
    }
}
