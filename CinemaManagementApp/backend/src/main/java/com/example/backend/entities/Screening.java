package com.example.backend.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "screenings")
public class Screening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "screening_id")
    private Long screeningID;


    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(
            name = "program_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_screening_program")
    )
    private Program program;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(
            name = "submitter_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_screening_submitter")
    )
    private User submitter;

    //STAFF ΠΟΥ ΧΕΙΡΊΖΕΤΑΙ/ΑΞΙΟΛΟΓΕΊ ΤΟ SCREENING
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "handler_staff_user_id",
            foreignKey = @ForeignKey(name = "fk_screening_handler_staff")
    )
    private User handlerStaff;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScreeningState state = ScreeningState.CREATED;

    @Column(nullable = false)
    private Instant creationDate = Instant.now();

    //ΠΌΤΕ ΈΓΙΝΕ ΤΟ FINAL SUBMIT (CREATED -> SUBMITTED)
    private Instant finalSubmissionAt;

    //NOTES ΓΙΑ APPROVE/REJECT
    @Column(length = 500)
    private String approvalNotes;

    @Column(length = 500)
    private String rejectionReason;

    //FILM INFO
    @Column(nullable = false, length = 200)
    private String filmTitle;

    @Column(length = 500)
    private String filmCast;

    @Column(length = 100)
    private String filmGenre;

    @Column(nullable = false)
    private Integer filmDuration; // MINUTES

    //SCHEDULING (LATER)
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Column(length = 100)
    private String auditoriumName;

    public Screening() {}

    public Screening(Program program, User submitter,
                String filmTitle, String filmCast,
                String filmGenre, Integer filmDuration) {
        this.program = program;
        this.submitter = submitter;
        this.filmTitle = filmTitle;
        this.filmCast = filmCast;
        this.filmGenre = filmGenre;
        this.filmDuration = filmDuration;
    }

    public Long getScreeningID() { return screeningID; }

    public Program getProgram() { return program; }
    public void setProgram(Program program) { this.program = program; }

    public User getSubmitter() { return submitter; }
    public void setSubmitter(User submitter) { this.submitter = submitter; }

    public User getHandlerStaff() { return handlerStaff; }
    public void setHandlerStaff(User handlerStaff) { this.handlerStaff = handlerStaff; }

    public ScreeningState getState() { return state; }
    public void setState(ScreeningState state) { this.state = state; }

    public Instant getCreationDate() { return creationDate; }
    public void setCreationDate(Instant creationDate) { this.creationDate = creationDate; }

    public Instant getFinalSubmissionAt() { return finalSubmissionAt; }
    public void setFinalSubmissionAt(Instant finalSubmissionAt) { this.finalSubmissionAt = finalSubmissionAt; }

    public String getApprovalNotes() { return approvalNotes; }
    public void setApprovalNotes(String approvalNotes) { this.approvalNotes = approvalNotes; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getFilmTitle() { return filmTitle; }
    public void setFilmTitle(String filmTitle) { this.filmTitle = filmTitle; }

    public String getFilmCast() { return filmCast; }
    public void setFilmCast(String filmCast) { this.filmCast = filmCast; }

    public String getFilmGenre() { return filmGenre; }
    public void setFilmGenre(String filmGenre) { this.filmGenre = filmGenre; }

    public Integer getFilmDuration() { return filmDuration; }
    public void setFilmDuration(Integer filmDuration) { this.filmDuration = filmDuration; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getAuditoriumName() { return auditoriumName; }
    public void setAuditoriumName(String auditoriumName) { this.auditoriumName = auditoriumName; }
}
