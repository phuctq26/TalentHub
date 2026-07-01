package com.talenthub.recruitment.repository;

import com.talenthub.recruitment.entity.JobPosting;
import com.talenthub.recruitment.entity.User;
import com.talenthub.recruitment.entity.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    @Query(value = """
            SELECT *
            FROM job_postings
            WHERE status = CAST('ACTIVE' AS job_status)
              AND (
                  :keyword IS NULL
                  OR :keyword = ''
                  OR LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(department) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
              AND (
                  :department IS NULL
                  OR :department = ''
                  OR LOWER(department) = LOWER(:department)
              )
              AND (
                  :location IS NULL
                  OR :location = ''
                  OR LOWER(location) = LOWER(:location)
              )
            ORDER BY published_at DESC NULLS LAST, created_at DESC
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM job_postings
                    WHERE status = CAST('ACTIVE' AS job_status)
                      AND (
                          :keyword IS NULL
                          OR :keyword = ''
                          OR LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(department) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                      AND (
                          :department IS NULL
                          OR :department = ''
                          OR LOWER(department) = LOWER(:department)
                      )
                      AND (
                          :location IS NULL
                          OR :location = ''
                          OR LOWER(location) = LOWER(:location)
                      )
                    """,
            nativeQuery = true)
    Page<JobPosting> searchPublicJobs(
            @Param("keyword") String keyword,
            @Param("department") String department,
            @Param("location") String location,
            Pageable pageable
    );

    @Query(value = """
            SELECT *
            FROM job_postings
            WHERE id = :id
              AND status = CAST('ACTIVE' AS job_status)
            """, nativeQuery = true)
    Optional<JobPosting> findPublicJobById(@Param("id") Long id);

    // Admin xem tất cả job
    @Query(value = """
    SELECT *
    FROM job_postings j
    WHERE (:createdById IS NULL OR j.created_by_id = :createdById)
      AND (:status IS NULL OR j.status = CAST(:status AS job_status))
      AND (:department IS NULL OR :department = '' OR j.department = :department)
      AND (:keyword IS NULL OR :keyword = ''
           OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    ORDER BY j.created_at DESC
""", nativeQuery = true)
    List<JobPosting> search(
            @Param("createdById") Long createdById,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("department") String department
    );


    // Danh sách phòng ban
    @Query("""
            SELECT DISTINCT j.department
            FROM JobPosting j
            ORDER BY j.department
            """)
    List<String> findAllDepartments();

    @Query("""
                SELECT DISTINCT j.department
                FROM JobPosting j
                WHERE j.createdBy = :createdBy
                ORDER BY j.department
            """)
    List<String> findDepartmentsByCreatedBy(
            @Param("createdBy") User createdBy
    );

    long countByStatus(JobStatus status);

    long countByCreatedByAndStatus(User createdBy, JobStatus status);
}
