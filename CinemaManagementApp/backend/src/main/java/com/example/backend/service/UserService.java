package com.example.backend.service;

import com.example.backend.audit.AuditService;
import com.example.backend.dto.AdminUpdateUserRequest;
import com.example.backend.dto.ChangePasswordRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.dto.UpdateMyProfileRequest;
import com.example.backend.entities.PermanentRole;
import com.example.backend.entities.User;
import com.example.backend.repository.AuthenticationTokenRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationTokenRepository tokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                    AuthenticationTokenRepository tokenRepository,
                    BCryptPasswordEncoder passwordEncoder,
                    AuditService auditService) {

        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    //REGISTER: ΔΗΜΙΟΥΡΓΕΊ INACTIVE USER
    @Transactional
    public User register(RegisterRequest req) {
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User u = new User();
        u.setUsername(req.getUsername());
        u.setFullname(req.getFullname());
        u.setPermanentRole(PermanentRole.USER);
        u.setActive(false); // INACTIVE BY DEFAULT (PDF)
        u.setFailedAuthCount(0);
        u.setFailedPasswordChangeCount(0); // ✅ SEPARATE COUNTER FOR PASSWORD-CHANGE FAILURES
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));

        User saved = userRepository.save(u);

        auditService.logAnonymous("USER_REGISTERED", "USER", saved.getUserID(),
                "username=" + saved.getUsername() + " active=false");

        return saved;
    }

    //UPDATE MY PROFILE: ΑΝ ΑΛΛΆΞΕΙ USERNAME => INVALIDATE TOKENS
    @Transactional
    public User updateMyProfile(User me, UpdateMyProfileRequest req) {
        String oldUsername = me.getUsername();
        String newUsername = req.getUsername();

        if (!oldUsername.equals(newUsername) && userRepository.existsByUsername(newUsername)) {
            throw new RuntimeException("Username already exists");
        }

        me.setUsername(newUsername);
        me.setFullname(req.getFullname());

        User saved = userRepository.save(me);

        boolean usernameChanged = !oldUsername.equals(newUsername);
        if (usernameChanged) {
            tokenRepository.invalidateAllForUser(saved);
        }

        auditService.log(me, "USER_PROFILE_UPDATED", "USER", me.getUserID(),
                "usernameChanged=" + usernameChanged);

        return saved;
    }

    //ADMIN UPDATE USER INFO (NEEDS ACTOR FOR AUDIT)
    @Transactional
    public User adminUpdateUserInfo(User admin, Long targetUserId, AdminUpdateUserRequest req) {
        User target = getUserOrThrow(targetUserId);

        String oldUsername = target.getUsername();
        String newUsername = req.getUsername();

        if (!oldUsername.equals(newUsername) && userRepository.existsByUsername(newUsername)) {
            throw new RuntimeException("Username already exists");
        }

        target.setUsername(newUsername);
        target.setFullname(req.getFullname());

        User saved = userRepository.save(target);

        boolean usernameChanged = !oldUsername.equals(newUsername);
        if (usernameChanged) {
            tokenRepository.invalidateAllForUser(saved);
        }

        auditService.log(admin, "USER_UPDATED_BY_ADMIN", "USER", targetUserId,
                "usernameChanged=" + usernameChanged);

        return saved;
    }

    //CHANGE PASSWORD: 3 ΛΆΘΟΣ OLD => DEACTIVATE (SEPARATE COUNTER) + AUDIT
    @Transactional(noRollbackFor = RuntimeException.class)
    public void changeMyPassword(User me, ChangePasswordRequest req) {

        
        if (!req.getNewPassword().equals(req.getConfirmNewPassword())) {
            auditService.log(me, "PASSWORD_CHANGE_FAILED", "USER", me.getUserID(),
                    "reason=new_confirm_mismatch");

            tokenRepository.invalidateAllForUser(me);
            throw new RuntimeException("Passwords do not match");
        }

        boolean ok = passwordEncoder.matches(req.getOldPassword(), me.getPasswordHash());
        if (!ok) {
            me.setFailedPasswordChangeCount(me.getFailedPasswordChangeCount() + 1);

            auditService.log(me, "PASSWORD_CHANGE_FAILED", "USER", me.getUserID(),
                    "reason=wrong_old count=" + me.getFailedPasswordChangeCount());

            if (me.getFailedPasswordChangeCount() >= 3) {
                me.setActive(false);
                userRepository.save(me);

                auditService.log(me, "USER_DEACTIVATED_PASSWORD_CHANGE_FAIL", "USER", me.getUserID(),
                        "count=" + me.getFailedPasswordChangeCount());

                tokenRepository.invalidateAllForUser(me);
                throw new RuntimeException("Wrong old password. User deactivated.");
            }

            userRepository.save(me);
            tokenRepository.invalidateAllForUser(me);
            throw new RuntimeException("Wrong old password");
        }

        
        me.setFailedPasswordChangeCount(0);
        me.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(me);

        tokenRepository.invalidateAllForUser(me);

        auditService.log(me, "PASSWORD_CHANGED", "USER", me.getUserID(),
                "sessionsInvalidated=true");
    }

    //ADMIN ACTIVATE/DEACTIVATE ΆΛΛΟΥΣ (NEEDS ACTOR FOR AUDIT)
    @Transactional
    public User adminSetActive(User admin, Long targetUserId, boolean active) {
        User target = getUserOrThrow(targetUserId);

        target.setActive(active);

        if (!active) {
            tokenRepository.invalidateAllForUser(target);
        }

        User saved = userRepository.save(target);

        auditService.log(admin,
                active ? "USER_ACTIVATED_BY_ADMIN" : "USER_DEACTIVATED_BY_ADMIN",
                "USER",
                targetUserId,
                "tokensInvalidated=" + (!active));

        return saved;
    }

    //DEACTIVATE HELPER
    @Transactional
    public void deactivate(User user) {
        user.setActive(false);
        userRepository.save(user);
        tokenRepository.invalidateAllForUser(user);
    }

    @Transactional
    public void deactivateBoth(User requester, User target) {
        deactivate(requester);
        deactivate(target);
    }

    //DELETE USER (ADMIN ACCOUNTS NON-DELETABLE) + AUDIT
    @Transactional
    public void deleteUser(User actor, User target) {
        if (target.getPermanentRole() == PermanentRole.ADMIN) {
            throw new RuntimeException("ADMIN accounts cannot be deleted");
        }

        tokenRepository.invalidateAllForUser(target);
        tokenRepository.deleteAllByUser(target);
        userRepository.delete(target);

        String action = actor.getUserID().equals(target.getUserID())
                ? "USER_SELF_DELETED"
                : "USER_DELETED_BY_ADMIN";

        auditService.log(actor, action, "USER", target.getUserID(),
                "username=" + target.getUsername());
    }

    //ADMIN FORCED LOGOUT NON-ADMIN + AUDIT
    @Transactional
    public void adminForceLogout(User admin, Long targetUserId) {
        User target = getUserOrThrow(targetUserId);

        if (target.getPermanentRole() == PermanentRole.ADMIN) {
            throw new RuntimeException("Cannot force logout an ADMIN");
        }

        int changed = tokenRepository.invalidateAllForUser(target);

        auditService.log(admin, "USER_FORCE_LOGOUT_BY_ADMIN", "USER", targetUserId,
                "tokensInvalidated=" + changed);
    }

    @Transactional(readOnly = true)
    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

}
