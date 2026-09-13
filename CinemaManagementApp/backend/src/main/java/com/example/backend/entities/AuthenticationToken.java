package com.example.backend.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "authentication_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_auth_token_value", columnNames = "token_value")

        }
)
public class AuthenticationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenID;


    @Column(nullable = false, length = 120, unique = true)
    private String tokenValue;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_auth_token_user"))
    private User user;

    @Column(nullable = false)
    private Instant createdAt;

    
    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean isValid;

    public AuthenticationToken() {}

    public AuthenticationToken(String tokenValue, User user, Instant expiresAt) {
        this.tokenValue = tokenValue;
        this.user = user;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.isValid = true;
    }

    public Long getTokenID() { return tokenID; }

    public String getTokenValue() { return tokenValue; }
    public void setTokenValue(String tokenValue) { this.tokenValue = tokenValue; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }
}