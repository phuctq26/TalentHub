package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.dto.ApplicationDetailDTO;
import com.talenthub.recruitment.dto.ApplicationNoteDTO;
import com.talenthub.recruitment.dto.InterviewEvaluationDTO;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.ApplicationStatus;
import com.talenthub.recruitment.service.ApplicationManagementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class ApplicationDetailController {

    private final ApplicationManagementService applicationManagementService;

    public ApplicationDetailController(ApplicationManagementService applicationManagementService) {
        this.applicationManagementService = applicationManagementService;
    }

    @GetMapping({"/hr/applications/{id}", "/interviewer/applications/{id}"})
    public String viewApplicationDetail(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");

        ApplicationDetailDTO app = applicationManagementService.getApplicationDetail(id, currentUser.getId(), currentRole);
        model.addAttribute("app", app);
        model.addAttribute("isInterviewer", "INTERVIEWER".equalsIgnoreCase(currentRole));

        if (!"INTERVIEWER".equalsIgnoreCase(currentRole)) {
            List<ApplicationNoteDTO> notes = applicationManagementService.getInternalNotes(id, currentUser.getId(), currentRole);
            List<InterviewEvaluationDTO> evaluations = applicationManagementService.getEvaluations(id, currentUser.getId(), currentRole);
            model.addAttribute("notes", notes);
            model.addAttribute("evaluations", evaluations);
            model.addAttribute("activeTab", "applications");
        } else {
            List<com.talenthub.recruitment.dto.InterviewerInterviewDTO> myInterviews = applicationManagementService.getMyInterviews(id, currentUser.getId());
            com.talenthub.recruitment.dto.InterviewerInterviewDTO scheduledInterview = null;
            com.talenthub.recruitment.dto.InterviewerInterviewDTO evaluatedInterview = null;
            for (com.talenthub.recruitment.dto.InterviewerInterviewDTO i : myInterviews) {
                if (i.getStatus() == com.talenthub.recruitment.entity.enums.InterviewStatus.SCHEDULED) {
                    scheduledInterview = i;
                } else if (i.getStatus() == com.talenthub.recruitment.entity.enums.InterviewStatus.EVALUATED) {
                    evaluatedInterview = i;
                }
            }
            model.addAttribute("scheduledInterview", scheduledInterview);
            model.addAttribute("evaluatedInterview", evaluatedInterview);
            model.addAttribute("activeTab", "interviewer-apps");
        }

        model.addAttribute("title", "Hồ sơ ứng viên: " + app.getCandidateFullName());
        return "hr/application-detail";
    }

    @PostMapping("/hr/applications/{id}/status")
    public String changeApplicationStatus(
            @PathVariable Long id,
            @RequestParam("status") ApplicationStatus status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");

        try {
            applicationManagementService.changeStatus(id, status, currentUser.getId(), currentRole);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái ứng viên thành công.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái: " + ex.getMessage());
        }

        return "redirect:/hr/applications/" + id;
    }

    @GetMapping({"/hr/applications/{id}/cv", "/interviewer/applications/{id}/cv"})
    public ResponseEntity<Resource> downloadCv(
            @PathVariable Long id,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");

        ApplicationManagementService.DownloadedCv cv = applicationManagementService.downloadCv(id, currentUser.getId(), currentRole);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, cv.contentType() != null ? cv.contentType() : "application/octet-stream")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(cv.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(cv.resource());
    }
}
