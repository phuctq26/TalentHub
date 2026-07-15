package com.talenthub.recruitment.service.impl;

import com.talenthub.recruitment.dto.ApplicationDetailDTO;
import com.talenthub.recruitment.dto.ApplicationListDTO;
import com.talenthub.recruitment.dto.ApplicationNoteDTO;
import com.talenthub.recruitment.dto.InterviewEvaluationDTO;
import com.talenthub.recruitment.dto.PipelineCountsDTO;
import com.talenthub.recruitment.entity.Application;
import com.talenthub.recruitment.entity.ApplicationNote;
import com.talenthub.recruitment.entity.Interview;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.ApplicationStatus;
import com.talenthub.recruitment.entity.enums.InterviewStatus;
import com.talenthub.recruitment.repository.ApplicationNoteRepository;
import com.talenthub.recruitment.repository.ApplicationRepository;
import com.talenthub.recruitment.repository.InterviewRepository;
import com.talenthub.recruitment.repository.UserRepository;
import com.talenthub.recruitment.service.ApplicationManagementService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApplicationManagementServiceImpl implements ApplicationManagementService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationNoteRepository applicationNoteRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    public ApplicationManagementServiceImpl(
            ApplicationRepository applicationRepository,
            ApplicationNoteRepository applicationNoteRepository,
            InterviewRepository interviewRepository,
            UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.applicationNoteRepository = applicationNoteRepository;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationListDTO> getApplicationList(Long jobId, String status, int page, int size, Long hrId) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        String statusFilter = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();

        Page<Application> applications = applicationRepository.findByJobIdAndStatus(jobId, hrId, statusFilter, pageable);

        return applications.map(app -> new ApplicationListDTO(
                app.getId(),
                app.getCandidate().getFullName(),
                app.getCandidate().getEmail(),
                app.getSubmittedAt(),
                app.getDaysInStage(),
                app.getStatus(),
                app.getJob().getId(),
                app.getJob().getTitle(),
                app.getJob().getDepartment()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PipelineCountsDTO getPipelineCounts(Long jobId, Long hrId) {
        long allCount = applicationRepository.countByFiltersAndStatus(jobId, hrId, null);
        long appliedCount = applicationRepository.countByFiltersAndStatus(jobId, hrId, "APPLIED");
        long screeningCount = applicationRepository.countByFiltersAndStatus(jobId, hrId, "SCREENING");
        long interviewCount = applicationRepository.countByFiltersAndStatus(jobId, hrId, "INTERVIEW");
        long offerCount = applicationRepository.countByFiltersAndStatus(jobId, hrId, "OFFER");
        long hiredCount = applicationRepository.countByFiltersAndStatus(jobId, hrId, "HIRED");
        long rejectedCount = applicationRepository.countByFiltersAndStatus(jobId, hrId, "REJECTED");
        long withdrawnCount = applicationRepository.countByFiltersAndStatus(jobId, hrId, "WITHDRAWN");

        return new PipelineCountsDTO(allCount, appliedCount, screeningCount, interviewCount, offerCount, hiredCount, rejectedCount, withdrawnCount);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDetailDTO getApplicationDetail(Long applicationId, Long userId, String role) {
        Application app = getValidatedApplication(applicationId, userId, role);

        Long creatorId = null;
        if (app.getJob() != null && app.getJob().getCreatedBy() != null) {
            creatorId = app.getJob().getCreatedBy().getId();
        }

        return new ApplicationDetailDTO(
                app.getId(),
                app.getCandidate().getFullName(),
                app.getCandidate().getEmail(),
                app.getSubmittedAt(),
                app.getStatus(),
                app.getCoverLetter(),
                app.getCvFileName(),
                app.getJob().getId(),
                app.getJob().getTitle(),
                creatorId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationNoteDTO> getInternalNotes(Long applicationId, Long userId, String role) {
        // Validation check for access
        getValidatedApplication(applicationId, userId, role);

        List<ApplicationNote> notes = applicationNoteRepository.findByApplication_IdOrderByCreatedAtDesc(applicationId);
        List<ApplicationNoteDTO> dtos = new ArrayList<>();
        for (ApplicationNote note : notes) {
            String authorName = note.getAuthor() != null ? note.getAuthor().getFullName() : "Unknown";
            dtos.add(new ApplicationNoteDTO(note.getId(), authorName, note.getCreatedAt(), note.getNoteContent()));
        }
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewEvaluationDTO> getEvaluations(Long applicationId, Long userId, String role) {
        getValidatedApplication(applicationId, userId, role);

        List<Interview> interviews = interviewRepository.findByApplication_IdOrderByScheduledAtDesc(applicationId);
        List<InterviewEvaluationDTO> dtos = new ArrayList<>();
        for (Interview interview : interviews) {
            if (interview.getStatus() == com.talenthub.recruitment.entity.enums.InterviewStatus.EVALUATED) {
                Integer ratingInt = interview.getRating() != null ? interview.getRating().intValue() : null;
                dtos.add(new InterviewEvaluationDTO(
                        interview.getInterviewer().getFullName(),
                        interview.getScheduledAt(),
                        ratingInt,
                        interview.getFeedback(),
                        interview.getEvaluatedAt()
                ));
            }
        }
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.talenthub.recruitment.dto.InterviewerInterviewDTO> getMyInterviews(Long applicationId, Long interviewerId) {
        List<Interview> interviews = interviewRepository.findByApplication_IdOrderByScheduledAtDesc(applicationId);
        List<com.talenthub.recruitment.dto.InterviewerInterviewDTO> dtos = new ArrayList<>();
        for (Interview interview : interviews) {
            if (interview.getInterviewer().getId().equals(interviewerId)) {
                Integer ratingInt = interview.getRating() != null ? interview.getRating().intValue() : null;
                dtos.add(new com.talenthub.recruitment.dto.InterviewerInterviewDTO(
                        interview.getId(),
                        interview.getScheduledAt(),
                        interview.getLocationOrMeetingLink(),
                        ratingInt,
                        interview.getFeedback(),
                        interview.getEvaluatedAt(),
                        interview.getStatus()
                ));
            }
        }
        return dtos;
    }

    @Override
    @Transactional
    public void addNote(Long applicationId, String content, Long hrId) {
        Application app = getValidatedApplication(applicationId, hrId, "HR_MANAGER");
        User author = userRepository.findById(hrId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ApplicationNote note = new ApplicationNote();
        note.setApplication(app);
        note.setAuthor(author);
        note.setNoteContent(content);
        note.setCreatedAt(Instant.now());

        applicationNoteRepository.save(note);
    }

    @Override
    @Transactional
    public void changeStatus(Long applicationId, ApplicationStatus status, Long userId, String role) {
        Application app = getValidatedApplication(applicationId, userId, role);
        
        Instant now = Instant.now();
        Instant withdrawnAt = (status == ApplicationStatus.WITHDRAWN) ? now : app.getWithdrawnAt();

        applicationRepository.updateApplicationStatus(applicationId, status.name(), now, withdrawnAt);
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadedCv downloadCv(Long applicationId, Long userId, String role) {
        Application app = getValidatedApplication(applicationId, userId, role);

        Path cvPath = Paths.get(app.getCvStoragePath()).toAbsolutePath().normalize();
        if (!Files.exists(cvPath) || !Files.isReadable(cvPath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV file not found");
        }

        return new DownloadedCv(
                new FileSystemResource(cvPath),
                app.getCvFileName(),
                app.getCvContentType()
        );
    }

    private Application getValidatedApplication(Long applicationId, Long userId, String role) {
        Application application = applicationRepository.findByIdWithRelations(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        if ("HR_MANAGER".equalsIgnoreCase(role)) {
            User creator = application.getJob().getCreatedBy();
            if (creator == null || !creator.getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        } else if ("INTERVIEWER".equalsIgnoreCase(role)) {
            List<Interview> interviews = interviewRepository.findByApplication_IdOrderByScheduledAtDesc(applicationId);
            boolean isAssigned = false;
            for (Interview i : interviews) {
                if (i.getInterviewer().getId().equals(userId)) {
                    isAssigned = true;
                    break;
                }
            }
            if (!isAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }
        return application;
    }

    @Override
    @Transactional
    public void scheduleInterview(Long applicationId, Long interviewerId, Instant scheduledAt, String link, User hr) {
        Application application = getValidatedApplication(applicationId, hr.getId(), "HR_MANAGER");

        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interviewer not found"));

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setInterviewer(interviewer);
        interview.setScheduledAt(scheduledAt);
        interview.setLocationOrMeetingLink(link);
        interview.setStatus(InterviewStatus.SCHEDULED);

        interviewRepository.save(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getActiveInterviewers() {
        return userRepository.findByRoleNameAndStatus("INTERVIEWER", "ACTIVE");
    }

    @Override
    @Transactional
    public void submitEvaluation(Long interviewId, Integer rating, String feedback, User interviewer) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview not found"));

        if (!interview.getInterviewer().getId().equals(interviewer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (interview.getStatus() == InterviewStatus.EVALUATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evaluation already submitted");
        }

        interview.setRating(rating != null ? rating.shortValue() : null);
        interview.setFeedback(feedback);
        interview.setStatus(InterviewStatus.EVALUATED);
        interview.setEvaluatedAt(Instant.now());

        interviewRepository.save(interview);
    }
}
