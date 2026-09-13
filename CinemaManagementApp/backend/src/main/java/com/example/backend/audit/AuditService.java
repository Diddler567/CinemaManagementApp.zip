package com.example.backend.audit;

import com.example.backend.entities.AuditEvent;
import com.example.backend.entities.User;
import com.example.backend.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository repo;

    public AuditService(AuditEventRepository repo) {
        this.repo = repo;
    }

    public void log(User actor, String action, String targetType, Long targetId, String details) {
        Long actorId = (actor == null) ? null : actor.getUserID();
        save(actorId, action, targetType, targetId, details);
    }

    public void logAnonymous(String action, String targetType, Long targetId, String details) {
        save(null, action, targetType, targetId, details);
    }

    private void save(Long actorUserId, String action, String targetType, Long targetId, String details) {
        String ip = AuditContext.ip();
        String method = AuditContext.method();
        String path = AuditContext.path();

        //APPLICATION LOGS
        log.info("AUDIT action={} actor={} targetType={} targetId={} ip={} method={} path={} details={}",
                action, actorUserId, targetType, targetId, ip, method, path, details);

        //PERSISTENT AUDIT TABLE
        AuditEvent ev = new AuditEvent(
                Instant.now(),
                actorUserId,
                action,
                targetType,
                targetId,
                details,
                ip,
                method,
                path
        );
        repo.save(ev);
    }
}

