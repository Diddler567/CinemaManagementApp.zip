package com.example.backend.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class UpdateProgramRequest {

    @Size(max = 120)
    private String name;

    @Size(max = 2000)
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;

    private List<Long> programmerUserIDs;
    private List<Long> staffUserIDs;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public List<Long> getProgrammerUserIDs() { return programmerUserIDs; }
    public void setProgrammerUserIDs(List<Long> programmerUserIDs) { this.programmerUserIDs = programmerUserIDs; }

    public List<Long> getStaffUserIDs() { return staffUserIDs; }
    public void setStaffUserIDs(List<Long> staffUserIDs) { this.staffUserIDs = staffUserIDs; }
}
