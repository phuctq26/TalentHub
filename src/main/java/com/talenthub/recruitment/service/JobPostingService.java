package com.talenthub.recruitment.service;

import com.talenthub.recruitment.dto.JobPostingForm;
import com.talenthub.recruitment.entity.JobPosting;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.JobStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface JobPostingService {

    List<JobPosting> search(
            Long createdById,
            String keyword,
            JobStatus status,
            String department
    );
    JobPosting saveDraft(JobPostingForm form);

    JobPosting saveAndPublish(JobPostingForm form);

    JobPosting findById(Long id);

    JobPostingForm getFormById(Long id);

    JobPosting saveDraft(JobPostingForm form, User curentUser);

    JobPosting saveAndPublish(JobPostingForm form, User currentUser);

    void publish(Long id);

    void close(Long id);

    void delete(Long id);
}
