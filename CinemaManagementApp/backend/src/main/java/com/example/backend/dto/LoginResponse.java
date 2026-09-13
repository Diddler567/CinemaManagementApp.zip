package com.example.backend.dto;

public class LoginResponse {
    private String token;
    private Long userID;
    private String permanentRole;

    public LoginResponse(String token, Long userID, String permanentRole) {
        this.token = token;
        this.userID = userID;
        this.permanentRole = permanentRole;
    }

    public String getToken() {
        return token;
    }

    public Long getUserID() {
        return userID;
    }

    public String getPermanentRole() {
        return permanentRole;
    }
}

