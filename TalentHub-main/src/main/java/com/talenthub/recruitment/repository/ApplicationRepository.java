package com.talenthub.recruitment.repository;

import com.talenthub.recruitment.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
