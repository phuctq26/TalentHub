package com.talenthub.recruitment.dto;

import com.talenthub.recruitment.entity.enums.ApplicationStatus;

import java.time.Instant;

public class ApplicationListDTO {
    private Long id;
    private String candidateFullName;
    private String candidateEmail;
    private Instant submissionDate;
    private long daysInCurrentStage;
    private ApplicationStatus currentStatus;
    private Long jobId;
    private String jobTitle;
    private String jobDepartment;

    public ApplicationListDTO() {
    }

    public ApplicationListDTO(Long id, String candidateFullName, String candidateEmail, Instant submissionDate, long daysInCurrentStage, ApplicationStatus currentStatus, Long jobId, String jobTitle, String jobDepartment) {
        this.id = id;
        this.candidateFullName = candidateFullName;
        this.candidateEmail = candidateEmail;
        this.submissionDate = submissionDate;
        this.daysInCurrentStage = daysInCurrentStage;
        this.currentStatus = currentStatus;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.jobDepartment = jobDepartment;
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

    public long getDaysInCurrentStage() {
        return daysInCurrentStage;
    }

    public void setDaysInCurrentStage(long daysInCurrentStage) {
        this.daysInCurrentStage = daysInCurrentStage;
    }

    public ApplicationStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(ApplicationStatus currentStatus) {
        this.currentStatus = currentStatus;
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

    public String getJobDepartment() {
        return jobDepartment;
    }

    public void setJobDepartment(String jobDepartment) {
        this.jobDepartment = jobDepartment;
    }
}
