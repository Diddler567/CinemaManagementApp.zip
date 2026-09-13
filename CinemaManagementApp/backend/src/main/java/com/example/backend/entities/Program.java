package com.example.backend.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
        name = "programs",
        uniqueConstraints = @UniqueConstraint(name = "uk_programs_name", columnNames = "name")
)
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "program_id")
    private Long programID;


    @Column(nullable = false, length = 120)
    private String name;

    //REQUIRED DESCRIPTION
    @Column(nullable = false, length = 500)
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalDate creationDate = LocalDate.now();

    @Convert(converter = ProgramStateConverter.class)
    @Column(nullable = false, length = 30)
    private ProgramState state = ProgramState.CREATED;


    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(
            name = "creator_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_program_creator")
    )
    private User creator;

    public Program() {}

    //UPDATED CONSTRUCTOR INCLUDES DESCRIPTION
    public Program(String name, String description, LocalDate startDate, LocalDate endDate, User creator) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.creator = creator;
    }

    public Long getProgramID() { return programID; }

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

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }
}
