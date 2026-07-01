package com.talenthub.recruitment.repository;

import com.talenthub.recruitment.entity.JobPosting;
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
      """, countQuery = """
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
      """, nativeQuery = true)
  Page<JobPosting> searchPublicJobs(
      @Param("keyword") String keyword,
      @Param("department") String department,
      @Param("location") String location,
      Pageable pageable);

  @Query(value = """
      SELECT *
      FROM job_postings
      WHERE id = :id
        AND status = CAST('ACTIVE' AS job_status)
      """, nativeQuery = true)
  Optional<JobPosting> findPublicJobById(@Param("id") Long id);

  @Query(value = """
      SELECT * FROM job_postings
      WHERE (:createdById IS NULL OR created_by_id = :createdById)
        AND (:statusText IS NULL OR :statusText = '' OR status = CAST(:statusText AS job_status))
        AND (:department IS NULL OR :department = '' OR LOWER(department) = LOWER(:department))
        AND (:keyword IS NULL OR :keyword = '' OR
             LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(description) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
      ORDER BY created_at DESC
      """, nativeQuery = true)
  List<JobPosting> search(
      @Param("createdById") Long createdById,
      @Param("keyword") String keyword,
      @Param("statusText") String statusText,
      @Param("department") String department);
}
