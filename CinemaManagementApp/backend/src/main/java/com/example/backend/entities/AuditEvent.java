package com.example.backend.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant timestamp;

    //ΜΠΟΡΕΊ ΝΑ ΕΊΝΑΙ NULL ΓΙΑ UNAUTH EVENTS
    private Long actorUserId;

    @Column(length = 120)
    private String action;        

    @Column(length = 60)
    private String targetType;    

    private Long targetId;

    @Column(length = 2000)
    private String details;

    @Column(length = 64)
    private String ip;

    @Column(length = 10)
    private String method;

    @Column(length = 300)
    private String path;

    public AuditEvent() {}

    public AuditEvent(Instant timestamp, Long actorUserId, String action, String targetType, Long targetId,
                    String details, String ip, String method, String path) {
        this.timestamp = timestamp;
        this.actorUserId = actorUserId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.ip = ip;
        this.method = method;
        this.path = path;
    }

    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}

