package com.talenthub.recruitment.service;

import com.talenthub.recruitment.dto.MyApplicationResponse;
import com.talenthub.recruitment.entity.JobPosting;
import com.talenthub.recruitment.entity.Application;
import com.talenthub.recruitment.entity.Interview;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.ApplicationStatus;
import com.talenthub.recruitment.repository.ApplicationRepository;
import com.talenthub.recruitment.repository.InterviewRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ApplicationService {

    private static final long MAX_CV_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CV_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final PublicJobService publicJobService;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            InterviewRepository interviewRepository,
            PublicJobService publicJobService
    ) {
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
        this.publicJobService = publicJobService;
    }

    @Transactional
    public void apply(Long jobId, User candidate, MultipartFile cvFile, String coverLetter) {
        JobPosting job = publicJobService.getPublicJobDetail(jobId);
        validateCandidate(candidate);
        validateCv(cvFile);

        if (applicationRepository.existsByJob_IdAndCandidate_Id(job.getId(), candidate.getId())) {
            throw new IllegalStateException("You have already applied for this job.");
        }

        String storagePath = storeCv(cvFile, job.getId(), candidate.getId());

        applicationRepository.insertApplication(
                job.getId(),
                candidate.getId(),
                ApplicationStatus.APPLIED.name(),
                cvFile.getOriginalFilename(),
                cvFile.getContentType(),
                cvFile.getSize(),
                storagePath,
                coverLetter
        );
    }

    @Transactional(readOnly = true)
    public Page<MyApplicationResponse> getMyApplications(
            Long candidateId,
            String status,
            int page,
            int size
    ) {
        validateCandidateId(candidateId);

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        ApplicationStatus applicationStatus = parseStatus(status);

        Page<Application> applicationPage = applicationStatus == null
                ? applicationRepository.findMyApplications(candidateId, pageable)
                : applicationRepository.findMyApplicationsByStatus(
                        candidateId,
                        applicationStatus.name(),
                        pageable
                );

        return applicationPage.map(this::toMyApplicationResponse);
    }

    @Transactional(readOnly = true)
    public MyApplicationResponse getMyApplicationDetail(Long candidateId, Long applicationId) {
        validateCandidateId(candidateId);
        Application application = applicationRepository.findByIdAndCandidate_Id(applicationId, candidateId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Application not found"));
        return toMyApplicationResponse(application);
    }

    @Transactional
    public void withdraw(Long candidateId, Long applicationId) {
        validateCandidateId(candidateId);
        Application application = applicationRepository.findByIdAndCandidate_Id(applicationId, candidateId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Application not found"));

        if (!canWithdraw(application.getStatus())) {
            throw new IllegalStateException("This application cannot be withdrawn at its current status.");
        }

        Instant now = Instant.now();
        applicationRepository.withdrawApplication(
                applicationId,
                candidateId,
                ApplicationStatus.WITHDRAWN.name(),
                now,
                now
        );
    }

    @Transactional(readOnly = true)
    public CandidateCvFile getCandidateCv(Long candidateId, Long applicationId) {
        validateCandidateId(candidateId);
        Application application = applicationRepository.findByIdAndCandidate_Id(applicationId, candidateId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Application not found"));

        Path cvPath = Paths.get(application.getCvStoragePath()).toAbsolutePath().normalize();
        if (!Files.exists(cvPath) || !Files.isReadable(cvPath)) {
            throw new ResponseStatusException(NOT_FOUND, "CV file not found");
        }

        return new CandidateCvFile(
                new FileSystemResource(cvPath),
                application.getCvFileName(),
                application.getCvContentType()
        );
    }

    public record CandidateCvFile(Resource resource, String fileName, String contentType) {
    }

    private MyApplicationResponse toMyApplicationResponse(Application application) {
        Interview interview = interviewRepository
                .findFirstByApplication_IdOrderByScheduledAtAsc(application.getId())
                .orElse(null);
        return MyApplicationResponse.fromEntity(application, interview);
    }

    private ApplicationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ApplicationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean canWithdraw(ApplicationStatus status) {
        return status == ApplicationStatus.APPLIED || status == ApplicationStatus.SCREENING;
    }

    private void validateCandidateId(Long candidateId) {
        if (candidateId == null) {
            throw new IllegalStateException("Candidate is required.");
        }
    }

    private void validateCandidate(User candidate) {
        if (candidate == null || candidate.getId() == null) {
            throw new IllegalStateException("Candidate is required.");
        }
    }

    private void validateCv(MultipartFile cvFile) {
        if (cvFile == null || cvFile.isEmpty()) {
            throw new IllegalArgumentException("CV file is required.");
        }
        if (cvFile.getSize() > MAX_CV_SIZE_BYTES) {
            throw new IllegalArgumentException("CV file must be 5MB or smaller.");
        }
        if (!ALLOWED_CV_TYPES.contains(cvFile.getContentType())) {
            throw new IllegalArgumentException("CV file must be PDF, DOC, or DOCX.");
        }
    }

    private String storeCv(MultipartFile cvFile, Long jobId, Long candidateId) {
        try {
            Path uploadDir = Paths.get("uploads", "cv").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            String originalFileName = cvFile.getOriginalFilename() == null ? "cv" : cvFile.getOriginalFilename();
            String safeFileName = originalFileName.replaceAll("[^A-Za-z0-9._-]", "_");
            String storedFileName = jobId + "-" + candidateId + "-" + UUID.randomUUID() + "-" + safeFileName;
            Path destination = uploadDir.resolve(storedFileName).normalize();

            cvFile.transferTo(destination);
            return destination.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save CV file.", ex);
        }
    }
}
