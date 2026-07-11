package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.entity.Application;
import com.talenthub.recruitment.entity.AuditLog;
import com.talenthub.recruitment.entity.JobPosting;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;

    public AdminDashboardController(
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            JobPostingRepository jobPostingRepository,
            ApplicationRepository applicationRepository,
            InterviewRepository interviewRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        // 1. Phân cấp người dùng và tổng số lượng tài khoản theo vai trò
        List<Object[]> roleCounts = userRepository.countUsersGroupByRole();
        Map<String, Long> roleMap = new HashMap<>();
        long totalUsers = 0;
        for (Object[] row : roleCounts) {
            String roleName = (String) row[0];
            if (roleName != null) {
                Long count = ((Number) row[1]).longValue();
                roleMap.put(roleName.toUpperCase(), count);
                totalUsers += count;
            }
        }
        
        // Đảm bảo các vai trò mặc định có mặt trong map để tránh lỗi render
        roleMap.putIfAbsent("ADMIN", 0L);
        roleMap.putIfAbsent("HR_MANAGER", 0L);
        roleMap.putIfAbsent("INTERVIEWER", 0L);
        roleMap.putIfAbsent("CANDIDATE", 0L);

        model.addAttribute("roleMap", roleMap);
        model.addAttribute("totalUsers", totalUsers);

        // 2. Số lượng tài khoản đang bị khóa (LOCKED)
        long lockedCount = userRepository.countLockedAccounts();
        model.addAttribute("lockedCount", lockedCount);

        // 3. Nhật ký hoạt động gần đây nhất (Recent Activity) - Lấy tối đa 10 dòng
        List<AuditLog> recentLogs = auditLogRepository.findRecentLogs(PageRequest.of(0, 10));
        model.addAttribute("recentLogs", recentLogs);

        // 4. Tổng quan tuyển dụng (Recruitment Summary)
        long activeJobs = jobPostingRepository.countActiveJobsForHrOrAdmin(null);
        long totalApplications = applicationRepository.countAwaitingReviewForHrOrAdmin(null);
        long interviewStage = applicationRepository.countInterviewStage();
        long upcomingInterviewCount = interviewRepository.countUpcomingInterviewsForAdmin();
        long hiredCount = applicationRepository.countHired();

        model.addAttribute("activeJobs", activeJobs);
        model.addAttribute("totalApplications", totalApplications);
        model.addAttribute("interviewStage", interviewStage);
        model.addAttribute("upcomingInterviewCount", upcomingInterviewCount);
        model.addAttribute("hiredCount", hiredCount);

        List<JobPosting> activeJobsList = jobPostingRepository.findActiveJobsForHrOrAdmin(null);
        Map<Long, Long> jobAppCounts = new HashMap<>();
        for (JobPosting job : activeJobsList) {
            long count = applicationRepository.countByJobId(job.getId());
            jobAppCounts.put(job.getId(), count);
        }
        model.addAttribute("activeJobsList", activeJobsList);
        model.addAttribute("jobAppCounts", jobAppCounts);

        return "admin/dashboard";
    }

    @GetMapping("/audit-log")
    public String viewAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {
        Page<AuditLog> logPage = auditLogRepository.findAll(
                PageRequest.of(page, 20, Sort.by("createdAt").descending())
        );

        model.addAttribute("logs", logPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logPage.getTotalPages());
        model.addAttribute("totalItems", logPage.getTotalElements());

        return "admin/audit-log";
    }
}
