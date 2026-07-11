package com.talenthub.recruitment.repository;

import com.talenthub.recruitment.entity.ApplicationNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationNoteRepository extends JpaRepository<ApplicationNote, Long> {
    List<ApplicationNote> findByApplication_IdOrderByCreatedAtDesc(Long applicationId);
}
