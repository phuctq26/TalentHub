package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.dto.ApplicationDetailDTO;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.service.ApplicationManagementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Controller
public class InterviewAssignmentController {

    private final ApplicationManagementService applicationManagementService;

    public InterviewAssignmentController(ApplicationManagementService applicationManagementService) {
        this.applicationManagementService = applicationManagementService;
    }

    @GetMapping("/hr/applications/{id}/assign-interview")
    public String showAssignInterviewForm(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");

        // Validate access and get app details
        ApplicationDetailDTO app = applicationManagementService.getApplicationDetail(id, currentUser.getId(), currentRole);
        List<User> interviewers = applicationManagementService.getActiveInterviewers();

        model.addAttribute("app", app);
        model.addAttribute("interviewers", interviewers);
        model.addAttribute("title", "Lên lịch phỏng vấn");
        model.addAttribute("activeTab", "applications");

        return "hr/assign-interview";
    }

    @PostMapping("/hr/applications/{id}/assign-interview")
    public String processAssignInterview(
            @PathVariable Long id,
            @RequestParam("interviewerId") Long interviewerId,
            @RequestParam("interviewDate") String interviewDate,
            @RequestParam("interviewTime") String interviewTime,
            @RequestParam(value = "locationOrMeetingLink", required = false) String locationOrMeetingLink,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");

        ApplicationDetailDTO app = null;
        List<User> interviewers = null;

        try {
            app = applicationManagementService.getApplicationDetail(id, currentUser.getId(), currentRole);
            interviewers = applicationManagementService.getActiveInterviewers();
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/applications/" + id;
        }

        boolean hasError = false;
        if (interviewerId == null) {
            model.addAttribute("interviewerError", "Vui lòng chọn người phỏng vấn.");
            hasError = true;
        }
        if (interviewDate == null || interviewDate.isBlank()) {
            model.addAttribute("dateError", "Vui lòng chọn ngày phỏng vấn.");
            hasError = true;
        }
        if (interviewTime == null || interviewTime.isBlank()) {
            model.addAttribute("timeError", "Vui lòng chọn giờ phỏng vấn.");
            hasError = true;
        }

        Instant scheduledAt = null;
        if (!hasError) {
            try {
                LocalDate localDate = LocalDate.parse(interviewDate);
                LocalTime localTime = LocalTime.parse(interviewTime);
                LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
                ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh"));
                scheduledAt = zonedDateTime.toInstant();

                if (scheduledAt.isBefore(Instant.now())) {
                    model.addAttribute("dateError", "Interview must be scheduled for a future date and time.");
                    hasError = true;
                }
            } catch (Exception ex) {
                model.addAttribute("errorMessage", "Định dạng ngày/giờ không hợp lệ.");
                hasError = true;
            }
        }

        if (hasError) {
            model.addAttribute("app", app);
            model.addAttribute("interviewers", interviewers);
            model.addAttribute("locationOrMeetingLink", locationOrMeetingLink);
            model.addAttribute("title", "Lên lịch phỏng vấn");
            model.addAttribute("activeTab", "applications");
            return "hr/assign-interview";
        }

        try {
            applicationManagementService.scheduleInterview(id, interviewerId, scheduledAt, locationOrMeetingLink, currentUser);

            String interviewerName = "";
            for (User u : interviewers) {
                if (u.getId().equals(interviewerId)) {
                    interviewerName = u.getFullName();
                    break;
                }
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Interview scheduled. " + interviewerName + " has been assigned.");
            return "redirect:/hr/applications/" + id;
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Đã xảy ra lỗi: " + ex.getMessage());
            model.addAttribute("app", app);
            model.addAttribute("interviewers", interviewers);
            model.addAttribute("title", "Lên lịch phỏng vấn");
            model.addAttribute("activeTab", "applications");
            return "hr/assign-interview";
        }
    }
}
