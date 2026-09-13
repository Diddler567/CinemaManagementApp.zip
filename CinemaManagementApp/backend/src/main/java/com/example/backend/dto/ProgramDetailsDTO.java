package com.example.backend.dto;

import com.example.backend.entities.Program;
import com.example.backend.entities.ProgramState;

import java.time.LocalDate;

import java.util.List;




public class ProgramDetailsDTO {
    private Long programID;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate creationDate;
    private ProgramState state;

    //CREATOR (SAFE MINIMAL)
    private Long creatorUserID;
    private String creatorUsername;

    //ROLE OF CURRENT USER IN THIS PROGRAM (NULL IF NONE)
    private String myRoleInProgram;

    public ProgramDetailsDTO() {}

    public static ProgramDetailsDTO from(Program p, String myRoleInProgram) {
        ProgramDetailsDTO dto = new ProgramDetailsDTO();
        dto.programID = p.getProgramID();
        dto.name = p.getName();
        dto.description = p.getDescription();
        dto.startDate = p.getStartDate();
        dto.endDate = p.getEndDate();
        dto.creationDate = p.getCreationDate();
        dto.state = p.getState();

        if (p.getCreator() != null) {
            dto.creatorUserID = p.getCreator().getUserID();
            dto.creatorUsername = p.getCreator().getUsername();
        }

        dto.myRoleInProgram = myRoleInProgram;
        return dto;
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

    public ProgramState getState() { return state; }
    public void setState(ProgramState state) { this.state = state; }

    public Long getCreatorUserID() { return creatorUserID; }
    public void setCreatorUserID(Long creatorUserID) { this.creatorUserID = creatorUserID; }

    public String getCreatorUsername() { return creatorUsername; }
    public void setCreatorUsername(String creatorUsername) { this.creatorUsername = creatorUsername; }

    public String getMyRoleInProgram() { return myRoleInProgram; }
    public void setMyRoleInProgram(String myRoleInProgram) { this.myRoleInProgram = myRoleInProgram; }

    private List<String> programmerUsernames;

    private List<String> staffUsernames;

    public List<String> getStaffUsernames() { return staffUsernames; }
    public void setStaffUsernames(List<String> staffUsernames) { this.staffUsernames = staffUsernames; }


    public List<String> getProgrammerUsernames() { return programmerUsernames; }
    public void setProgrammerUsernames(List<String> programmerUsernames) { this.programmerUsernames = programmerUsernames; }
}
