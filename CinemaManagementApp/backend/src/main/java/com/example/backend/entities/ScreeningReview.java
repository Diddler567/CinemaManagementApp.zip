package com.example.backend.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "screening_reviews")
public class ScreeningReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewID;


    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "screening_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_review_screening"))
    private Screening screening;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_review_staff"))
    private User staff;

    @Column(nullable = false)
    private Integer score; // Π.Χ. 1..10

    @Column(length = 1000)
    private String comments;

    @Column(nullable = false)
    private Instant reviewAt = Instant.now();

    public ScreeningReview() {}

    public ScreeningReview(Screening screening, User staff, Integer score, String comments) {
        this.screening = screening;
        this.staff = staff;
        this.score = score;
        this.comments = comments;
    }

    public Long getReviewID() { return reviewID; }
    public Screening getScreening() { return screening; }
    public User getStaff() { return staff; }
    public Integer getScore() { return score; }
    public String getComments() { return comments; }
    public Instant getReviewAt() { return reviewAt; }
}

