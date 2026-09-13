package com.example.backend.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "program_roles")
public class ProgramRole {

    @EmbeddedId
    private ProgramRoleId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("programID")
    @JoinColumn(name = "program_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_programrole_program"))
    private Program program;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("userID")
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_programrole_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgramRoleType role;

    @Column(nullable = false)
    private Instant assignedAt = Instant.now();

    public ProgramRole() {}

    public ProgramRole(Program program, User user, ProgramRoleType role) {
        this.program = program;
        this.user = user;
        this.role = role;
        this.id = new ProgramRoleId(program.getProgramID(), user.getUserID());
    }

    public ProgramRoleId getId() { return id; }
    public Program getProgram() { return program; }
    public User getUser() { return user; }
    public ProgramRoleType getRole() { return role; }
    public void setRole(ProgramRoleType role) { this.role = role; }
    public Instant getAssignedAt() { return assignedAt; }
}

