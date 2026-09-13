package com.example.backend.entities;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ProgramRoleId implements Serializable {
    private Long programID;
    private Long userID;

    public ProgramRoleId() {}

    public ProgramRoleId(Long programID, Long userID) {
        this.programID = programID;
        this.userID = userID;
    }

    public Long getProgramID() { return programID; }
    public Long getUserID() { return userID; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProgramRoleId that)) return false;
        return Objects.equals(programID, that.programID) && Objects.equals(userID, that.userID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(programID, userID);
    }
}

