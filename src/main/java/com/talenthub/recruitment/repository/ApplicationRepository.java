package com.talenthub.recruitment.repository;

import com.talenthub.recruitment.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByJob_IdAndCandidate_Id(Long jobId, Long candidateId);

    @Query(
            value = """
                    SELECT a.*
                    FROM applications a
                    WHERE a.candidate_id = :candidateId
                    ORDER BY a.submitted_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM applications a
                    WHERE a.candidate_id = :candidateId
                    """,
            nativeQuery = true
    )
    Page<Application> findMyApplications(
            @Param("candidateId") Long candidateId,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT a.*
                    FROM applications a
                    WHERE a.candidate_id = :candidateId
                      AND CAST(a.status AS TEXT) = :status
                    ORDER BY a.submitted_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM applications a
                    WHERE a.candidate_id = :candidateId
                      AND CAST(a.status AS TEXT) = :status
                    """,
            nativeQuery = true
    )
    Page<Application> findMyApplicationsByStatus(
            @Param("candidateId") Long candidateId,
            @Param("status") String status,
            Pageable pageable
    );

    Optional<Application> findByIdAndCandidate_Id(Long id, Long candidateId);

    @Modifying
    @Query(value = """
            UPDATE applications
            SET status = CAST(:status AS application_status),
                status_changed_at = :statusChangedAt,
                withdrawn_at = :withdrawnAt
            WHERE id = :applicationId
              AND candidate_id = :candidateId
            """, nativeQuery = true)
    int withdrawApplication(
            @Param("applicationId") Long applicationId,
            @Param("candidateId") Long candidateId,
            @Param("status") String status,
            @Param("statusChangedAt") Instant statusChangedAt,
            @Param("withdrawnAt") Instant withdrawnAt
    );

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

    @Query(value = """
            SELECT COUNT(*)
            FROM applications
            WHERE job_id = :jobId
              AND CAST(status AS TEXT) = :status
            """, nativeQuery = true)
    long countByJobIdAndStatus(
            @Param("jobId") Long jobId,
            @Param("status") String status
    );

    @Query(
            value = """
                    SELECT *
                    FROM applications
                    WHERE job_id = :jobId
                    ORDER BY submitted_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM applications
                    WHERE job_id = :jobId
                    """,
            nativeQuery = true
    )
    Page<Application> findByJobId(
            @Param("jobId") Long jobId,
            Pageable pageable
    );

    @Query("SELECT a FROM Application a " +
           "JOIN FETCH a.job j " +
           "JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH j.createdBy u " +
           "WHERE a.id = :id")
    Optional<Application> findByIdWithRelations(@Param("id") Long id);

    @Query("SELECT a FROM Application a " +
           "JOIN FETCH a.job j " +
           "JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH j.createdBy u " +
           "WHERE (:jobId IS NULL OR j.id = :jobId) " +
           "  AND (:hrManagerId IS NULL OR j.createdBy.id = :hrManagerId) " +
           "  AND (:statusText IS NULL OR :statusText = '' OR CAST(a.status AS string) = :statusText) " +
           "ORDER BY a.submittedAt DESC")
    Page<Application> findByJobIdAndStatus(
            @Param("jobId") Long jobId,
            @Param("hrManagerId") Long hrManagerId,
            @Param("statusText") String statusText,
            Pageable pageable
    );

    /**
     * Đếm số hồ sơ theo bộ lọc (jobId, hrManagerId, status) mà không load entity.
     * Dùng để hiển thị số lượng trên từng tab trạng thái trong trang danh sách
     * mà không gây ra LazyInitializationException khi dùng JOIN FETCH + phân trang.
     */
    @Query(value = """
            SELECT COUNT(a.id)
            FROM applications a
            JOIN job_postings j ON j.id = a.job_id
            WHERE (:jobId IS NULL OR j.id = :jobId)
              AND (:hrManagerId IS NULL OR j.created_by_id = :hrManagerId)
              AND (:statusText IS NULL OR :statusText = '' OR CAST(a.status AS TEXT) = :statusText)
            """, nativeQuery = true)
    long countByFiltersAndStatus(
            @Param("jobId") Long jobId,
            @Param("hrManagerId") Long hrManagerId,
            @Param("statusText") String statusText
    );
}
