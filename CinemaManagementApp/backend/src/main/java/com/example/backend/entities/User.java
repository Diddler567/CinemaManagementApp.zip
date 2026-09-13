package com.example.backend.entities;

import jakarta.persistence.*;

@Entity
@Table(
        name = "users",
        uniqueConstraints = { @UniqueConstraint(name = "uk_users_username", columnNames = "username")}
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userID;


    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 120)
    private String fullname;

    @Column(nullable = false, length = 255)
    private String passwordHash;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PermanentRole permanentRole = PermanentRole.USER;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private int failedAuthCount = 0;

    @Column(name = "failed_password_change_count", nullable = false)
    private int failedPasswordChangeCount = 0;


    public User() { }

    public User(String username, String fullname, PermanentRole permanentRole) {
        this.username = username;
        this.fullname = fullname;
        this.permanentRole = permanentRole;
    }

    public Long getUserID() {
        return userID;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public PermanentRole getPermanentRole() {
        return permanentRole;
    }

    public void setPermanentRole(PermanentRole permanentRole) {
        this.permanentRole = permanentRole;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getFailedAuthCount() {
        return failedAuthCount;
    }

    public void setFailedAuthCount(int failedAuthCount) {
        this.failedAuthCount = failedAuthCount;
    }

    public int getFailedPasswordChangeCount() {
        return failedPasswordChangeCount;
    }

    public void setFailedPasswordChangeCount(int failedPasswordChangeCount) {
        this.failedPasswordChangeCount = failedPasswordChangeCount;
    }


    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

}