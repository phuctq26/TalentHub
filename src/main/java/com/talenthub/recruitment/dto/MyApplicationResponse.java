package com.talenthub.recruitment.dto;

import com.talenthub.recruitment.entity.Application;
import com.talenthub.recruitment.entity.Interview;
import com.talenthub.recruitment.entity.JobPosting;

import java.time.Instant;

public class MyApplicationResponse {

    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private Instant appliedAt;
    private String status;
    private Instant updatedAt;
    private Instant interviewDate;
    private String cvFileName;
    private String cvContentType;
    private Long cvSizeBytes;

    public static MyApplicationResponse fromEntity(Application application, Interview interview) {
        JobPosting job = application.getJob();

        MyApplicationResponse response = new MyApplicationResponse();
        response.setApplicationId(application.getId());
        response.setJobId(job != null ? job.getId() : null);
        response.setJobTitle(job != null ? job.getTitle() : "N/A");
        response.setCompanyName("N/A");
        response.setAppliedAt(application.getSubmittedAt());
        response.setStatus(application.getStatus() != null ? application.getStatus().name() : "N/A");
        response.setUpdatedAt(application.getStatusChangedAt());
        response.setInterviewDate(interview != null ? interview.getScheduledAt() : null);
        response.setCvFileName(application.getCvFileName());
        response.setCvContentType(application.getCvContentType());
        response.setCvSizeBytes(application.getCvSizeBytes());
        return response;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getInterviewDate() {
        return interviewDate;
    }

    public void setInterviewDate(Instant interviewDate) {
        this.interviewDate = interviewDate;
    }

    public String getCvFileName() {
        return cvFileName;
    }

    public void setCvFileName(String cvFileName) {
        this.cvFileName = cvFileName;
    }

    public String getCvContentType() {
        return cvContentType;
    }

    public void setCvContentType(String cvContentType) {
        this.cvContentType = cvContentType;
    }

    public Long getCvSizeBytes() {
        return cvSizeBytes;
    }

    public void setCvSizeBytes(Long cvSizeBytes) {
        this.cvSizeBytes = cvSizeBytes;
    }

    public String getCvDisplayType() {
        if ("application/pdf".equals(cvContentType)) {
            return "PDF";
        }
        if ("application/msword".equals(cvContentType)) {
            return "DOC";
        }
        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(cvContentType)) {
            return "DOCX";
        }
        return "CV";
    }

    public String getCvDisplaySize() {
        if (cvSizeBytes == null) {
            return "-";
        }
        double megabytes = cvSizeBytes / (1024.0 * 1024.0);
        return String.format("%.1f MB", megabytes);
    }
}
