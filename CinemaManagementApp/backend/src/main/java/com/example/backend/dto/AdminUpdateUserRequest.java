package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class AdminUpdateUserRequest {

    @NotBlank
    @Size(max = 50)
    @Pattern(
            regexp = "^[A-Za-z][A-Za-z0-9_]{4,}$",
            message = "Invalid username format"
    )
    private String username;

    @NotBlank
    @Size(max = 100)
    private String fullname;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }
}

