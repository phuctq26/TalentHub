package com.talenthub.recruitment.dto;

import java.time.Instant;

public class InterviewEvaluationDTO {
    private String interviewerName;
    private Instant interviewDate;
    private Integer rating;
    private String feedback;
    private Instant evaluationDate;

    public InterviewEvaluationDTO() {
    }

    public InterviewEvaluationDTO(String interviewerName, Instant interviewDate, Integer rating, String feedback, Instant evaluationDate) {
        this.interviewerName = interviewerName;
        this.interviewDate = interviewDate;
        this.rating = rating;
        this.feedback = feedback;
        this.evaluationDate = evaluationDate;
    }

    public String getInterviewerName() {
        return interviewerName;
    }

    public void setInterviewerName(String interviewerName) {
        this.interviewerName = interviewerName;
    }

    public Instant getInterviewDate() {
        return interviewDate;
    }

    public void setInterviewDate(Instant interviewDate) {
        this.interviewDate = interviewDate;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Instant getEvaluationDate() {
        return evaluationDate;
    }

    public void setEvaluationDate(Instant evaluationDate) {
        this.evaluationDate = evaluationDate;
    }
}
