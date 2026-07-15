package com.talenthub.recruitment.service;

import com.talenthub.recruitment.dto.ApplicationDetailDTO;
import com.talenthub.recruitment.dto.ApplicationListDTO;
import com.talenthub.recruitment.dto.ApplicationNoteDTO;
import com.talenthub.recruitment.dto.InterviewEvaluationDTO;
import com.talenthub.recruitment.dto.PipelineCountsDTO;
import com.talenthub.recruitment.entity.enums.ApplicationStatus;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ApplicationManagementService {

    record DownloadedCv(Resource resource, String fileName, String contentType) {}

    Page<ApplicationListDTO> getApplicationList(Long jobId, String status, int page, int size, Long hrId);

    PipelineCountsDTO getPipelineCounts(Long jobId, Long hrId);

    ApplicationDetailDTO getApplicationDetail(Long applicationId, Long userId, String role);

    List<ApplicationNoteDTO> getInternalNotes(Long applicationId, Long userId, String role);

    List<InterviewEvaluationDTO> getEvaluations(Long applicationId, Long userId, String role);

    List<com.talenthub.recruitment.dto.InterviewerInterviewDTO> getMyInterviews(Long applicationId, Long interviewerId);

    void addNote(Long applicationId, String content, Long hrId);

    void changeStatus(Long applicationId, com.talenthub.recruitment.entity.enums.ApplicationStatus status, Long userId, String role);

    DownloadedCv downloadCv(Long applicationId, Long userId, String role);

    void scheduleInterview(Long applicationId, Long interviewerId, java.time.Instant scheduledAt, String link, com.talenthub.recruitment.entity.User hr);

    List<com.talenthub.recruitment.entity.User> getActiveInterviewers();

    void submitEvaluation(Long interviewId, Integer rating, String feedback, com.talenthub.recruitment.entity.User interviewer);
}
