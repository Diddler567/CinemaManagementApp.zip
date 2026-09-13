package com.example.backend.dto;

import com.example.backend.entities.Program;
import com.example.backend.entities.ProgramState;

import java.time.LocalDate;

public class ProgramSearchResponseDTO {
    private Long programID;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private ProgramState state;

    public ProgramSearchResponseDTO() {}

    public static ProgramSearchResponseDTO from(Program p) {
        ProgramSearchResponseDTO dto = new ProgramSearchResponseDTO();
        dto.programID = p.getProgramID();
        dto.name = p.getName();
        dto.startDate = p.getStartDate();
        dto.endDate = p.getEndDate();
        dto.state = p.getState();
        return dto;
    }

    public Long getProgramID() { return programID; }
    public void setProgramID(Long programID) { this.programID = programID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public ProgramState getState() { return state; }
    public void setState(ProgramState state) { this.state = state; }
}

