package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.dto.ApplicationNoteDTO;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.service.ApplicationManagementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ApplicationNoteController {

    private final ApplicationManagementService applicationManagementService;

    public ApplicationNoteController(ApplicationManagementService applicationManagementService) {
        this.applicationManagementService = applicationManagementService;
    }

    @PostMapping("/hr/applications/{id}/note")
    public String addApplicationNote(
            @PathVariable Long id,
            @RequestParam("noteContent") String noteContent,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpSession session,
            Model model) {

        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");

        Long hrId = "HR_MANAGER".equalsIgnoreCase(currentRole) ? currentUser.getId() : null;

        applicationManagementService.addNote(id, noteContent.trim(), hrId);

        if (hxRequest != null) {
            List<ApplicationNoteDTO> notes = applicationManagementService.getInternalNotes(id, currentUser.getId(), currentRole);
            model.addAttribute("notes", notes);
            return "hr/application-detail :: notes-list";
        }

        return "redirect:/hr/applications/" + id;
    }
}
