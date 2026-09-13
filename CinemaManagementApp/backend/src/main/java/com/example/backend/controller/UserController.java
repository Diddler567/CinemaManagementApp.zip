package com.example.backend.controller;

import com.example.backend.entities.PermanentRole;
import com.example.backend.entities.User;
import com.example.backend.security.AuthUtils;
import com.example.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.backend.dto.ChangePasswordRequest;
import com.example.backend.dto.SimpleMessageResponse;
import com.example.backend.dto.UpdateMyProfileRequest;
import com.example.backend.dto.UpdateUserStatusRequest;
import jakarta.validation.Valid;
import com.example.backend.dto.AdminUpdateUserRequest;



@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        User u = AuthUtils.requireAuthUser(request);
        return ResponseEntity.ok(toResponse(u));
    }

    //GET /USERS/{ID}
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, HttpServletRequest request) {
        User requester = AuthUtils.requireAuthUser(request);

        //ADMIN ΜΠΟΡΕΊ ΝΑ ΔΕΙ ΌΛΟΥΣ
        boolean isAdmin = requester.getPermanentRole() == PermanentRole.ADMIN;
        if (isAdmin) {
            User target = userService.getUserOrThrow(id);
            return ResponseEntity.ok(toResponse(target));
        }

        //USER ΜΠΟΡΕΊ ΝΑ ΔΕΙ ΜΌΝΟ ΤΟΝ ΕΑΥΤΌ ΤΟΥ
        if (!requester.getUserID().equals(id)) {
            User target = userService.getUserOrThrow(id);

            //ΠΡΟΣΠΆΘΗΣΕ ΝΑ ΔΕΙ ΆΛΛΟΝ => DEACTIVATE ΚΑΙ ΤΟΥΣ 2
            userService.deactivateBoth(requester, target);

            //403 FORBIDDEN (Ή 401) - ΔΙΆΛΕΞΑ 403
            return ResponseEntity.status(403).body("Access denied. Both users deactivated.");
        }

        return ResponseEntity.ok(toResponse(requester));
    }

    @GetMapping
    public ResponseEntity<?> listUsers(
        @RequestParam(value = "username", required = false) String username,
        HttpServletRequest request) {

        User admin = AuthUtils.requireAuthUser(request);
        if (admin.getPermanentRole() != PermanentRole.ADMIN) {
            return ResponseEntity.status(403).body("Admin only");
        }

        if (username != null && !username.isBlank()) {
            User u = userService.getByUsernameOrThrow(username);
            return ResponseEntity.ok(toResponse(u));
        }

        return ResponseEntity.ok(
            userService.getAllUsers().stream().map(this::toResponse).toList()
        );
    }


    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@Valid @RequestBody UpdateMyProfileRequest req, HttpServletRequest request) {
        User me = AuthUtils.requireAuthUser(request);
        User updated = userService.updateMyProfile(me, req);
        return ResponseEntity.ok(toResponse(updated));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> adminUpdateUser(@PathVariable Long id,
                                        @Valid @RequestBody AdminUpdateUserRequest req,
                                        HttpServletRequest request) {

        User admin = AuthUtils.requireAuthUser(request);

        if (admin.getPermanentRole() != PermanentRole.ADMIN) {
            return ResponseEntity.status(403).body("Admin only");
        }

        User updated = userService.adminUpdateUserInfo(admin, id, req);

        return ResponseEntity.ok(toResponse(updated));
    }


    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest req, HttpServletRequest request) {
        User me = AuthUtils.requireAuthUser(request);
        userService.changeMyPassword(me, req);
        return ResponseEntity.ok(new SimpleMessageResponse("Password changed. Sessions invalidated."));
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMe(HttpServletRequest request) {
        User me = AuthUtils.requireAuthUser(request);
        userService.deleteUser(me, me);

        return ResponseEntity.ok(new SimpleMessageResponse("Account deleted."));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> adminSetStatus(@PathVariable Long id,
                                    @RequestBody UpdateUserStatusRequest req,
                                    HttpServletRequest request) {
        User admin = AuthUtils.requireAuthUser(request);
        if (admin.getPermanentRole() != PermanentRole.ADMIN) {
            return ResponseEntity.status(403).body("Admin only");
        }

        User updated = userService.adminSetActive(admin, id, req.isActive());

        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> adminDelete(@PathVariable Long id, HttpServletRequest request) {
        User admin = AuthUtils.requireAuthUser(request);
        if (admin.getPermanentRole() != PermanentRole.ADMIN) {
            return ResponseEntity.status(403).body("Admin only");
        }

        User target = userService.getUserOrThrow(id);
        userService.deleteUser(admin, target);

        return ResponseEntity.ok(new SimpleMessageResponse("User deleted."));
    }

    @PostMapping("/{id}/force-logout")
    public ResponseEntity<?> adminForceLogout(@PathVariable Long id, HttpServletRequest request) {
        User admin = AuthUtils.requireAuthUser(request);
        if (admin.getPermanentRole() != PermanentRole.ADMIN) {
            return ResponseEntity.status(403).body("Admin only");
        }

        userService.adminForceLogout(admin, id);

        return ResponseEntity.ok(new SimpleMessageResponse("User logged out (tokens invalidated)."));
    }


    private UserResponse toResponse(User u) {
        return new UserResponse(
            u.getUserID(),      
            u.getUserID(),      
            u.getUsername(),
            u.getFullname(),
            u.getPermanentRole().name(),
            u.isActive(),       
            u.isActive()        
            );
    }

    

    public record UserResponse(
        Long id,
        Long userID,
        String username,
        String fullname,
        String permanentRole,
        boolean active,
        boolean isActive
    ) {}

}

