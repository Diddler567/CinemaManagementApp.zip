package com.example.backend.dto;

import com.example.backend.entities.Program;

import java.time.LocalDate;

public class ProgramPublicDTO {
    private Long programID;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;

    public ProgramPublicDTO() {}

    public ProgramPublicDTO(Long programID, String name, LocalDate startDate, LocalDate endDate) {
        this.programID = programID;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static ProgramPublicDTO from(Program p) {
        return new ProgramPublicDTO(
                p.getProgramID(),
                p.getName(),
                p.getStartDate(),
                p.getEndDate()
        );
    }

    public Long getProgramID() { return programID; }
    public void setProgramID(Long programID) { this.programID = programID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}

