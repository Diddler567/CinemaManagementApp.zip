package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class ChangeProgramStateRequest {

    @NotBlank
    private String nextState;

    public String getNextState() { return nextState; }
    public void setNextState(String nextState) { this.nextState = nextState; }
}
