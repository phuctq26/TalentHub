package com.talenthub.recruitment.dto;

import com.talenthub.recruitment.entity.enums.ApplicationStatus;

import java.time.Instant;

public class ApplicationDetailDTO {
    private Long id;
    private String candidateFullName;
    private String candidateEmail;
    private Instant submissionDate;
    private ApplicationStatus currentStatus;
    private String coverLetter;
    private String cvFileName;
    private Long jobId;
    private String jobTitle;
    private Long jobCreatedById;

    public ApplicationDetailDTO() {
    }

    public ApplicationDetailDTO(Long id, String candidateFullName, String candidateEmail, Instant submissionDate, ApplicationStatus currentStatus, String coverLetter, String cvFileName, Long jobId, String jobTitle, Long jobCreatedById) {
        this.id = id;
        this.candidateFullName = candidateFullName;
        this.candidateEmail = candidateEmail;
        this.submissionDate = submissionDate;
        this.currentStatus = currentStatus;
        this.coverLetter = coverLetter;
        this.cvFileName = cvFileName;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.jobCreatedById = jobCreatedById;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCandidateFullName() {
        return candidateFullName;
    }

    public void setCandidateFullName(String candidateFullName) {
        this.candidateFullName = candidateFullName;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }

    public Instant getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(Instant submissionDate) {
        this.submissionDate = submissionDate;
    }

    public ApplicationStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(ApplicationStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public String getCvFileName() {
        return cvFileName;
    }

    public void setCvFileName(String cvFileName) {
        this.cvFileName = cvFileName;
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

    public Long getJobCreatedById() {
        return jobCreatedById;
    }

    public void setJobCreatedById(Long jobCreatedById) {
        this.jobCreatedById = jobCreatedById;
    }
}
