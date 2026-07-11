package com.talenthub.recruitment.service.impl;

import com.talenthub.recruitment.dto.JobPostingForm;
import com.talenthub.recruitment.entity.JobPosting;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.JobStatus;
import com.talenthub.recruitment.repository.ApplicationRepository;
import com.talenthub.recruitment.repository.JobPostingRepository;
import com.talenthub.recruitment.service.JobPostingService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class JobPostingServiceImpl implements JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;

    public JobPostingServiceImpl(JobPostingRepository jobPostingRepository, ApplicationRepository applicationRepository) {
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public List<JobPosting> search(
            Long createdById,
            String keyword,
            JobStatus status,
            String department
    ) {
        String statusText = status == null ? null : status.name();

        return jobPostingRepository.search(
                createdById,
                keyword,
                statusText,
                department
        );
    }

    @Override
    public JobPosting findById(Long id) {
        return jobPostingRepository.findById(id).
                orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng"));
    }

    @Override
    public JobPostingForm getFormById(Long id) {
        JobPosting job = findById(id);

        JobPostingForm form = new JobPostingForm();

        form.setId(job.getId());
        form.setTitle(job.getTitle());
        form.setDepartment(job.getDepartment());
        form.setLocation(job.getLocation());
        form.setDescription(job.getDescription());
        form.setRequirements(job.getRequirements());
        form.setSalaryRange(job.getSalaryRange());
        form.setApplicationDeadline(job.getApplicationDeadline());

        return form;
    }

    @Override
    public JobPosting saveDraft(JobPostingForm form, User curentUser) {
        JobPosting job;

        if (form.getId() == null) {
            job = new JobPosting();
            job.setCreatedBy(curentUser);
            job.setStatus(JobStatus.DRAFT);
        } else {
            job = findById(form.getId());
        }

        job.setTitle(form.getTitle());
        job.setDepartment(form.getDepartment());
        job.setLocation(form.getLocation());
        job.setDescription(form.getDescription());
        job.setRequirements(form.getRequirements());
        job.setSalaryRange(form.getSalaryRange());
        job.setApplicationDeadline(form.getApplicationDeadline());

        return jobPostingRepository.save(job);
    }


    @Override
    public JobPosting saveAndPublish(JobPostingForm form, User currentUser) {
        JobPosting job = saveDraft(form, currentUser);

        if (job.getStatus() == JobStatus.DRAFT) {

            job.setStatus(JobStatus.ACTIVE);

            job.setPublishedAt(Instant.now());
        }

        return jobPostingRepository.save(job);
    }

    @Override
    public void publish(Long id) {
        JobPosting job = findById(id);

        if (job.getStatus() != JobStatus.DRAFT) {

            throw new RuntimeException("Chỉ có thể đăng tin ở trạng thái nháp.");
        }

        job.setStatus(JobStatus.ACTIVE);

        job.setPublishedAt(Instant.now());

        jobPostingRepository.save(job);
    }

    @Override
    public void close(Long id) {
        JobPosting job = findById(id);

        if (job.getStatus() != JobStatus.ACTIVE) {

            throw new RuntimeException("Chỉ có thể đóng tin đang hoạt động.");
        }

        job.setStatus(JobStatus.CLOSED);

        job.setClosedAt(Instant.now());

        jobPostingRepository.save(job);
    }

    @Override
    public void delete(Long id) {
        JobPosting job = findById(id);

        if (job.getStatus() != JobStatus.DRAFT) {

            throw new RuntimeException("Chỉ được xóa tin ở trạng thái nháp.");
        }

        if (applicationRepository.countByJobId(id) > 0) {
            throw new RuntimeException("Không thể xóa tin tuyển dụng đã có hồ sơ ứng tuyển.");
        }

        jobPostingRepository.delete(job);
    }
}
