package com.talenthub.recruitment.dto;

import java.time.Instant;

public class ApplicationNoteDTO {
    private Long id;
    private String authorName;
    private Instant createdAt;
    private String content;

    public ApplicationNoteDTO() {
    }

    public ApplicationNoteDTO(Long id, String authorName, Instant createdAt, String content) {
        this.id = id;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
