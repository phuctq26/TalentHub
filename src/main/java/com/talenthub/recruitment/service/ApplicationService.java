package com.talenthub.recruitment.service;

import com.talenthub.recruitment.entity.JobPosting;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.ApplicationStatus;
import com.talenthub.recruitment.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class ApplicationService {

    private static final long MAX_CV_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CV_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final ApplicationRepository applicationRepository;
    private final PublicJobService publicJobService;

    public ApplicationService(ApplicationRepository applicationRepository, PublicJobService publicJobService) {
        this.applicationRepository = applicationRepository;
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
