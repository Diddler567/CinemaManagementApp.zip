package com.example.backend.dto;

import com.example.backend.entities.Program;

import java.time.LocalDate;

public class ProgramPublicDetailsDTO {
    private Long programID;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate creationDate;

    public ProgramPublicDetailsDTO() {}

    public ProgramPublicDetailsDTO(Long programID, String name, String description,
                                LocalDate startDate, LocalDate endDate, LocalDate creationDate) {
        this.programID = programID;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.creationDate = creationDate;
    }

    public static ProgramPublicDetailsDTO from(Program p) {
        return new ProgramPublicDetailsDTO(
                p.getProgramID(),
                p.getName(),
                p.getDescription(),
                p.getStartDate(),
                p.getEndDate(),
                p.getCreationDate()
        );
    }

    public Long getProgramID() { return programID; }
    public void setProgramID(Long programID) { this.programID = programID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDate getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDate creationDate) { this.creationDate = creationDate; }
}