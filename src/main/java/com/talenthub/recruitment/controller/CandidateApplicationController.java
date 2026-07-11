package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.dto.MyApplicationResponse;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.ApplicationStatus;
import com.talenthub.recruitment.service.ApplicationService;
import com.talenthub.recruitment.service.ApplicationService.CandidateCvFile;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
public class CandidateApplicationController {

    private static final int PAGE_SIZE = 10;

    private final ApplicationService applicationService;

    public CandidateApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/candidate/applications")
    public String viewMyApplications(
            HttpSession session,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        User candidate = resolveCandidate(session);

        Page<MyApplicationResponse> applicationPage = applicationService.getMyApplications(
                candidate.getId(),
                status,
                page,
                PAGE_SIZE
        );

        model.addAttribute("applicationPage", applicationPage);
        model.addAttribute("statuses", ApplicationStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("title", "My Applications");
        model.addAttribute("activeTab", "candidate-apps");
        return "candidate/my-applications";
    }

    @GetMapping("/candidate/applications/{id}")
    public String viewMyApplicationDetail(
            @PathVariable Long id,
            HttpSession session,
            Model model
    ) {
        User candidate = resolveCandidate(session);
        MyApplicationResponse applicationDetail = applicationService.getMyApplicationDetail(candidate.getId(), id);

        model.addAttribute("applicationDetail", applicationDetail);
        model.addAttribute("title", "Application Details");
        model.addAttribute("activeTab", "candidate-apps");
        return "candidate/application-detail";
    }

    @GetMapping("/candidate/applications/{id}/cv")
    public ResponseEntity<Resource> viewMyApplicationCv(
            @PathVariable Long id,
            HttpSession session
    ) {
        User candidate = resolveCandidate(session);
        CandidateCvFile cvFile = applicationService.getCandidateCv(candidate.getId(), id);

        return ResponseEntity.ok()
                .contentType(resolveMediaType(cvFile.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(cvFile.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .header("X-Content-Type-Options", "nosniff")
                .body(cvFile.resource());
    }

    @PostMapping("/candidate/applications/{id}/withdraw")
    public String withdrawMyApplication(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User candidate = resolveCandidate(session);

        try {
            applicationService.withdraw(candidate.getId(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Application withdrawn successfully.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/candidate/applications";
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private User resolveCandidate(HttpSession session) {
        User currentUser = session != null ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        if (currentUser.getRole() == null || !"CANDIDATE".equals(currentUser.getRole().getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Candidate role required");
        }
        return currentUser;
    }
}
