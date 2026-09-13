package com.example.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ScreeningViewDTO {

    private Long screeningID;
    private Long programID;

    private String filmTitle;
    private String filmCast;
    private String filmGenre;
    private Integer filmDuration;

    private String state;

    private String auditoriumName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Long submitterUserID;
    private Long handlerStaffUserID;

    private String approvalNotes;
    private String rejectionReason;
    private String finalSubmissionAt;

    private List<ScreeningReviewDTO> reviews;

    public Long getScreeningID() { return screeningID; }
    public void setScreeningID(Long screeningID) { this.screeningID = screeningID; }

    public Long getProgramID() { return programID; }
    public void setProgramID(Long programID) { this.programID = programID; }

    public String getFilmTitle() { return filmTitle; }
    public void setFilmTitle(String filmTitle) { this.filmTitle = filmTitle; }

    public String getFilmCast() { return filmCast; }
    public void setFilmCast(String filmCast) { this.filmCast = filmCast; }

    public String getFilmGenre() { return filmGenre; }
    public void setFilmGenre(String filmGenre) { this.filmGenre = filmGenre; }

    public Integer getFilmDuration() { return filmDuration; }
    public void setFilmDuration(Integer filmDuration) { this.filmDuration = filmDuration; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getAuditoriumName() { return auditoriumName; }
    public void setAuditoriumName(String auditoriumName) { this.auditoriumName = auditoriumName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Long getSubmitterUserID() { return submitterUserID; }
    public void setSubmitterUserID(Long submitterUserID) { this.submitterUserID = submitterUserID; }

    public Long getHandlerStaffUserID() { return handlerStaffUserID; }
    public void setHandlerStaffUserID(Long handlerStaffUserID) { this.handlerStaffUserID = handlerStaffUserID; }

    public String getApprovalNotes() { return approvalNotes; }
    public void setApprovalNotes(String approvalNotes) { this.approvalNotes = approvalNotes; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getFinalSubmissionAt() { return finalSubmissionAt; }
    public void setFinalSubmissionAt(String finalSubmissionAt) { this.finalSubmissionAt = finalSubmissionAt; }

    public List<ScreeningReviewDTO> getReviews() { return reviews; }
    public void setReviews(List<ScreeningReviewDTO> reviews) { this.reviews = reviews; }
}
