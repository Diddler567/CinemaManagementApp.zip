package com.example.backend.dto;

public class CreateScreeningReviewRequest {
    private Integer score;
    private String comments;

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}

