package com.talenthub.recruitment.repository;

import com.talenthub.recruitment.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {
    Optional<Interview> findFirstByApplication_IdOrderByScheduledAtAsc(Long applicationId);

    @Query("SELECT i FROM Interview i " +
           "JOIN FETCH i.application a " +
           "JOIN FETCH a.candidate c " +
           "JOIN FETCH a.job j " +
           "JOIN FETCH i.interviewer u " +
           "WHERE i.id = :id")
    Optional<Interview> findByIdWithRelations(@Param("id") Long id);
    List<Interview> findByApplication_IdOrderByScheduledAtDesc(Long applicationId);

    @Query(value = """
        SELECT i.* FROM interviews i
        WHERE i.interviewer_id = :interviewerId
        ORDER BY i.scheduled_at DESC
        """, nativeQuery = true)
    List<Interview> findByInterviewerId(@Param("interviewerId") Long interviewerId);

    @Query(value = """
        SELECT COUNT(i.id) 
        FROM interviews i
        JOIN applications a ON a.id = i.application_id
        JOIN job_postings j ON j.id = a.job_id
        WHERE CAST(i.status AS TEXT) = 'SCHEDULED'
          AND i.scheduled_at >= NOW()
          AND i.scheduled_at <= NOW() + interval '7 days'
          AND j.created_by_id = :hrManagerId
        """, nativeQuery = true)
    long countUpcomingInterviewsForHr(@Param("hrManagerId") Long hrManagerId);

    @Query(value = """
        SELECT COUNT(i.id)
        FROM interviews i
        WHERE CAST(i.status AS TEXT) = 'SCHEDULED'
          AND i.scheduled_at >= NOW()
          AND i.scheduled_at <= NOW() + interval '7 days'
        """, nativeQuery = true)
    long countUpcomingInterviewsForAdmin();
}
