package com.example.backend.dto;

public class ScreeningReviewDTO {

    private Long reviewID;
    private Integer score;
    private String comments;
    private Long staffUserID;

    public ScreeningReviewDTO() {}

    public ScreeningReviewDTO(Long reviewID, Integer score, String comments, Long staffUserID) {
        this.reviewID = reviewID;
        this.score = score;
        this.comments = comments;
        this.staffUserID = staffUserID;
    }

    public Long getReviewID() { return reviewID; }
    public void setReviewID(Long reviewID) { this.reviewID = reviewID; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Long getStaffUserID() { return staffUserID; }
    public void setStaffUserID(Long staffUserID) { this.staffUserID = staffUserID; }
}
