package com.talenthub.recruitment.dto;

import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.LocalDate;

public class PipelineReportResponse {

    private Long jobId;
    private String jobTitle;
    private String department;
    private String recruiterName;
    private String jobStatus;
    private String numberOfVacancies;
    private Instant postedDate;
    private LocalDate applicationDeadline;
    private long totalApplications;
    private long appliedCount;
    private long screeningCount;
    private long interviewCount;
    private long offeredCount;
    private long hiredCount;
    private long rejectedCount;
    private long withdrawnCount;
    private double screeningRate;
    private double interviewRate;
    private double offerRate;
    private double hiringRate;
    private Page<ApplicationRow> applicationPage;

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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRecruiterName() {
        return recruiterName;
    }

    public void setRecruiterName(String recruiterName) {
        this.recruiterName = recruiterName;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getNumberOfVacancies() {
        return numberOfVacancies;
    }

    public void setNumberOfVacancies(String numberOfVacancies) {
        this.numberOfVacancies = numberOfVacancies;
    }

    public Instant getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(Instant postedDate) {
        this.postedDate = postedDate;
    }

    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(LocalDate applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public long getAppliedCount() {
        return appliedCount;
    }

    public void setAppliedCount(long appliedCount) {
        this.appliedCount = appliedCount;
    }

    public long getScreeningCount() {
        return screeningCount;
    }

    public void setScreeningCount(long screeningCount) {
        this.screeningCount = screeningCount;
    }

    public long getInterviewCount() {
        return interviewCount;
    }

    public void setInterviewCount(long interviewCount) {
        this.interviewCount = interviewCount;
    }

    public long getOfferedCount() {
        return offeredCount;
    }

    public void setOfferedCount(long offeredCount) {
        this.offeredCount = offeredCount;
    }

    public long getHiredCount() {
        return hiredCount;
    }

    public void setHiredCount(long hiredCount) {
        this.hiredCount = hiredCount;
    }

    public long getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(long rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public long getWithdrawnCount() {
        return withdrawnCount;
    }

    public void setWithdrawnCount(long withdrawnCount) {
        this.withdrawnCount = withdrawnCount;
    }

    public double getScreeningRate() {
        return screeningRate;
    }

    public void setScreeningRate(double screeningRate) {
        this.screeningRate = screeningRate;
    }

    public double getInterviewRate() {
        return interviewRate;
    }

    public void setInterviewRate(double interviewRate) {
        this.interviewRate = interviewRate;
    }

    public double getOfferRate() {
        return offerRate;
    }

    public void setOfferRate(double offerRate) {
        this.offerRate = offerRate;
    }

    public double getHiringRate() {
        return hiringRate;
    }

    public void setHiringRate(double hiringRate) {
        this.hiringRate = hiringRate;
    }

    public Page<ApplicationRow> getApplicationPage() {
        return applicationPage;
    }

    public void setApplicationPage(Page<ApplicationRow> applicationPage) {
        this.applicationPage = applicationPage;
    }

    public static class ApplicationRow {

        private String candidateName;
        private String status;
        private Instant appliedDate;
        private Instant lastUpdated;

        public String getCandidateName() {
            return candidateName;
        }

        public void setCandidateName(String candidateName) {
            this.candidateName = candidateName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Instant getAppliedDate() {
            return appliedDate;
        }

        public void setAppliedDate(Instant appliedDate) {
            this.appliedDate = appliedDate;
        }

        public Instant getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
        }
    }
}
