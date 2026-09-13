package com.example.backend.dto;

public class UpdateUserStatusRequest {
    private boolean isActive;

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}

