package com.talenthub.recruitment.service.impl;

import com.talenthub.recruitment.entity.JobPosting;
import com.talenthub.recruitment.repository.JobPostingRepository;
import com.talenthub.recruitment.service.PublicJobService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class PublicJobServiceImpl implements PublicJobService {

    private final JobPostingRepository jobPostingRepository;

    public PublicJobServiceImpl(JobPostingRepository jobPostingRepository) {
        this.jobPostingRepository = jobPostingRepository;
    }

    @Override
    public Page<JobPosting> getPublicJobs(
            String keyword,
            String department,
            String location,
            int page,
            int size
    ) {
        return jobPostingRepository.searchPublicJobs(
                keyword,
                department,
                location,
                PageRequest.of(Math.max(page, 0), size)
        );
    }

    @Override
    public JobPosting getPublicJobDetail(Long id) {
        return jobPostingRepository.findPublicJobById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Job not found"));
    }
}
