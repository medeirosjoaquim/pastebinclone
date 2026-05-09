package com.app.pastebinclone.DTOs;

import com.app.pastebinclone.models.Exposure;

import java.time.LocalDateTime;

public class PasteDTO {
    private Long id;
    private String title;
    private String content;
    private Exposure exposure;
    private String language;
    private long views;
    private boolean burnAfterRead;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expirationDate;
    private String url;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Exposure getExposure() {
        return exposure;
    }

    public void setExposure(Exposure exposure) {
        this.exposure = exposure;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public long getViews() {
        return views;
    }

    public void setViews(long views) {
        this.views = views;
    }

    public boolean isBurnAfterRead() {
        return burnAfterRead;
    }

    public void setBurnAfterRead(boolean burnAfterRead) {
        this.burnAfterRead = burnAfterRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
