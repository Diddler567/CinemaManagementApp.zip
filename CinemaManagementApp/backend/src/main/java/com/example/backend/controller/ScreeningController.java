package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.entities.Screening;
import com.example.backend.entities.ScreeningReview;
import com.example.backend.entities.User;
import com.example.backend.security.AuthUtils;
import com.example.backend.service.ScreeningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/screenings")
public class ScreeningController {

    private final ScreeningService screeningService;

    public ScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    //CREATE DRAFT (CREATED)
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody SubmitScreeningRequest req, HttpServletRequest request) {
        User submitter = AuthUtils.requireAuthUser(request);

        Screening s = screeningService.createDraft(
        submitter,
            req.getProgramID(),
            req.getFilmTitle(),
            req.getFilmCast(),
            req.getFilmGenre(),
            req.getFilmDuration(),
            req.getAuditoriumName(),
            req.getStartTime(),
            req.getEndTime()
        );


        return ResponseEntity.ok(new ScreeningResponse(
                s.getScreeningID(),
                s.getProgram().getProgramID(),
                s.getFilmTitle(),
                s.getState().name()
        ));
    }

    //UPDATE DRAFT (ONLY CREATED, ONLY SUBMITTER)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDraft(@PathVariable Long id,
                                        @Valid @RequestBody UpdateScreeningRequest req,
                                        HttpServletRequest request) {
        User me = AuthUtils.requireAuthUser(request);

        Screening s = screeningService.updateDraft(
        me,
            id,
            req.getFilmTitle(),
            req.getFilmCast(),
            req.getFilmGenre(),
            req.getFilmDuration(),
            req.getAuditoriumName(),
            req.getStartTime(),
            req.getEndTime()
        );


        return ResponseEntity.ok(new ScreeningResponse(
                s.getScreeningID(),
                s.getProgram().getProgramID(),
                s.getFilmTitle(),
                s.getState().name()
        ));
    }

    //WITHDRAW DRAFT (DELETE, ONLY CREATED, ONLY SUBMITTER)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> withdraw(@PathVariable Long id, HttpServletRequest request) {
        User me = AuthUtils.requireAuthUser(request);
        screeningService.withdrawDraft(me, id);
        return ResponseEntity.ok(new SimpleMessageResponse("Draft withdrawn (deleted)."));
    }

    //SUBMIT (CREATED -> SUBMITTED, PROGRAM MUST BE SUBMISSION, SCREENING MUST BE COMPLETE)
    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submit(@PathVariable Long id, HttpServletRequest request) {
        User requester = AuthUtils.requireAuthUser(request);
        Screening s = screeningService.submitScreening(requester, id);
        return ResponseEntity.ok(new SimpleMessageResponse("Submitted. New state=" + s.getState().name()));
    }

    //ASSIGN HANDLER (PROGRAMMER, PROGRAM ASSIGNMENT)
    @PostMapping("/{id}/assign-handler")
    public ResponseEntity<?> assignHandler(@PathVariable Long id,
                                        @Valid @RequestBody AssignScreeningHandlerRequest req,
                                        HttpServletRequest request) {
        User requester = AuthUtils.requireAuthUser(request);
        Screening s = screeningService.assignHandler(requester, id, req.getStaffUserID());
        return ResponseEntity.ok(new SimpleMessageResponse("Handler assigned. Screening state=" + s.getState().name()));
    }

    //REVIEW (STAFF HANDLER, PROGRAM REVIEW)
    @PostMapping("/{id}/reviews")
    public ResponseEntity<?> addReview(@PathVariable Long id,
                                    @Valid @RequestBody CreateScreeningReviewRequest req,
                                    HttpServletRequest request) {
        User requester = AuthUtils.requireAuthUser(request);
        ScreeningReview r = screeningService.addReview(requester, id, req.getScore(), req.getComments());
        return ResponseEntity.ok(new ReviewResponse(r.getReviewID(), r.getScore(), r.getComments()));
    }

    //SCHEDULING: SUBMITTER APPROVE/REJECT
    @PostMapping("/{id}/scheduling/approve")
    public ResponseEntity<?> approveScheduling(@PathVariable Long id,
                                            @Valid @RequestBody ApproveScreeningRequest req,
                                            HttpServletRequest request) {
        User requester = AuthUtils.requireAuthUser(request);
        Screening s = screeningService.approveScheduling(requester, id, req.getApprovalNotes());
        return ResponseEntity.ok(new SimpleMessageResponse("Approved. New state=" + s.getState().name()));
    }

    @PostMapping("/{id}/scheduling/reject")
    public ResponseEntity<?> rejectScheduling(@PathVariable Long id,
                                            @Valid @RequestBody RejectScreeningRequest req,
                                            HttpServletRequest request) {
        User requester = AuthUtils.requireAuthUser(request);
        Screening s = screeningService.rejectScheduling(requester, id, req.getRejectionReason());
        return ResponseEntity.ok(new SimpleMessageResponse("Rejected. New state=" + s.getState().name()));
    }

    //SCHEDULING: PROGRAMMER MANUAL REJECT
    @PostMapping("/{id}/scheduling/programmer-reject")
    public ResponseEntity<?> programmerRejectScheduling(@PathVariable Long id,
                                                @Valid @RequestBody RejectScreeningRequest req,
                                                HttpServletRequest request) {
        User requester = AuthUtils.requireAuthUser(request);
        Screening s = screeningService.programmerRejectInScheduling(requester, id, req.getRejectionReason());
        return ResponseEntity.ok(
            new SimpleMessageResponse("Rejected by PROGRAMMER in scheduling. New state=" + s.getState().name())
        );

    }

    //SCHEDULING: PROGRAMMER TENTATIVE SCHEDULING
    @PostMapping("/{id}/scheduling/tentative")
    public ResponseEntity<?> tentativeScheduling(@PathVariable Long id,
                                            @Valid @RequestBody TentativeScheduleRequest req,
                                            HttpServletRequest request) {
    User requester = AuthUtils.requireAuthUser(request);
    Screening updated = screeningService.tentativeSchedule(requester, id,
            req.getAuditoriumName(),
            req.getStartTime(),
            req.getEndTime());
    return ResponseEntity.ok(updated);
}



    //FINAL SUBMIT (SUBMITTER, PROGRAM FINAL_SUBMISSION, SCREENING APPROVED)
    @PostMapping("/{id}/final-submit")
    public ResponseEntity<?> finalSubmit(@PathVariable Long id, HttpServletRequest request) {
        User requester = AuthUtils.requireAuthUser(request);
        Screening s = screeningService.finalSubmit(requester, id);
        return ResponseEntity.ok(new SimpleMessageResponse("Final-submitted at=" + s.getFinalSubmissionAt()));
    }

    //DECISION: ACCEPT INTO SCHEDULE (PROGRAMMER SCHEDULES)
    @PostMapping("/{id}/decision/accept")
    public ResponseEntity<?> acceptDecision(@PathVariable Long id,
                                            @Valid @RequestBody ScheduleScreeningRequest req,
                                            HttpServletRequest request) {
        User requester = AuthUtils.requireAuthUser(request);
        Screening s = screeningService.acceptIntoSchedule(requester, id, req.getAuditoriumName(), req.getStartTime());
        return ResponseEntity.ok(new SimpleMessageResponse("Scheduled. New state=" + s.getState().name()));
    }

    //DECISION: REJECT
    @PostMapping("/{id}/decision/reject")
    public ResponseEntity<?> rejectDecision(@PathVariable Long id,
                                            @Valid @RequestBody RejectScreeningRequest req,
                                            HttpServletRequest request) {
        User requester = AuthUtils.requireAuthUser(request);
        Screening s = screeningService.rejectInDecision(requester, id, req.getRejectionReason());
        return ResponseEntity.ok(new SimpleMessageResponse("Rejected in decision. New state=" + s.getState().name()));
    }

    //VIEW BY ID (PUBLIC ALLOWED ONLY IF PROGRAM ANNOUNCED + SCREENING SCHEDULED)
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, HttpServletRequest request) {
        User me = null;
        try {
            me = AuthUtils.requireAuthUser(request);
        } catch (Exception ignored) {
            //ALLOW PUBLIC WHEN PROGRAM ANNOUNCED (SERVICE WILL ENFORCE SCHEDULED-ONLY)
        }
        return ResponseEntity.ok(screeningService.viewScreening(me, id));
    }

    //SEARCH WITHIN PROGRAM (PUBLIC ALLOWED ONLY IF ANNOUNCED → SCHEDULED-ONLY REDACTED)
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam("programID") Long programID,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "cast", required = false) String cast,
            @RequestParam(name = "genre", required = false) String genre,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "view", required = false) String view,
            HttpServletRequest request
    ) {
        boolean timetableView = view != null && view.equalsIgnoreCase("timetable");

        User me = null;
        try {
            me = AuthUtils.requireAuthUser(request);
        } catch (Exception ignored) {
            //ALLOW PUBLIC WHEN PROGRAM ANNOUNCED
        }

        LocalDateTime fromDt = (from == null || from.isBlank()) ? null : LocalDateTime.parse(from.trim());
        LocalDateTime toDt = (to == null || to.isBlank()) ? null : LocalDateTime.parse(to.trim());

        List<ScreeningViewDTO> out = screeningService.searchScreeningsView(me, programID, title, cast, genre, fromDt, toDt, timetableView);

        return ResponseEntity.ok(out);
    }

    //SMALL RESPONSE DTOS
    public record ScreeningResponse(Long screeningID, Long programID, String filmTitle, String state) {}
    public record ReviewResponse(Long reviewID, Integer score, String comments) {}
}
