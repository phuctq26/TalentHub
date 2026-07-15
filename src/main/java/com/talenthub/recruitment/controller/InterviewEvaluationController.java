package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.entity.Interview;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.InterviewStatus;
import com.talenthub.recruitment.repository.InterviewRepository;
import com.talenthub.recruitment.service.ApplicationManagementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class InterviewEvaluationController {

    private final InterviewRepository interviewRepository;
    private final ApplicationManagementService applicationManagementService;

    public InterviewEvaluationController(
            InterviewRepository interviewRepository,
            ApplicationManagementService applicationManagementService) {
        this.interviewRepository = interviewRepository;
        this.applicationManagementService = applicationManagementService;
    }

    @GetMapping("/interviewer/interviews/{interviewId}/evaluate")
    public String showEvaluationForm(
            @PathVariable Long interviewId,
            HttpSession session,
            Model model) {

        User currentUser = (User) session.getAttribute("currentUser");

        Interview interview = interviewRepository.findByIdWithRelations(interviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));

        if (!interview.getInterviewer().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (interview.getStatus() == InterviewStatus.EVALUATED) {
            return "redirect:/interviewer/applications/" + interview.getApplication().getId();
        }

        model.addAttribute("interview", interview);
        model.addAttribute("app", interview.getApplication());
        model.addAttribute("title", "Đánh giá phỏng vấn");
        model.addAttribute("activeTab", "interviewer-apps");

        return "interviewer/evaluate";
    }

    @PostMapping("/interviewer/interviews/{interviewId}/evaluate")
    public String processEvaluation(
            @PathVariable Long interviewId,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam("feedback") String feedback,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("currentUser");

        Interview interview = interviewRepository.findByIdWithRelations(interviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));

        if (!interview.getInterviewer().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (interview.getStatus() == InterviewStatus.EVALUATED) {
            return "redirect:/interviewer/applications/" + interview.getApplication().getId();
        }

        boolean hasError = false;
        if (rating == null || rating < 1 || rating > 5) {
            model.addAttribute("ratingError", "Vui lòng chọn mức điểm đánh giá (1-5 sao).");
            hasError = true;
        }
        if (feedback == null || feedback.trim().isEmpty()) {
            model.addAttribute("feedbackError", "Vui lòng nhập nhận xét chi tiết.");
            hasError = true;
        }

        if (hasError) {
            model.addAttribute("interview", interview);
            model.addAttribute("app", interview.getApplication());
            model.addAttribute("feedback", feedback);
            model.addAttribute("title", "Đánh giá phỏng vấn");
            model.addAttribute("activeTab", "interviewer-apps");
            return "interviewer/evaluate";
        }

        try {
            applicationManagementService.submitEvaluation(interviewId, rating, feedback.trim(), currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Evaluation submitted. Thank you.");
            return "redirect:/interviewer/applications/" + interview.getApplication().getId();
        } catch (Exception ex) {
            model.addAttribute("interview", interview);
            model.addAttribute("app", interview.getApplication());
            model.addAttribute("feedback", feedback);
            model.addAttribute("title", "Đánh giá phỏng vấn");
            model.addAttribute("activeTab", "interviewer-apps");
            model.addAttribute("errorMessage", "Đã xảy ra lỗi: " + ex.getMessage());
            return "interviewer/evaluate";
        }
    }
}
