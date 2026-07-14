package com.talenthub.recruitment.service;

import com.talenthub.recruitment.dto.PipelineReportResponse;
import com.talenthub.recruitment.entity.Application;
import com.talenthub.recruitment.entity.Interview;
import com.talenthub.recruitment.entity.JobPosting;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.ApplicationStatus;
import com.talenthub.recruitment.repository.ApplicationRepository;
import com.talenthub.recruitment.repository.InterviewRepository;
import com.talenthub.recruitment.repository.JobPostingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class PipelineReportService {

    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;

    public PipelineReportService(
            JobPostingRepository jobPostingRepository,
            ApplicationRepository applicationRepository,
            InterviewRepository interviewRepository
    ) {
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
    }

    @Transactional(readOnly = true)
    public List<PipelineReportResponse.JobOption> getAvailableJobs(User currentUser, String currentRole) {
        Long hrManagerId = resolveHrManagerId(currentUser, currentRole);
        return jobPostingRepository.findReportJobsForHrOrAdmin(hrManagerId)
                .stream()
                .map(job -> new PipelineReportResponse.JobOption(job.getId(), job.getTitle()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PipelineReportResponse getPipelineReport(
            Long jobId,
            int page,
            int size,
            User currentUser,
            String currentRole
    ) {
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy job"));
        ensureCanViewJob(job, currentUser, currentRole);

        long total = applicationRepository.countByJobId(jobId);
        long applied = count(jobId, ApplicationStatus.APPLIED);
        long screening = count(jobId, ApplicationStatus.SCREENING);
        long interview = count(jobId, ApplicationStatus.INTERVIEW);
        long offered = count(jobId, ApplicationStatus.OFFER);
        long hired = count(jobId, ApplicationStatus.HIRED);
        long rejected = count(jobId, ApplicationStatus.REJECTED);
        long withdrawn = count(jobId, ApplicationStatus.WITHDRAWN);

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<PipelineReportResponse.ApplicationRow> applicationPage = applicationRepository
                .findByJobId(jobId, pageable)
                .map(this::toApplicationRow);

        PipelineReportResponse response = new PipelineReportResponse();
        response.setJobId(job.getId());
        response.setJobTitle(job.getTitle());
        response.setDepartment(job.getDepartment());
        response.setRecruiterName(resolveRecruiterName(job.getCreatedBy()));
        response.setJobStatus(job.getStatus() != null ? job.getStatus().name() : "N/A");
        response.setNumberOfVacancies("N/A");
        response.setPostedDate(job.getPublishedAt() != null ? job.getPublishedAt() : job.getCreatedAt());
        response.setApplicationDeadline(job.getApplicationDeadline());
        response.setTotalApplications(total);
        response.setAppliedCount(applied);
        response.setScreeningCount(screening);
        response.setInterviewCount(interview);
        response.setOfferedCount(offered);
        response.setHiredCount(hired);
        response.setRejectedCount(rejected);
        response.setWithdrawnCount(withdrawn);
        response.setScreeningRate(calculateRate(screening, applied));
        response.setInterviewRate(calculateRate(interview, screening));
        response.setOfferRate(calculateRate(offered, interview));
        response.setHiringRate(calculateRate(hired, offered));
        response.setMaxStageCount(resolveMaxStageCount(applied, screening, interview, offered, hired, rejected, withdrawn));
        response.setStageRows(List.of(
                new PipelineReportResponse.StageRow("Applied", applied, response.getMaxStageCount()),
                new PipelineReportResponse.StageRow("Screening", screening, response.getMaxStageCount()),
                new PipelineReportResponse.StageRow("Interview", interview, response.getMaxStageCount()),
                new PipelineReportResponse.StageRow("Offer", offered, response.getMaxStageCount()),
                new PipelineReportResponse.StageRow("Hired", hired, response.getMaxStageCount()),
                new PipelineReportResponse.StageRow("Rejected", rejected, response.getMaxStageCount()),
                new PipelineReportResponse.StageRow("Withdrawn", withdrawn, response.getMaxStageCount())
        ));
        response.setApplicationPage(applicationPage);
        return response;
    }

    private long count(Long jobId, ApplicationStatus status) {
        return applicationRepository.countByJobIdAndStatus(jobId, status.name());
    }

    private double calculateRate(long next, long previous) {
        if (previous == 0) {
            return 0;
        }
        return Math.round(((double) next / previous * 100) * 10) / 10.0;
    }

    private PipelineReportResponse.ApplicationRow toApplicationRow(Application application) {
        PipelineReportResponse.ApplicationRow row = new PipelineReportResponse.ApplicationRow();
        User candidate = application.getCandidate();
        row.setCandidateName(candidate != null ? candidate.getFullName() : "N/A");
        row.setStatus(application.getStatus() != null ? application.getStatus().name() : "N/A");
        row.setDaysInStage(calculateDaysInStage(application));
        row.setAssignedInterviewer(resolveInterviewer(application.getId()));
        row.setAppliedDate(application.getSubmittedAt());
        row.setLastUpdated(application.getStatusChangedAt());
        return row;
    }

    private String resolveRecruiterName(User recruiter) {
        if (recruiter == null) {
            return "N/A";
        }
        return recruiter.getFullName();
    }

    private long resolveMaxStageCount(long... counts) {
        long max = 0;
        for (long count : counts) {
            max = Math.max(max, count);
        }
        return max;
    }

    private long calculateDaysInStage(Application application) {
        Instant stageStartedAt = application.getStatusChangedAt() != null
                ? application.getStatusChangedAt()
                : application.getSubmittedAt();
        if (stageStartedAt == null) {
            return 0;
        }
        return Math.max(Duration.between(stageStartedAt, Instant.now()).toDays(), 0);
    }

    private String resolveInterviewer(Long applicationId) {
        return interviewRepository.findFirstByApplication_IdOrderByScheduledAtAsc(applicationId)
                .map(Interview::getInterviewer)
                .map(User::getFullName)
                .orElse("-");
    }

    private Long resolveHrManagerId(User currentUser, String currentRole) {
        if ("ADMIN".equals(currentRole)) {
            return null;
        }
        if ("HR_MANAGER".equals(currentRole) && currentUser != null) {
            return currentUser.getId();
        }
        throw new IllegalArgumentException("Không có quyền xem báo cáo");
    }

    private void ensureCanViewJob(JobPosting job, User currentUser, String currentRole) {
        if ("ADMIN".equals(currentRole)) {
            return;
        }
        if (!"HR_MANAGER".equals(currentRole) || currentUser == null) {
            throw new IllegalArgumentException("Không có quyền xem báo cáo");
        }
        User createdBy = job.getCreatedBy();
        if (createdBy == null || !currentUser.getId().equals(createdBy.getId())) {
            throw new IllegalArgumentException("Không có quyền xem báo cáo");
        }
    }

}
