package com.app.pastebinclone.DTOs;

import com.app.pastebinclone.models.Exposure;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UpdatePasteDTO {

    @NotBlank
    private String password;

    @Size(max = 255)
    private String title;

    @Size(max = 1_000_000)
    private String content;

    private Exposure exposure;

    @Future
    private LocalDateTime expirationDate;

    @Size(max = 64)
    private String language;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
