package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.entity.JobPosting;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.repository.ApplicationRepository;
import com.talenthub.recruitment.repository.InterviewRepository;
import com.talenthub.recruitment.repository.JobPostingRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/hr")
public class HrDashboardController {

    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;

    public HrDashboardController(
            JobPostingRepository jobPostingRepository,
            ApplicationRepository applicationRepository,
            InterviewRepository interviewRepository) {
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
    }

    @GetMapping("/dashboard")
    public String hrDashboard(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        String currentRole = (String) session.getAttribute("currentRole");
        Long hrManagerId = null;
        if ("HR_MANAGER".equalsIgnoreCase(currentRole)) {
            hrManagerId = currentUser.getId();
        } else if ("ADMIN".equalsIgnoreCase(currentRole)) {
            hrManagerId = null; // Admin sees all jobs & applications
        } else {
            return "redirect:/login";
        }

        // 1. Số lượng công việc hoạt động (Active jobs count)
        long activeJobsCount = jobPostingRepository.countActiveJobsForHrOrAdmin(hrManagerId);
        model.addAttribute("activeJobsCount", activeJobsCount);

        // 2. Số lượng đơn ứng tuyển chờ duyệt (Applications awaiting review)
        long awaitingReviewCount = applicationRepository.countAwaitingReviewForHrOrAdmin(hrManagerId);
        model.addAttribute("awaitingReviewCount", awaitingReviewCount);

        // 3. Số lượng lịch phỏng vấn trong 7 ngày tới (Upcoming interviews)
        long upcomingInterviewsCount = (hrManagerId != null)
                ? interviewRepository.countUpcomingInterviewsForHr(hrManagerId)
                : interviewRepository.countUpcomingInterviewsForAdmin();
        model.addAttribute("upcomingInterviewsCount", upcomingInterviewsCount);

        // 4. Danh sách các tin tuyển dụng đang hoạt động (Active jobs table)
        List<JobPosting> activeJobsList = jobPostingRepository.findActiveJobsForHrOrAdmin(hrManagerId);
        Map<Long, Long> jobAppCounts = new HashMap<>();
        for (JobPosting job : activeJobsList) {
            long count = applicationRepository.countByJobId(job.getId());
            jobAppCounts.put(job.getId(), count);
        }

        model.addAttribute("activeJobs", activeJobsList);
        model.addAttribute("jobAppCounts", jobAppCounts);

        return "hr/dashboard";
    }
}
