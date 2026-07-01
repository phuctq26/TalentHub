package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.UserRole;
import com.talenthub.recruitment.repository.UserRepository;
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

import java.lang.reflect.Method;

@Controller
public class ApplyJobController {

    private static final String LOGGED_IN_USER = "LOGGED_IN_USER";
    private static final String LEGACY_LOGGED_IN_USER = "loggedInUser";

    private final ApplicationService applicationService;
    private final PublicJobService publicJobService;
    private final UserRepository userRepository;

    public ApplyJobController(
            ApplicationService applicationService,
            PublicJobService publicJobService,
            UserRepository userRepository
    ) {
        this.applicationService = applicationService;
        this.publicJobService = publicJobService;
        this.userRepository = userRepository;
    }

    @GetMapping("/jobs/{id}/apply")
    public String showApplyForm(@PathVariable Long id, HttpSession session, Model model) {
        User candidate = resolveCandidate(session);
        if (candidate == null) {
            return "redirect:/candidate/login";
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
            return "redirect:/candidate/login";
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
        Object sessionUser = session.getAttribute(LOGGED_IN_USER);
        if (sessionUser == null) {
            sessionUser = session.getAttribute(LEGACY_LOGGED_IN_USER);
        }

        User user = toUser(sessionUser);
        if (user == null || user.getRole() == null || !"CANDIDATE".equalsIgnoreCase(user.getRole().getName())) {
            return null;
        }
        return user;
    }

    private User toUser(Object sessionUser) {
        if (sessionUser instanceof User user) {
            return user;
        }
        Long userId = extractUserId(sessionUser);
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private Long extractUserId(Object sessionUser) {
        if (sessionUser instanceof Number number) {
            return number.longValue();
        }
        if (sessionUser == null) {
            return null;
        }
        try {
            Method getId = sessionUser.getClass().getMethod("getId");
            Object value = getId.invoke(sessionUser);
            return value instanceof Number number ? number.longValue() : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
