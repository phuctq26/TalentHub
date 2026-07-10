package com.talenthub.recruitment.repository;

import com.talenthub.recruitment.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByJob_IdAndCandidate_Id(Long jobId, Long candidateId);

    @Modifying
    @Query(value = """
            INSERT INTO applications (
                job_id,
                candidate_id,
                status,
                cv_file_name,
                cv_content_type,
                cv_size_bytes,
                cv_storage_path,
                cover_letter
            )
            VALUES (
                :jobId,
                :candidateId,
                CAST(:status AS application_status),
                :cvFileName,
                :cvContentType,
                :cvSizeBytes,
                :cvStoragePath,
                :coverLetter
            )
            """, nativeQuery = true)
    void insertApplication(
            @Param("jobId") Long jobId,
            @Param("candidateId") Long candidateId,
            @Param("status") String status,
            @Param("cvFileName") String cvFileName,
            @Param("cvContentType") String cvContentType,
            @Param("cvSizeBytes") Long cvSizeBytes,
            @Param("cvStoragePath") String cvStoragePath,
            @Param("coverLetter") String coverLetter
    );

    @Query(value = "SELECT COUNT(*) FROM applications WHERE CAST(status AS TEXT) = 'INTERVIEW'", nativeQuery = true)
    long countInterviewStage();

    @Query(value = "SELECT COUNT(*) FROM applications WHERE CAST(status AS TEXT) = 'HIRED'", nativeQuery = true)
    long countHired();

    @Query("SELECT a FROM Application a JOIN FETCH a.candidate JOIN FETCH a.job ORDER BY a.submittedAt DESC")
    List<Application> findRecentApplications(org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT COUNT(a.id) FROM applications a JOIN job_postings j ON j.id = a.job_id WHERE CAST(a.status AS TEXT) = 'APPLIED' AND (:hrManagerId IS NULL OR j.created_by_id = :hrManagerId)", nativeQuery = true)
    long countAwaitingReviewForHrOrAdmin(@Param("hrManagerId") Long hrManagerId);

    @Query(value = "SELECT COUNT(*) FROM applications WHERE job_id = :jobId", nativeQuery = true)
    long countByJobId(@Param("jobId") Long jobId);
}
