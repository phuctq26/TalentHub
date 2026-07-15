package com.talenthub.recruitment.dto;

public class PipelineCountsDTO {
    private long allCount;
    private long appliedCount;
    private long screeningCount;
    private long interviewCount;
    private long offerCount;
    private long hiredCount;
    private long rejectedCount;
    private long withdrawnCount;

    public PipelineCountsDTO() {
    }

    public PipelineCountsDTO(long allCount, long appliedCount, long screeningCount, long interviewCount, long offerCount, long hiredCount, long rejectedCount, long withdrawnCount) {
        this.allCount = allCount;
        this.appliedCount = appliedCount;
        this.screeningCount = screeningCount;
        this.interviewCount = interviewCount;
        this.offerCount = offerCount;
        this.hiredCount = hiredCount;
        this.rejectedCount = rejectedCount;
        this.withdrawnCount = withdrawnCount;
    }

    public long getAllCount() {
        return allCount;
    }

    public void setAllCount(long allCount) {
        this.allCount = allCount;
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

    public long getOfferCount() {
        return offerCount;
    }

    public void setOfferCount(long offerCount) {
        this.offerCount = offerCount;
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
}
