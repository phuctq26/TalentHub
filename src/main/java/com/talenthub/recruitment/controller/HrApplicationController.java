package com.talenthub.recruitment.controller;

import com.talenthub.recruitment.entity.Application;
import com.talenthub.recruitment.entity.ApplicationNote;
import com.talenthub.recruitment.entity.Interview;
import com.talenthub.recruitment.entity.JobPosting;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.ApplicationStatus;
import com.talenthub.recruitment.entity.enums.InterviewStatus;
import com.talenthub.recruitment.service.ApplicationService;
import com.talenthub.recruitment.service.ApplicationService.CandidateCvFile;
import com.talenthub.recruitment.service.JobPostingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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
import java.util.ArrayList;
import java.util.List;

@Controller
public class HrApplicationController {

    private static final int PAGE_SIZE = 10;

    private final ApplicationService applicationService;
    private final JobPostingService jobPostingService;
    private final com.talenthub.recruitment.repository.InterviewRepository interviewRepository;
    private final com.talenthub.recruitment.repository.AuditLogRepository auditLogRepository;

    public HrApplicationController(
            ApplicationService applicationService,
            JobPostingService jobPostingService,
            com.talenthub.recruitment.repository.InterviewRepository interviewRepository,
            com.talenthub.recruitment.repository.AuditLogRepository auditLogRepository
    ) {
        this.applicationService = applicationService;
        this.jobPostingService = jobPostingService;
        this.interviewRepository = interviewRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Hiển thị danh sách tất cả hồ sơ ứng tuyển (SCR-16) có phân trang và bộ lọc.
     */
    @GetMapping("/hr/applications")
    public String listAllApplications(
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        // Lấy thông tin người dùng đang đăng nhập từ Session
        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");
        
        // Nếu là HR_MANAGER, chỉ lấy các công việc do chính HR đó tạo ra. Nếu là ADMIN, được xem tất cả.
        Long hrManagerId = null;
        if ("HR_MANAGER".equalsIgnoreCase(currentRole)) {
            hrManagerId = currentUser.getId();
        }

        // Lấy danh sách công việc tương ứng để hiển thị trong ô lựa chọn bộ lọc (Dropdown filter)
        List<JobPosting> jobsForFilter = jobPostingService.search(hrManagerId, null, null, null);

        // Lấy danh sách hồ sơ ứng tuyển đã được lọc theo công việc, trạng thái và phân trang
        Page<Application> applicationPage = applicationService.getApplications(jobId, status, page, PAGE_SIZE, hrManagerId);

        // Lấy toàn bộ danh sách hồ sơ (không phân trang) để đếm số lượng cho từng tab bộ lọc
        List<Application> allAppsInScope = applicationService.getApplications(jobId, null, 0, Integer.MAX_VALUE, hrManagerId).getContent();
        
        // Đếm số lượng ứng viên theo từng trạng thái bằng vòng lặp for đơn giản, dễ hiểu
        long allCount = allAppsInScope.size();
        long appliedCount = 0;
        long screeningCount = 0;
        long interviewCount = 0;
        long offerCount = 0;
        long hiredCount = 0;
        long rejectedCount = 0;
        long withdrawnCount = 0;

        for (Application app : allAppsInScope) {
            if (app.getStatus() == ApplicationStatus.APPLIED) {
                appliedCount++;
            } else if (app.getStatus() == ApplicationStatus.SCREENING) {
                screeningCount++;
            } else if (app.getStatus() == ApplicationStatus.INTERVIEW) {
                interviewCount++;
            } else if (app.getStatus() == ApplicationStatus.OFFER) {
                offerCount++;
            } else if (app.getStatus() == ApplicationStatus.HIRED) {
                hiredCount++;
            } else if (app.getStatus() == ApplicationStatus.REJECTED) {
                rejectedCount++;
            } else if (app.getStatus() == ApplicationStatus.WITHDRAWN) {
                withdrawnCount++;
            }
        }

        // Đưa các thuộc tính vào Model để hiển thị ngoài giao diện Thymeleaf
        model.addAttribute("applicationPage", applicationPage);
        model.addAttribute("jobs", jobsForFilter);
        model.addAttribute("selectedJobId", jobId);
        model.addAttribute("selectedStatus", status);

        model.addAttribute("allCount", allCount);
        model.addAttribute("appliedCount", appliedCount);
        model.addAttribute("screeningCount", screeningCount);
        model.addAttribute("interviewCount", interviewCount);
        model.addAttribute("offerCount", offerCount);
        model.addAttribute("hiredCount", hiredCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("withdrawnCount", withdrawnCount);

        model.addAttribute("title", "Recruitment Pipeline");
        model.addAttribute("activeTab", "applications");

        // Thiết lập thông tin ngữ cảnh công việc nếu đang lọc theo một Job cụ thể
        if (jobId != null) {
            JobPosting job = jobPostingService.findById(jobId);
            model.addAttribute("jobContext", job);
        }

        return "hr/application-list";
    }

    /**
     * Hiển thị danh sách hồ sơ ứng tuyển của một công việc cụ thể (SCR-16).
     */
    @GetMapping("/hr/jobs/{jobId}/applications")
    public String listJobApplications(
            @PathVariable Long jobId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {
        // Tái sử dụng lại phương thức listAllApplications bằng cách truyền trực tiếp jobId vào
        return listAllApplications(jobId, status, page, session, model);
    }

    /**
     * Hiển thị chi tiết hồ sơ ứng tuyển dành cho HR Manager / Admin (SCR-17).
     */
    @GetMapping("/hr/applications/{id}")
    public String viewApplicationDetail(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        // Lấy thông tin người dùng đang đăng nhập từ Session
        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");
        
        Long hrManagerId = null;
        if ("HR_MANAGER".equalsIgnoreCase(currentRole)) {
            hrManagerId = currentUser.getId();
        }

        // Tìm thông tin hồ sơ ứng tuyển theo ID
        Application application = applicationService.getApplicationById(id);

        // Kiểm tra bảo mật: Nếu là HR Manager, chỉ cho phép xem hồ sơ ứng tuyển của công việc do mình tạo ra
        if (hrManagerId != null) {
            Long creatorId = application.getJob().getCreatedBy().getId();
            if (!creatorId.equals(hrManagerId)) {
                return "redirect:/hr/applications";
            }
        }

        // Lấy danh sách ghi chú nội bộ và lịch sử phỏng vấn của hồ sơ này
        List<ApplicationNote> notes = applicationService.getNotes(id);
        List<Interview> interviews = applicationService.getInterviews(id);

        // Lọc danh sách các buổi phỏng vấn đã đánh giá (EVALUATED) bằng vòng lặp for đơn giản
        List<Interview> evaluations = new ArrayList<>();
        for (Interview interview : interviews) {
            if (interview.getStatus() == InterviewStatus.EVALUATED) {
                evaluations.add(interview);
            }
        }

        // Đưa các thông tin vào Model để hiển thị ngoài giao diện Thymeleaf
        model.addAttribute("application", application);
        model.addAttribute("notes", notes);
        model.addAttribute("interviews", interviews);
        model.addAttribute("evaluations", evaluations);
        model.addAttribute("isInterviewer", false);
        model.addAttribute("title", "Hồ sơ ứng viên: " + application.getCandidate().getFullName());
        model.addAttribute("activeTab", "applications");

        return "hr/application-detail";
    }

    /**
     * Hiển thị chi tiết hồ sơ ứng tuyển dành cho Interviewer được phân công (SCR-17).
     */
    @GetMapping("/interviewer/applications/{id}")
    public String viewInterviewerApplicationDetail(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        // Lấy thông tin người dùng đang đăng nhập từ Session
        User currentUser = (User) session.getAttribute("currentUser");
        
        // Tìm thông tin hồ sơ ứng tuyển theo ID
        Application application = applicationService.getApplicationById(id);
        List<Interview> interviews = applicationService.getInterviews(id);

        // Kiểm tra bảo mật: Kiểm tra xem người phỏng vấn hiện tại có được phân công đánh giá hồ sơ này không
        boolean isAssigned = false;
        for (Interview i : interviews) {
            if (i.getInterviewer().getId().equals(currentUser.getId())) {
                isAssigned = true;
                break;
            }
        }

        // Nếu không được phân công, từ chối quyền truy cập (Forbidden)
        if (!isAssigned) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
        }

        // Tìm lịch phỏng vấn chưa đánh giá (SCHEDULED) của người phỏng vấn hiện tại
        Interview scheduledInterview = null;
        for (Interview i : interviews) {
            if (i.getInterviewer().getId().equals(currentUser.getId()) && i.getStatus() == InterviewStatus.SCHEDULED) {
                scheduledInterview = i;
                break;
            }
        }

        // Tìm lịch phỏng vấn đã đánh giá (EVALUATED) của người phỏng vấn hiện tại
        Interview evaluatedInterview = null;
        for (Interview i : interviews) {
            if (i.getInterviewer().getId().equals(currentUser.getId()) && i.getStatus() == InterviewStatus.EVALUATED) {
                evaluatedInterview = i;
                break;
            }
        }

        // Đưa các thông tin vào Model để hiển thị ngoài giao diện Thymeleaf
        model.addAttribute("application", application);
        model.addAttribute("interviews", interviews);
        model.addAttribute("scheduledInterview", scheduledInterview);
        model.addAttribute("evaluatedInterview", evaluatedInterview);
        model.addAttribute("isInterviewer", true);
        model.addAttribute("title", "Hồ sơ ứng viên: " + application.getCandidate().getFullName());
        model.addAttribute("activeTab", "interviewer-apps");

        return "hr/application-detail";
    }

    /**
     * Xử lý chuyển đổi trạng thái (Advance/Reject) cho hồ sơ ứng tuyển của HR Manager / Admin (SCR-17).
     */
    @PostMapping("/hr/applications/{id}/status")
    public String changeApplicationStatus(
            @PathVariable Long id,
            @RequestParam("status") ApplicationStatus status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Lấy thông tin người dùng đang đăng nhập từ Session
        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");
        
        Long hrManagerId = null;
        if ("HR_MANAGER".equalsIgnoreCase(currentRole)) {
            hrManagerId = currentUser.getId();
        }

        Application application = applicationService.getApplicationById(id);

        // Kiểm tra bảo mật: Nếu là HR Manager, chỉ cho phép thao tác trên hồ sơ của công việc mình tạo ra
        if (hrManagerId != null) {
            Long creatorId = application.getJob().getCreatedBy().getId();
            if (!creatorId.equals(hrManagerId)) {
                return "redirect:/hr/applications";
            }
        }

        try {
            // Thực hiện thay đổi trạng thái của hồ sơ ứng tuyển
            applicationService.changeStatus(id, status);
            
            com.talenthub.recruitment.entity.AuditLog log = new com.talenthub.recruitment.entity.AuditLog();
            log.setActorUser(currentUser);
            log.setActorUsernameSnapshot(currentUser.getUsername());
            log.setActorFullNameSnapshot(currentUser.getFullName());
            log.setEventType(com.talenthub.recruitment.entity.enums.AuditEventType.APPLICATION_STATUS_CHANGED);
            log.setDescription("Chuyển trạng thái hồ sơ #" + id + " của " + application.getCandidate().getFullName() + " sang " + status.name());
            auditLogRepository.save(log);
            
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái ứng viên thành công.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái: " + ex.getMessage());
        }

        return "redirect:/hr/applications/" + id;
    }

    /**
     * Thêm ghi chú nội bộ mới cho hồ sơ ứng tuyển (SCR-17).
     */
    @PostMapping("/hr/applications/{id}/note")
    public String addNote(
            @PathVariable Long id,
            @RequestParam("noteContent") String noteContent,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Lấy thông tin người dùng đang đăng nhập từ Session
        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");
        
        Long hrManagerId = null;
        if ("HR_MANAGER".equalsIgnoreCase(currentRole)) {
            hrManagerId = currentUser.getId();
        }

        Application application = applicationService.getApplicationById(id);

        // Kiểm tra bảo mật: Nếu là HR Manager, chỉ cho phép thêm ghi chú cho hồ sơ thuộc công việc của mình
        if (hrManagerId != null) {
            Long creatorId = application.getJob().getCreatedBy().getId();
            if (!creatorId.equals(hrManagerId)) {
                return "redirect:/hr/applications";
            }
        }

        try {
            // Thêm ghi chú nội bộ mới
            applicationService.addNote(id, currentUser, noteContent);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm ghi chú nội bộ thành công.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể thêm ghi chú: " + ex.getMessage());
        }

        return "redirect:/hr/applications/" + id;
    }

    /**
     * Tải xuống tệp hồ sơ CV tuyển dụng dành cho HR Manager / Admin (SCR-17).
     */
    @GetMapping("/hr/applications/{id}/cv")
    public ResponseEntity<Resource> downloadCvForHr(
            @PathVariable Long id,
            HttpSession session) {

        // Lấy thông tin người dùng đang đăng nhập từ Session
        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");

        // Lấy tệp CV trên máy chủ, hệ thống sẽ kiểm tra quyền truy cập ở tầng dịch vụ (Service layer)
        CandidateCvFile cvFile = applicationService.getCvForHrOrInterviewer(id, currentUser.getId(), currentRole);

        // Ghi audit log
        com.talenthub.recruitment.entity.AuditLog log = new com.talenthub.recruitment.entity.AuditLog();
        log.setActorUser(currentUser);
        log.setActorUsernameSnapshot(currentUser.getUsername());
        log.setActorFullNameSnapshot(currentUser.getFullName());
        log.setEventType(com.talenthub.recruitment.entity.enums.AuditEventType.CV_DOWNLOADED);
        log.setDescription("Tải CV hồ sơ #" + id);
        auditLogRepository.save(log);

        return ResponseEntity.ok()
                .contentType(resolveMediaType(cvFile.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(cvFile.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(cvFile.resource());
    }

    /**
     * Tải xuống tệp hồ sơ CV tuyển dụng dành cho Interviewer (SCR-17).
     */
    @GetMapping("/interviewer/applications/{id}/cv")
    public ResponseEntity<Resource> downloadCvForInterviewer(
            @PathVariable Long id,
            HttpSession session) {
        // Tái sử dụng lại phương thức downloadCvForHr để tận dụng phân quyền kiểm tra bên trong
        return downloadCvForHr(id, session);
    }

    /**
     * Hiển thị danh sách các hồ sơ ứng tuyển được giao cho Interviewer hiện tại.
     */
    @GetMapping("/interviewer/applications")
    public String listInterviewerApplications(HttpSession session, Model model) {
        // Lấy thông tin người dùng đang đăng nhập từ Session
        User currentUser = (User) session.getAttribute("currentUser");

        // Lấy danh sách lịch phỏng vấn được giao cho Interviewer hiện tại
        List<Interview> interviews = applicationService.getInterviewerInterviews(currentUser.getId());
        
        // Lấy danh sách hồ sơ ứng tuyển từ các cuộc phỏng vấn được giao bằng vòng lặp for đơn giản, tránh trùng lặp
        List<Application> applications = new ArrayList<>();
        for (Interview interview : interviews) {
            Application app = interview.getApplication();
            if (!applications.contains(app)) {
                applications.add(app);
            }
        }

        // Đưa thông tin vào Model để hiển thị ngoài giao diện Thymeleaf
        model.addAttribute("applications", applications);
        model.addAttribute("title", "Đánh giá ứng viên");
        model.addAttribute("activeTab", "interviewer-apps");

        return "hr/application-list";
    }

    /**
     * Nhận kết quả đánh giá phỏng vấn từ người phỏng vấn (SCR-19/SCR-17).
     */
    @PostMapping("/interviewer/interviews/{interviewId}/evaluate")
    public String submitEvaluation(
            @PathVariable Long interviewId,
            @RequestParam("rating") Short rating,
            @RequestParam("feedback") String feedback,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Lấy thông tin người dùng đang đăng nhập từ Session
        User currentUser = (User) session.getAttribute("currentUser");
        
        // Tìm thông tin cuộc phỏng vấn theo ID
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy lịch phỏng vấn"));

        // Kiểm tra bảo mật: Chỉ cho phép người phỏng vấn được phân công thực hiện đánh giá
        if (!interview.getInterviewer().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
        }

        // Cập nhật kết quả đánh giá, nhận xét chi tiết và đổi trạng thái cuộc phỏng vấn thành EVALUATED
        interview.setRating(rating);
        interview.setFeedback(feedback);
        interview.setStatus(InterviewStatus.EVALUATED);
        interview.setEvaluatedAt(java.time.Instant.now());
        
        // Lưu cập nhật vào cơ sở dữ liệu
        interviewRepository.save(interview);

        // Ghi audit log
        com.talenthub.recruitment.entity.AuditLog log = new com.talenthub.recruitment.entity.AuditLog();
        log.setActorUser(currentUser);
        log.setActorUsernameSnapshot(currentUser.getUsername());
        log.setActorFullNameSnapshot(currentUser.getFullName());
        log.setEventType(com.talenthub.recruitment.entity.enums.AuditEventType.EVALUATION_SUBMITTED);
        log.setDescription("Đã nộp đánh giá phỏng vấn #" + interviewId + " - Điểm: " + rating);
        auditLogRepository.save(log);

        redirectAttributes.addFlashAttribute("successMessage", "Đã nộp đánh giá phỏng vấn thành công.");
        return "redirect:/interviewer/applications/" + interview.getApplication().getId();
    }

    /**
     * Xác định định dạng MediaType phù hợp cho tệp tin tải xuống.
     */
    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
