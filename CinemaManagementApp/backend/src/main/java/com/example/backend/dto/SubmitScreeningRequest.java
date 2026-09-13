package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class SubmitScreeningRequest {

    private Long programID;
    private String filmTitle;
    private String filmCast;
    private String filmGenre;
    private Integer filmDuration;

    //CANDIDATE SCHEDULING FIELDS (CREATED STAGE)
    private String auditoriumName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;


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

    public String getAuditoriumName() { return auditoriumName; }
    public void setAuditoriumName(String auditoriumName) { this.auditoriumName = auditoriumName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

}
