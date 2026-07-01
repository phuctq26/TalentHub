package com.talenthub.recruitment.service;

import com.talenthub.recruitment.entity.JobPosting;
import org.springframework.data.domain.Page;

public interface PublicJobService {

    Page<JobPosting> getPublicJobs(
            String keyword,
            String department,
            String location,
            int page,
            int size
    );

    JobPosting getPublicJobDetail(Long id);
}
