package com.app.pastebinclone.DTOs;

import jakarta.validation.constraints.NotBlank;

public class DeletePasteDTO {

    @NotBlank
    private String url;

    @NotBlank
    private String password;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
