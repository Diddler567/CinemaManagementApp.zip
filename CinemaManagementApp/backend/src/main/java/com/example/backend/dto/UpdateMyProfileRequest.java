package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateMyProfileRequest {

    @NotBlank
    @Pattern(
        regexp = "^[A-Za-z][A-Za-z0-9_]{4,}$",
        message = "Invalid username format"
    )
    private String username;


    @NotBlank
    private String fullname;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }
}

