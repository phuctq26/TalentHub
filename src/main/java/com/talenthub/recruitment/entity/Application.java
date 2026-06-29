package com.talenthub.recruitment.entity;

import com.talenthub.recruitment.entity.enums.ApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(
    name = "applications",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_applications_job_candidate",
        columnNames = {"job_id", "candidate_id"}
    )
)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPosting job;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "application_status")
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @NotBlank
    @Size(max = 255)
    @Column(name = "cv_file_name", nullable = false)
    private String cvFileName;

    @NotBlank
    @Size(max = 120)
    @Column(name = "cv_content_type", nullable = false, length = 120)
    private String cvContentType;

    @Min(1)
    @Column(name = "cv_size_bytes", nullable = false)
    private Long cvSizeBytes;

    @NotBlank
    @Size(max = 500)
    @Column(name = "cv_storage_path", nullable = false, length = 500)
    private String cvStoragePath;

    @Column(name = "cover_letter", columnDefinition = "text")
    private String coverLetter;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        submittedAt = now;
        statusChangedAt = now;
        if (status == null) {
            status = ApplicationStatus.APPLIED;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JobPosting getJob() {
        return job;
    }

    public void setJob(JobPosting job) {
        this.job = job;
    }

    public User getCandidate() {
        return candidate;
    }

    public void setCandidate(User candidate) {
        this.candidate = candidate;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    public void setStatusChangedAt(Instant statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(Instant withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
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

    public String getCvStoragePath() {
        return cvStoragePath;
    }

    public void setCvStoragePath(String cvStoragePath) {
        this.cvStoragePath = cvStoragePath;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }
}
