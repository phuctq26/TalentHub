package com.talenthub.recruitment.dto;

import com.talenthub.recruitment.entity.enums.InterviewStatus;

import java.time.Instant;

public class InterviewerInterviewDTO {
    private Long id;
    private Instant scheduledAt;
    private String locationOrMeetingLink;
    private Integer rating;
    private String feedback;
    private Instant evaluatedAt;
    private InterviewStatus status;

    public InterviewerInterviewDTO() {
    }

    public InterviewerInterviewDTO(Long id, Instant scheduledAt, String locationOrMeetingLink, Integer rating, String feedback, Instant evaluatedAt, InterviewStatus status) {
        this.id = id;
        this.scheduledAt = scheduledAt;
        this.locationOrMeetingLink = locationOrMeetingLink;
        this.rating = rating;
        this.feedback = feedback;
        this.evaluatedAt = evaluatedAt;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getLocationOrMeetingLink() {
        return locationOrMeetingLink;
    }

    public void setLocationOrMeetingLink(String locationOrMeetingLink) {
        this.locationOrMeetingLink = locationOrMeetingLink;
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

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public void setStatus(InterviewStatus status) {
        this.status = status;
    }
}
