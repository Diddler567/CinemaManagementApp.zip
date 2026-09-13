package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AssignRoleRequest {

    @NotNull
    private Long userID;

    @NotBlank
    private String role;

    public Long getUserID() { return userID; }
    public void setUserID(Long userID) { this.userID = userID; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}


