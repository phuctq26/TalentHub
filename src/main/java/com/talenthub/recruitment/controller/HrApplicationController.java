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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
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

        // Nếu là HR_MANAGER, chỉ lấy các công việc do chính HR đó tạo ra. Nếu là ADMIN,
        // được xem tất cả.
        Long hrManagerId = null;
        if ("HR_MANAGER".equalsIgnoreCase(currentRole)) {
            hrManagerId = currentUser.getId();
        }

        // Lấy danh sách công việc tương ứng để hiển thị trong ô lựa chọn bộ lọc
        // (Dropdown filter)
        List<JobPosting> jobsForFilter = jobPostingService.search(hrManagerId, null, null, null);

        // Lấy danh sách hồ sơ ứng tuyển đã được lọc theo công việc, trạng thái và phân
        // trang
        Page<Application> applicationPage = applicationService.getApplications(jobId, status, page, PAGE_SIZE,
                hrManagerId);

        // Đếm số lượng hồ sơ cho từng tab bằng query count riêng, tránh load toàn bộ
        // entity vào bộ nhớ (sẽ gây LazyInitializationException khi dùng JOIN FETCH)
        long allCount = applicationService.countApplications(jobId, null, hrManagerId);
        long appliedCount = applicationService.countApplications(jobId, "APPLIED", hrManagerId);
        long screeningCount = applicationService.countApplications(jobId, "SCREENING", hrManagerId);
        long interviewCount = applicationService.countApplications(jobId, "INTERVIEW", hrManagerId);
        long offerCount = applicationService.countApplications(jobId, "OFFER", hrManagerId);
        long hiredCount = applicationService.countApplications(jobId, "HIRED", hrManagerId);
        long rejectedCount = applicationService.countApplications(jobId, "REJECTED", hrManagerId);
        long withdrawnCount = applicationService.countApplications(jobId, "WITHDRAWN", hrManagerId);

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
            if (hrManagerId != null) {
                User creator = job.getCreatedBy();
                Long creatorId = (creator != null) ? creator.getId() : null;
                if (creatorId == null || !creatorId.equals(hrManagerId)) {
                    throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
                }
            }
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
        // Tái sử dụng lại phương thức listAllApplications bằng cách truyền trực tiếp
        // jobId vào
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

        // Kiểm tra bảo mật: Nếu là HR Manager, chỉ cho phép xem hồ sơ ứng tuyển của
        // công việc do mình tạo ra
        if (hrManagerId != null) {
            User creator = application.getJob().getCreatedBy();
            Long creatorId = (creator != null) ? creator.getId() : null;
            if (creatorId == null || !creatorId.equals(hrManagerId)) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
            }
        }

        // Lấy danh sách ghi chú nội bộ và lịch sử phỏng vấn của hồ sơ này
        List<ApplicationNote> notes = applicationService.getNotes(id);
        List<Interview> interviews = applicationService.getInterviews(id);

        // Lọc danh sách các buổi phỏng vấn đã đánh giá (EVALUATED) bằng vòng lặp for
        // đơn giản
        List<Interview> evaluations = new ArrayList<>();
        for (Interview interview : interviews) {
            if (interview.getStatus() == InterviewStatus.EVALUATED) {
                evaluations.add(interview);
            }
        }

        // Đưa các thông tin vào Model để hiển thị ngoài giao diện Thymeleaf
        model.addAttribute("app", application);
        model.addAttribute("notes", notes);
        model.addAttribute("interviews", interviews);
        model.addAttribute("evaluations", evaluations);
        model.addAttribute("isInterviewer", false);
        model.addAttribute("title", "Hồ sơ ứng viên: " + application.getCandidate().getFullName());
        model.addAttribute("activeTab", "applications");

        return "hr/application-detail";
    }

    /**
     * Thêm ghi chú nội bộ cho hồ sơ ứng tuyển (SCR-17).
     * Hỗ trợ HTMX để cập nhật danh sách ghi chú mà không cần tải lại toàn bộ trang.
     */
    @PostMapping("/hr/applications/{id}/note")
    public String addApplicationNote(
            @PathVariable Long id,
            @RequestParam("noteContent") String noteContent,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpSession session,
            Model model) {

        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");

        Long hrManagerId = null;
        if ("HR_MANAGER".equalsIgnoreCase(currentRole)) {
            hrManagerId = currentUser.getId();
        }

        Application application = applicationService.getApplicationById(id);

        // Kiểm tra bảo mật: Nếu là HR Manager, chỉ cho phép thêm ghi chú cho hồ sơ thuộc công việc của mình
        if (hrManagerId != null) {
            User creator = application.getJob().getCreatedBy();
            Long creatorId = (creator != null) ? creator.getId() : null;
            if (creatorId == null || !creatorId.equals(hrManagerId)) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
            }
        }

        // Lưu ghi chú nội bộ mới
        applicationService.addNote(id, currentUser, noteContent.trim());

        // Lấy danh sách ghi chú mới để cập nhật cho View
        List<ApplicationNote> notes = applicationService.getNotes(id);
        model.addAttribute("notes", notes);
        model.addAttribute("app", application);

        if (hxRequest != null) {
            return "hr/application-detail :: notes-list";
        }

        return "redirect:/hr/applications/" + id;
    }

    /**
     * Hiển thị chi tiết hồ sơ ứng tuyển dành cho Interviewer được phân công
     * (SCR-17).
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

        // Kiểm tra bảo mật: Kiểm tra xem người phỏng vấn hiện tại có được phân công
        // đánh giá hồ sơ này không
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
        model.addAttribute("app", application);
        model.addAttribute("interviews", interviews);
        model.addAttribute("scheduledInterview", scheduledInterview);
        model.addAttribute("evaluatedInterview", evaluatedInterview);
        model.addAttribute("isInterviewer", true);
        model.addAttribute("title", "Hồ sơ ứng viên: " + application.getCandidate().getFullName());
        model.addAttribute("activeTab", "interviewer-apps");

        return "hr/application-detail";
    }

    /**
     * Xử lý chuyển đổi trạng thái (Advance/Reject) cho hồ sơ ứng tuyển của HR
     * Manager / Admin (SCR-17).
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

        // Kiểm tra bảo mật: Nếu là HR Manager, chỉ cho phép thao tác trên hồ sơ của
        // công việc mình tạo ra
        if (hrManagerId != null) {
            User creator = application.getJob().getCreatedBy();
            Long creatorId = (creator != null) ? creator.getId() : null;
            if (creatorId == null || !creatorId.equals(hrManagerId)) {
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
     * Tải xuống tệp hồ sơ CV tuyển dụng dành cho HR Manager / Admin (SCR-17).
     */
    @GetMapping("/hr/applications/{id}/cv")
    public ResponseEntity<Resource> downloadCvForHr(
            @PathVariable Long id,
            HttpSession session) {

        // Lấy thông tin người dùng đang đăng nhập từ Session
        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");

        // Lấy tệp CV trên máy chủ, hệ thống sẽ kiểm tra quyền truy cập ở tầng dịch vụ
        // (Service layer)
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
                                .toString())
                .body(cvFile.resource());
    }

    /**
     * Tải xuống tệp hồ sơ CV tuyển dụng dành cho Interviewer (SCR-17).
     */
    @GetMapping("/interviewer/applications/{id}/cv")
    public ResponseEntity<Resource> downloadCvForInterviewer(
            @PathVariable Long id,
            HttpSession session) {
        // Tái sử dụng lại phương thức downloadCvForHr để tận dụng phân quyền kiểm tra
        // bên trong
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

        // Lấy danh sách hồ sơ ứng tuyển từ các cuộc phỏng vấn được giao bằng vòng lặp
        // for đơn giản, tránh trùng lặp
        List<Application> applications = new ArrayList<>();
        for (Interview interview : interviews) {
            Application app = interview.getApplication();
            if (!applications.contains(app)) {
                applications.add(app);
            }
        }

        // Đưa thông tin vào Model để hiển thị ngoài giao diện Thymeleaf
        model.addAttribute("applications", applications);
        model.addAttribute("isInterviewer", true);
        model.addAttribute("title", "Đánh giá ứng viên");
        model.addAttribute("activeTab", "interviewer-apps");

        return "hr/application-list";
    }

    /**
     * Hiển thị giao diện nộp đánh giá phỏng vấn (SCR-19).
     */
    @GetMapping("/interviewer/interviews/{interviewId}/evaluate")
    public String showEvaluationForm(
            @PathVariable Long interviewId,
            HttpSession session,
            Model model) {

        User currentUser = (User) session.getAttribute("currentUser");

        // Tìm cuộc phỏng vấn
        Interview interview = interviewRepository.findByIdWithRelations(interviewId)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "Không tìm thấy lịch phỏng vấn"));

        // Bảo mật: Chỉ cho phép người phỏng vấn được phân công
        if (!interview.getInterviewer().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
        }

        // Nếu cuộc phỏng vấn đã được đánh giá rồi, chuyển hướng về chi tiết ứng viên
        if (interview.getStatus() == InterviewStatus.EVALUATED) {
            return "redirect:/interviewer/applications/" + interview.getApplication().getId();
        }

        model.addAttribute("interview", interview);
        model.addAttribute("app", interview.getApplication());
        model.addAttribute("title", "Đánh giá phỏng vấn");
        model.addAttribute("activeTab", "interviewer-apps");

        return "interviewer/evaluate";
    }

    /**
     * Nhận kết quả đánh giá phỏng vấn từ người phỏng vấn (SCR-19/SCR-17).
     */
    @PostMapping("/interviewer/interviews/{interviewId}/evaluate")
    public String submitEvaluation(
            @PathVariable Long interviewId,
            @RequestParam(value = "rating", required = false) Short rating,
            @RequestParam(value = "feedback", required = false) String feedback,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("currentUser");

        Interview interview = interviewRepository.findByIdWithRelations(interviewId)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "Không tìm thấy lịch phỏng vấn"));

        if (!interview.getInterviewer().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
        }

        if (interview.getStatus() == InterviewStatus.EVALUATED) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cuộc phỏng vấn này đã được đánh giá trước đó.");
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

        interview.setRating(rating);
        interview.setFeedback(feedback.trim());
        interview.setStatus(InterviewStatus.EVALUATED);
        interview.setEvaluatedAt(java.time.Instant.now());

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
     * Hiển thị giao diện phân công phỏng vấn (SCR-18).
     */
    @GetMapping("/hr/applications/{id}/assign-interview")
    public String showAssignInterviewForm(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        User currentUser = (User) session.getAttribute("currentUser");
        String currentRole = (String) session.getAttribute("currentRole");

        Long hrManagerId = null;
        if ("HR_MANAGER".equalsIgnoreCase(currentRole)) {
            hrManagerId = currentUser.getId();
        }

        Application application = applicationService.getApplicationById(id);

        // Security check
        if (hrManagerId != null) {
            User creator = application.getJob().getCreatedBy();
            Long creatorId = (creator != null) ? creator.getId() : null;
            if (creatorId == null || !creatorId.equals(hrManagerId)) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
            }
        }

        List<User> interviewers = applicationService.getActiveInterviewers();

        model.addAttribute("app", application);
        model.addAttribute("interviewers", interviewers);
        model.addAttribute("title", "Lên lịch phỏng vấn");
        model.addAttribute("activeTab", "applications");

        return "hr/assign-interview";
    }

    /**
     * Xử lý lên lịch phỏng vấn (SCR-18).
     */
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

        Long hrManagerId = null;
        if ("HR_MANAGER".equalsIgnoreCase(currentRole)) {
            hrManagerId = currentUser.getId();
        }

        Application application = applicationService.getApplicationById(id);

        // Security check
        if (hrManagerId != null) {
            User creator = application.getJob().getCreatedBy();
            Long creatorId = (creator != null) ? creator.getId() : null;
            if (creatorId == null || !creatorId.equals(hrManagerId)) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
            }
        }

        List<User> interviewers = applicationService.getActiveInterviewers();

        // Validation
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
            model.addAttribute("app", application);
            model.addAttribute("interviewers", interviewers);
            model.addAttribute("locationOrMeetingLink", locationOrMeetingLink);
            model.addAttribute("title", "Lên lịch phỏng vấn");
            model.addAttribute("activeTab", "applications");
            return "hr/assign-interview";
        }

        try {
            applicationService.scheduleInterview(id, interviewerId, scheduledAt, locationOrMeetingLink, currentUser);

            // Get interviewer name for flash message
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
            model.addAttribute("app", application);
            model.addAttribute("interviewers", interviewers);
            model.addAttribute("title", "Lên lịch phỏng vấn");
            model.addAttribute("activeTab", "applications");
            return "hr/assign-interview";
        }
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