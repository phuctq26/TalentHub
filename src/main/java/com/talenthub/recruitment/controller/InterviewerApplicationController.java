package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.entity.Interview;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.repository.InterviewRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/interviewer")
public class InterviewerApplicationController {

    private final InterviewRepository interviewRepository;

    public InterviewerApplicationController(InterviewRepository interviewRepository) {
        this.interviewRepository = interviewRepository;
    }

    @GetMapping("/applications")
    public String viewMyInterviews(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        List<Interview> myInterviews = interviewRepository.findByInterviewerId(currentUser.getId());

        model.addAttribute("interviews", myInterviews);
        model.addAttribute("title", "Lịch phỏng vấn của tôi");
        model.addAttribute("activeTab", "interviewer-apps");

        return "interviewer/applications";
    }
}
