package com.example.backend.service;

import com.example.backend.audit.AuditService;
import com.example.backend.dto.ScreeningReviewDTO;
import com.example.backend.dto.ScreeningViewDTO;
import com.example.backend.entities.*;
import com.example.backend.exception.BadRequestException;
import com.example.backend.repository.ProgramRepository;
import com.example.backend.repository.ScreeningRepository;
import com.example.backend.repository.ScreeningReviewRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class ScreeningService {

    private final ScreeningRepository screeningRepository;
    private final ScreeningReviewRepository reviewRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final ProgramService programService;
    private final AuditService auditService;

    public ScreeningService(ScreeningRepository screeningRepository,
                            ScreeningReviewRepository reviewRepository,
                            ProgramRepository programRepository,
                            UserRepository userRepository,
                            ProgramService programService,
                            AuditService auditService) {
        this.screeningRepository = screeningRepository;
        this.reviewRepository = reviewRepository;
        this.programRepository = programRepository;
        this.userRepository = userRepository;
        this.programService = programService;
        this.auditService = auditService;
    }

    private void forbidAdminDomainOps(User u) {
        if (u.getPermanentRole() == PermanentRole.ADMIN) {
            throw new RuntimeException("ADMIN cannot perform screening domain operations");
        }
    }

    private void requireUserRole(User u) {
        if (u.getPermanentRole() != PermanentRole.USER) {
            throw new RuntimeException("Only USER can perform this action");
        }
    }

    private LocalDateTime deriveEnd(LocalDateTime start, Integer durationMinutes) {
        if (start == null || durationMinutes == null) return null;
        return start.plusMinutes(durationMinutes);
    }

    private LocalDateTime resolveEndTime(LocalDateTime start, LocalDateTime endCandidate, Integer durationMinutes) {
        if (endCandidate == null) {
            return deriveEnd(start, durationMinutes);
        }

        if (start == null || durationMinutes == null) {
            throw new BadRequestException("startTime and filmDuration are required when endTime is provided");
        }

        if (!endCandidate.isAfter(start)) {
            throw new BadRequestException("endTime must be after startTime");
        }

        long diffMinutes = Duration.between(start, endCandidate).toMinutes();
        if (diffMinutes < durationMinutes) {
            throw new BadRequestException("endTime - startTime must be >= filmDuration");
        }

        return endCandidate;
    }

    private void validateWithinProgramDates(Program program, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) return;

        LocalDate programStart = program.getStartDate();
        LocalDate programEnd = program.getEndDate();

        if (programStart != null && startTime.toLocalDate().isBefore(programStart)) {
            throw new RuntimeException("startTime is before program startDate");
        }
        if (programEnd != null && endTime.toLocalDate().isAfter(programEnd)) {
            throw new RuntimeException("endTime is after program endDate");
        }
    }

    private void requireDraftComplete(Screening s) {
        if (s.getFilmTitle() == null || s.getFilmTitle().isBlank())
            throw new RuntimeException("Film title is required");

        if (s.getFilmDuration() == null || s.getFilmDuration() <= 0)
            throw new RuntimeException("Film duration must be positive");

        if (s.getAuditoriumName() == null || s.getAuditoriumName().isBlank())
            throw new RuntimeException("auditoriumName is required (candidate)");

        if (s.getStartTime() == null)
            throw new RuntimeException("startTime is required (candidate)");

        if (s.getEndTime() == null)
            throw new RuntimeException("endTime is required (candidate or derived)");

        long diffMinutes = Duration.between(s.getStartTime(), s.getEndTime()).toMinutes();
        if (diffMinutes < s.getFilmDuration()) {
            throw new RuntimeException("endTime - startTime must be >= filmDuration");
        }
    }

    //CREATE DRAFT (CREATED)
    @Transactional
    public Screening createDraft(User submitter,
                                Long programID,
                                String title,
                                String cast,
                                String genre,
                                Integer duration,
                                String auditoriumCandidate,
                                LocalDateTime startTimeCandidate,
                                LocalDateTime endTimeCandidate) {

        forbidAdminDomainOps(submitter);
        requireUserRole(submitter);

        Program program = programRepository.findById(programID)
                .orElseThrow(() -> new RuntimeException("Program not found"));

        //PROGRAMMER/STAFF CANNOT SUBMIT TO OWN PROGRAM
        if (programService.isProgrammer(programID, submitter.getUserID())
                || programService.isStaff(programID, submitter.getUserID())) {
            throw new RuntimeException("Program members cannot submit screenings to their own program");
        }

        if (title == null || title.isBlank()) throw new RuntimeException("Film title is required");
        if (duration == null || duration <= 0) throw new RuntimeException("Film duration must be positive");

        Screening s = new Screening(program, submitter, title.trim(), cast, genre, duration);
        s.setState(ScreeningState.CREATED);

        if (auditoriumCandidate != null && !auditoriumCandidate.isBlank()) {
            s.setAuditoriumName(auditoriumCandidate.trim());
        } else {
            s.setAuditoriumName(null);
        }

        s.setStartTime(startTimeCandidate);
        s.setEndTime(resolveEndTime(startTimeCandidate, endTimeCandidate, duration));

        validateWithinProgramDates(program, s.getStartTime(), s.getEndTime());

        // PDF FIELDS
        s.setFinalSubmissionAt(null);
        s.setApprovalNotes(null);
        s.setRejectionReason(null);
        s.setHandlerStaff(null);

        Screening saved = screeningRepository.save(s);

        auditService.log(submitter, "SCREENING_DRAFT_CREATED", "SCREENING", saved.getScreeningID(),
                "programID=" + programID + " title=" + saved.getFilmTitle());

        return saved;
    }

    //UPDATE DRAFT (ONLY CREATED, ONLY SUBMITTER)
    @Transactional
    public Screening updateDraft(User submitter,
                                Long screeningID,
                                String title,
                                String cast,
                                String genre,
                                Integer duration,
                                String auditoriumCandidate,
                                LocalDateTime startTimeCandidate,
                                LocalDateTime endTimeCandidate) {

        forbidAdminDomainOps(submitter);
        requireUserRole(submitter);

        Screening s = screeningRepository.findById(screeningID)
                .orElseThrow(() -> new RuntimeException("Screening not found"));

        if (!Objects.equals(s.getSubmitter().getUserID(), submitter.getUserID())) {
            throw new RuntimeException("Only submitter can update this screening");
        }
        if (s.getState() != ScreeningState.CREATED) {
            throw new RuntimeException("Only CREATED screenings can be updated");
        }

        if (title == null || title.isBlank()) throw new RuntimeException("Film title is required");
        if (duration == null || duration <= 0) throw new RuntimeException("Film duration must be positive");

        s.setFilmTitle(title.trim());
        s.setFilmCast(cast);
        s.setFilmGenre(genre);
        s.setFilmDuration(duration);

        if (auditoriumCandidate != null && !auditoriumCandidate.isBlank()) {
            s.setAuditoriumName(auditoriumCandidate.trim());
        } else {
            s.setAuditoriumName(null);
        }

        s.setStartTime(startTimeCandidate);
        s.setEndTime(resolveEndTime(startTimeCandidate, endTimeCandidate, duration));

        validateWithinProgramDates(s.getProgram(), s.getStartTime(), s.getEndTime());

        Screening saved = screeningRepository.save(s);

        auditService.log(submitter, "SCREENING_DRAFT_UPDATED", "SCREENING", saved.getScreeningID(),
                "programID=" + saved.getProgram().getProgramID() + " title=" + saved.getFilmTitle());

        return saved;
    }

    //WITHDRAW DRAFT (DELETE, ONLY CREATED, ONLY SUBMITTER)
    @Transactional
    public void withdrawDraft(User submitter, Long screeningID) {
        forbidAdminDomainOps(submitter);
        requireUserRole(submitter);

        Screening s = screeningRepository.findById(screeningID)
                .orElseThrow(() -> new RuntimeException("Screening not found"));

        if (!Objects.equals(s.getSubmitter().getUserID(), submitter.getUserID())) {
            throw new RuntimeException("Only submitter can withdraw this screening");
        }
        if (s.getState() != ScreeningState.CREATED) {
            throw new RuntimeException("Only CREATED screenings can be withdrawn");
        }

        Long programID = s.getProgram().getProgramID();

        screeningRepository.delete(s);

        auditService.log(submitter, "SCREENING_DRAFT_WITHDRAWN", "SCREENING", screeningID,
                "programID=" + programID);
    }

    //SUBMIT (CREATED -> SUBMITTED, PROGRAM MUST BE SUBMISSION, SCREENING MUST BE COMPLETE)
    @Transactional
    public Screening submitScreening(User submitter, Long screeningID) {
        forbidAdminDomainOps(submitter);
        requireUserRole(submitter);

        Screening s = screeningRepository.findById(screeningID)
                .orElseThrow(() -> new RuntimeException("Screening not found"));

        if (!Objects.equals(s.getSubmitter().getUserID(), submitter.getUserID())) {
            throw new RuntimeException("Only submitter can submit this screening");
        }
        if (s.getState() != ScreeningState.CREATED) {
            throw new RuntimeException("Only CREATED screenings can be submitted");
        }
        if (s.getProgram().getState() != ProgramState.SUBMISSION) {
            throw new RuntimeException("Program is not in SUBMISSION state");
        }

        requireDraftComplete(s);

        s.setState(ScreeningState.SUBMITTED);

        Screening saved = screeningRepository.save(s);

        auditService.log(submitter, "SCREENING_SUBMITTED", "SCREENING", screeningID,
                "programID=" + saved.getProgram().getProgramID());

        return saved;
    }

    //ASSIGN HANDLER (PROGRAMMER, PROGRAM ASSIGNMENT)
    @Transactional
    public Screening assignHandler(User requester, Long screeningID, Long staffUserID) {
        forbidAdminDomainOps(requester);

        Screening s = screeningRepository.findById(screeningID)
                .orElseThrow(() -> new RuntimeException("Screening not found"));

        Long programID = s.getProgram().getProgramID();

        if (!programService.isProgrammer(programID, requester.getUserID())) {
            throw new RuntimeException("Only PROGRAMMER can assign handler");
        }
        if (s.getProgram().getState() != ProgramState.ASSIGNMENT) {
            throw new RuntimeException("Program is not in ASSIGNMENT state");
        }
        if (s.getState() != ScreeningState.SUBMITTED && s.getState() != ScreeningState.REVIEWED) {
            throw new RuntimeException("Only SUBMITTED/REVIEWED screenings can be assigned");
        }
        if (!programService.isStaff(programID, staffUserID)) {
            throw new RuntimeException("Selected user is not STAFF in this program");
        }

        User staff = userRepository.findById(staffUserID)
                .orElseThrow(() -> new RuntimeException("Staff user not found"));

        s.setHandlerStaff(staff);

        Screening saved = screeningRepository.save(s);

        auditService.log(requester, "SCREENING_HANDLER_ASSIGNED", "SCREENING", screeningID,
                "programID=" + programID + " staffUserID=" + staffUserID);

        return saved;
    }

    //REVIEW (STAFF HANDLER, PROGRAM REVIEW)
    @Transactional
    public ScreeningReview addReview(User requester, Long screeningID, int score, String comments) {
        forbidAdminDomainOps(requester);

        Screening s = screeningRepository.findById(screeningID)
                .orElseThrow(() -> new RuntimeException("Screening not found"));

        Long programID = s.getProgram().getProgramID();

        if (s.getProgram().getState() != ProgramState.REVIEW) {
            throw new RuntimeException("Program is not in REVIEW state");
        }
        if (s.getHandlerStaff() == null) {
            throw new RuntimeException("No handler assigned");
        }
        if (!Objects.equals(s.getHandlerStaff().getUserID(), requester.getUserID())) {
            throw new RuntimeException("Only assigned STAFF can review");
        }
        if (!programService.isStaff(programID, requester.getUserID())) {
            throw new RuntimeException("Requester is not STAFF in this program");
        }
        if (s.getState() != ScreeningState.SUBMITTED && s.getState() != ScreeningState.REVIEWED) {
            throw new RuntimeException("Only SUBMITTED/REVIEWED screenings can be reviewed");
        }
        if (score < 1 || score > 10) {
            throw new RuntimeException("Score must be between 1 and 10");
        }

        ScreeningReview r = new ScreeningReview(s, requester, score, comments);
        ScreeningReview savedReview = reviewRepository.save(r);

        auditService.log(requester, "SCREENING_REVIEW_ADDED", "SCREENING", screeningID,
                "programID=" + programID + " score=" + score + " reviewID=" + savedReview.getReviewID());

        s.setState(ScreeningState.REVIEWED);
        screeningRepository.save(s);

        return savedReview;
    }

    //SCHEDULING APPROVAL/REJECT (SUBMITTER, PROGRAM SCHEDULING, SCREENING REVIEWED)
    @Transactional
    public Screening approveScheduling(User submitter, Long screeningID, String approvalNotes) {
        forbidAdminDomainOps(submitter);
        requireUserRole(submitter);

        Screening s = screeningRepository.findById(screeningID)
                .orElseThrow(() -> new RuntimeException("Screening not found"));

        if (!Objects.equals(s.getSubmitter().getUserID(), submitter.getUserID())) {
            throw new RuntimeException("Only submitter can approve/reject scheduling");
        }
        if (s.getProgram().getState() != ProgramState.SCHEDULING) {
            throw new RuntimeException("Program is not in SCHEDULING state");
        }
        if (s.getState() != ScreeningState.REVIEWED) {
            throw new RuntimeException("Only REVIEWED screenings can be approved/rejected in SCHEDULING");
        }

        s.setApprovalNotes(approvalNotes);
        s.setRejectionReason(null);
        s.setState(ScreeningState.APPROVED);

        Screening saved = screeningRepository.save(s);

        auditService.log(submitter, "SCREENING_APPROVED_BY_SUBMITTER", "SCREENING", screeningID,
                "programID=" + saved.getProgram().getProgramID());

        return saved;
    }

    @Transactional
    public Screening rejectScheduling(User submitter, Long screeningID, String rejectionReason) {
        forbidAdminDomainOps(submitter);
        requireUserRole(submitter);

        Screening s = screeningRepository.findById(screeningID)
                .orElseThrow(() -> new RuntimeException("Screening not found"));

        if (!Objects.equals(s.getSubmitter().getUserID(), submitter.getUserID())) {
            throw new RuntimeException("Only submitter can approve/reject scheduling");
        }
        if (s.getProgram().getState() != ProgramState.SCHEDULING) {
            throw new RuntimeException("Program is not in SCHEDULING state");
        }
        if (s.getState() != ScreeningState.REVIEWED) {
            throw new RuntimeException("Only REVIEWED screenings can be approved/rejected in SCHEDULING");
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new RuntimeException("Rejection reason is required");
        }

        s.setRejectionReason(rejectionReason);
        s.setApprovalNotes(null);
        s.setState(ScreeningState.REJECTED);

        Screening saved = screeningRepository.save(s);

        auditService.log(submitter, "SCREENING_REJECTED_BY_SUBMITTER", "SCREENING", screeningID,
                "programID=" + saved.getProgram().getProgramID() + " reason=" + rejectionReason);

        return saved;
    }

    //SCHEDULING: TENTATIVE SCHEDULE (PROGRAMMER)
    @Transactional
    public Screening tentativeSchedule(User requester,
                                    Long screeningID,
                                    String auditoriumName,
                                    LocalDateTime startTime,
                                    LocalDateTime endTime) {

        forbidAdminDomainOps(requester);

        Screening s = screeningRepository.findById(screeningID)
                .orElseThrow(() -> new RuntimeException("Screening not found"));

        Long programID = s.getProgram().getProgramID();

        if (!programService.isProgrammer(programID, requester.getUserID())) {
            throw new RuntimeException("Only PROGRAMMER can do tentative scheduling");
        }
        if (s.getProgram().getState() != ProgramState.SCHEDULING) {
            throw new RuntimeException("Program is not in SCHEDULING state");
        }
        if (s.getState() != ScreeningState.REVIEWED) {
            throw new RuntimeException("Only REVIEWED screenings can be tentatively scheduled");
        }

        if (auditoriumName == null || auditoriumName.isBlank()) throw new RuntimeException("auditoriumName is required");
        if (startTime == null) throw new RuntimeException("startTime is required");
        if (endTime == null) throw new RuntimeException("endTime is required");
        if (!endTime.isAfter(startTime)) throw new RuntimeException("endTime must be after startTime");

        if (s.getFilmDuration() == null || s.getFilmDuration() <= 0) {
            throw new RuntimeException("filmDuration is required");
        }
        long diffMinutes = Duration.between(startTime, endTime).toMinutes();
        if (diffMinutes < s.getFilmDuration()) {
            throw new RuntimeException("endTime - startTime must be >= filmDuration");
        }

        validateWithinProgramDates(s.getProgram(), startTime, endTime);

        if (existsOverlappingScreening(programID, auditoriumName, startTime, endTime, screeningID)) {
            throw new RuntimeException("Scheduling overlap: another screening exists in that auditorium/time window");
        }

        s.setAuditoriumName(auditoriumName.trim());
        s.setStartTime(startTime);
        s.setEndTime(endTime);

        Screening saved = screeningRepository.save(s);

        auditService.log(requester,
                "SCREENING_TENTATIVE_SCHEDULED",
                "SCREENING",
                screeningID,
                "programID=" + programID + " auditorium=" + auditoriumName +
                        " start=" + startTime + " end=" + endTime);

        return saved;
    }

    //FINAL SUBMIT (SUBMITTER, PROGRAM FINAL_PUBLICATION, SCREENING APPROVED)
    @Transactional
    public Screening finalSubmit(User submitter, Long screeningID) {
        forbidAdminDomainOps(submitter);
        requireUserRole(submitter);

        Screening s = screeningRepository.findById(screeningID)
                .orElseThrow(() -> new RuntimeException("Screening not found"));

        if (!Objects.equals(s.getSubmitter().getUserID(), submitter.getUserID())) {
            throw new RuntimeException("Only submitter can final-submit");
        }
        if (s.getProgram().getState() != ProgramState.FINAL_PUBLICATION) {
            throw new RuntimeException("Program is not in FINAL_PUBLICATION state");
        }
        if (s.getState() != ScreeningState.APPROVED) {
            throw new RuntimeException("Only APPROVED screenings can be final-submitted");
        }
        if (s.getFinalSubmissionAt() != null) {
            throw new RuntimeException("Screening has already been final-submitted");
        }

        s.setFinalSubmissionAt(Instant.now());
        Screening saved = screeningRepository.save(s);

        auditService.log(submitter, "SCREENING_FINAL_SUBMITTED", "SCREENING", screeningID,
                "programID=" + saved.getProgram().getProgramID());

        return saved;
    }

    //DECISION: AUTO-REJECT APPROVED WITHOUT FINAL SUBMIT
    @Transactional
    public int autoRejectApprovedWithoutFinalSubmission(Long programID) {
        List<Screening> list = screeningRepository.findApprovedWithoutFinalSubmission(programID);
        int changed = 0;

        for (Screening s : list) {
            s.setState(ScreeningState.REJECTED);
            s.setRejectionReason("Auto-rejected: no final submission");
            s.setApprovalNotes(null);
            screeningRepository.save(s);
            changed++;

            auditService.logAnonymous("SCREENING_AUTO_REJECTED", "SCREENING", s.getScreeningID(),
                    "programID=" + programID);
        }

        auditService.logAnonymous("SCREENINGS_AUTO_REJECT_SUMMARY", "PROGRAM", programID,
                "count=" + changed);

        return changed;
    }

    //DECISION: ACCEPT INTO SCHEDULE (PROGRAMMER, PROGRAM DECISION)
    @Transactional
    public Screening acceptIntoSchedule(User requester,
                                        Long screeningID,
                                        String auditoriumName,
                                        LocalDateTime startTime) {
        forbidAdminDomainOps(requester);

        Screening s = screeningRepository.findById(screeningID)
                .orElseThrow(() -> new RuntimeException("Screening not found"));

        Long programID = s.getProgram().getProgramID();

        if (!programService.isProgrammer(programID, requester.getUserID())) {
            throw new RuntimeException("Only PROGRAMMER can accept/reject in DECISION");
        }
        if (s.getProgram().getState() != ProgramState.DECISION) {
            throw new RuntimeException("Program is not in DECISION state");
        }
        if (s.getState() != ScreeningState.APPROVED) {
            throw new RuntimeException("Only APPROVED screenings can be accepted into schedule");
        }
        if (s.getFinalSubmissionAt() == null) {
            throw new RuntimeException("Cannot schedule without final submission");
        }
        if (auditoriumName == null || auditoriumName.isBlank()) {
            throw new RuntimeException("auditoriumName is required");
        }
        if (startTime == null) {
            throw new RuntimeException("startTime is required");
        }

        LocalDateTime endTime = startTime.plusMinutes(s.getFilmDuration());

        validateWithinProgramDates(s.getProgram(), startTime, endTime);

        List<Screening> overlaps = screeningRepository.findOverlappingScheduled(
                programID, auditoriumName.trim(), startTime, endTime
        );
        if (!overlaps.isEmpty()) {
            throw new RuntimeException("Schedule overlap detected in auditorium");
        }

        s.setAuditoriumName(auditoriumName.trim());
        s.setStartTime(startTime);
        s.setEndTime(endTime);
        s.setState(ScreeningState.SCHEDULED);

        Screening saved = screeningRepository.save(s);

        auditService.log(requester, "SCREENING_ACCEPTED_SCHEDULED", "SCREENING", screeningID,
                "programID=" + saved.getProgram().getProgramID() +
                        " auditorium=" + saved.getAuditoriumName() +
                        " start=" + saved.getStartTime() + " end=" + saved.getEndTime());

        return saved;
    }

    //DECISION: REJECT (PROGRAMMER, PROGRAM DECISION)
    @Transactional
    public Screening rejectInDecision(User requester, Long screeningID, String reason) {
        forbidAdminDomainOps(requester);

        Screening s = screeningRepository.findById(screeningID)
                .orElseThrow(() -> new RuntimeException("Screening not found"));

        Long programID = s.getProgram().getProgramID();

        if (!programService.isProgrammer(programID, requester.getUserID())) {
            throw new RuntimeException("Only PROGRAMMER can accept/reject in DECISION");
        }
        if (s.getProgram().getState() != ProgramState.DECISION) {
            throw new RuntimeException("Program is not in DECISION state");
        }
        if (s.getState() != ScreeningState.APPROVED) {
            throw new RuntimeException("Only APPROVED screenings can be rejected in DECISION");
        }
        if (reason == null || reason.isBlank()) {
            throw new RuntimeException("Rejection reason is required");
        }

        s.setState(ScreeningState.REJECTED);
        s.setRejectionReason(reason);
        s.setApprovalNotes(null);

        Screening saved = screeningRepository.save(s);

        auditService.log(requester, "SCREENING_REJECTED_DECISION", "SCREENING", screeningID,
                "programID=" + saved.getProgram().getProgramID() + " reason=" + reason);

        return saved;
    }

    //SCHEDULING: MANUAL REJECT BY PROGRAMMER
    @Transactional
    public Screening programmerRejectInScheduling(User requester, Long screeningID, String reason) {
        forbidAdminDomainOps(requester);

        Screening s = screeningRepository.findById(screeningID)
                .orElseThrow(() -> new RuntimeException("Screening not found"));

        Long programID = s.getProgram().getProgramID();

        if (!programService.isProgrammer(programID, requester.getUserID())) {
            throw new RuntimeException("Only PROGRAMMER can reject in SCHEDULING");
        }
        if (s.getProgram().getState() != ProgramState.SCHEDULING) {
            throw new RuntimeException("Program is not in SCHEDULING state");
        }
        if (s.getState() != ScreeningState.REVIEWED) {
            throw new RuntimeException("Only REVIEWED screenings can be rejected by PROGRAMMER in SCHEDULING");
        }
        if (reason == null || reason.isBlank()) {
            throw new RuntimeException("Rejection reason is required");
        }

        s.setState(ScreeningState.REJECTED);
        s.setRejectionReason(reason);
        s.setApprovalNotes(null);

        Screening saved = screeningRepository.save(s);

        auditService.log(requester,
                "SCREENING_REJECTED_SCHEDULING",
                "SCREENING",
                screeningID,
                "programID=" + programID + " reason=" + reason);

        return saved;
    }

    //VIEW
    @Transactional(readOnly = true)
    public Screening getById(User requester, Long screeningID) {
        Screening s = screeningRepository.findById(screeningID)
                .orElseThrow(() -> new RuntimeException("Screening not found"));

        Program p = s.getProgram();
        Long programID = p.getProgramID();

        if (p.getState() == ProgramState.ANNOUNCED) {
            if (s.getState() != ScreeningState.SCHEDULED) {
                throw new RuntimeException("Forbidden");
            }
            return s;
        }

        if (requester == null) throw new RuntimeException("Forbidden");

        boolean isSubmitter = Objects.equals(s.getSubmitter().getUserID(), requester.getUserID());
        boolean isHandler = s.getHandlerStaff() != null && Objects.equals(s.getHandlerStaff().getUserID(), requester.getUserID());
        boolean isProgrammer = programService.isProgrammer(programID, requester.getUserID());
        boolean isStaff = programService.isStaff(programID, requester.getUserID());

        if (isSubmitter || isHandler || isProgrammer || isStaff) return s;

        throw new RuntimeException("Forbidden");
    }

    //SEARCH HELPERS
    private boolean matchesAllWords(String fieldValue, String filterValue) {
        if (filterValue == null || filterValue.isBlank()) return true;
        if (fieldValue == null) return false;

        String hay = fieldValue.toLowerCase();
        String[] words = filterValue.trim().toLowerCase().split("\\s+");

        for (String w : words) {
            if (w.isBlank()) continue;
            if (!hay.contains(w)) return false;
        }
        return true;
    }

    private Comparator<Screening> screeningSortComparator(boolean timetableView) {
        if (timetableView) {
            return Comparator
                    .comparing(Screening::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Screening::getFilmTitle, Comparator.nullsLast(String::compareToIgnoreCase));
        }

        return Comparator
                .comparing(Screening::getFilmGenre, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(Screening::getFilmTitle, Comparator.nullsLast(String::compareToIgnoreCase));
    }

    //SEARCH WITHIN PROGRAM
    @Transactional(readOnly = true)
    public List<Screening> searchInProgram(User requester,
                                        Long programID,
                                        String title,
                                        String cast,
                                        String genre,
                                        LocalDateTime from,
                                        LocalDateTime to,
                                        boolean timetableView) {

        Program p = programRepository.findById(programID)
                .orElseThrow(() -> new RuntimeException("Program not found"));

        boolean publicOk = p.getState() == ProgramState.ANNOUNCED;
        if (!publicOk && requester == null) throw new RuntimeException("Forbidden");

        return screeningRepository.findByProgram_ProgramID(programID).stream()
                .filter(s -> publicOk ? (s.getState() == ScreeningState.SCHEDULED) : true)
                .filter(s -> matchesAllWords(s.getFilmTitle(), title))
                .filter(s -> matchesAllWords(s.getFilmCast(), cast))
                .filter(s -> matchesAllWords(s.getFilmGenre(), genre))
                .filter(s -> from == null || (s.getStartTime() != null && !s.getStartTime().isBefore(from)))
                .filter(s -> to == null || (s.getStartTime() != null && !s.getStartTime().isAfter(to)))
                .sorted(screeningSortComparator(timetableView))
                .toList();
    }

    private boolean hasFullAccess(User requester, Screening s) {
        if (requester == null) return false;
        if (requester.getPermanentRole() != PermanentRole.USER) return false;

        Long programID = s.getProgram().getProgramID();
        boolean isProgrammer = programService.isProgrammer(programID, requester.getUserID());
        boolean isSubmitterOwner = s.getSubmitter() != null && s.getSubmitter().getUserID().equals(requester.getUserID());
        boolean isHandler = s.getHandlerStaff() != null && s.getHandlerStaff().getUserID().equals(requester.getUserID());

        return isProgrammer || isSubmitterOwner || isHandler;
    }

    private boolean existsOverlappingScreening(Long programID,
                                            String auditoriumName,
                                            LocalDateTime start,
                                            LocalDateTime end,
                                            Long excludeScreeningID) {

        List<Screening> all = screeningRepository.findByProgram_ProgramID(programID);

        for (Screening other : all) {
            if (excludeScreeningID != null && other.getScreeningID().equals(excludeScreeningID)) continue;
            if (other.getAuditoriumName() == null) continue;
            if (!other.getAuditoriumName().equalsIgnoreCase(auditoriumName)) continue;
            if (other.getStartTime() == null || other.getEndTime() == null) continue;

            boolean overlaps = start.isBefore(other.getEndTime()) && end.isAfter(other.getStartTime());
            if (overlaps) return true;
        }
        return false;
    }

    private ScreeningViewDTO toViewDTO(User requester, Screening s) {
        ScreeningViewDTO dto = new ScreeningViewDTO();
        dto.setScreeningID(s.getScreeningID());
        dto.setProgramID(s.getProgram().getProgramID());
        dto.setFilmTitle(s.getFilmTitle());
        dto.setFilmGenre(s.getFilmGenre());
        dto.setStartTime(s.getStartTime());
        dto.setEndTime(s.getEndTime());
        dto.setAuditoriumName(s.getAuditoriumName());

        boolean full = hasFullAccess(requester, s);

        if (full) {
            dto.setFilmCast(s.getFilmCast());
            dto.setFilmDuration(s.getFilmDuration());
            dto.setState(s.getState().name());

            dto.setSubmitterUserID(s.getSubmitter() == null ? null : s.getSubmitter().getUserID());
            dto.setHandlerStaffUserID(s.getHandlerStaff() == null ? null : s.getHandlerStaff().getUserID());

            dto.setApprovalNotes(s.getApprovalNotes());
            dto.setRejectionReason(s.getRejectionReason());
            dto.setFinalSubmissionAt(s.getFinalSubmissionAt() == null ? null : s.getFinalSubmissionAt().toString());

            dto.setReviews(
                    reviewRepository.findByScreening_ScreeningID(s.getScreeningID()).stream()
                            .map(r -> new ScreeningReviewDTO(
                                    r.getReviewID(),
                                    r.getScore(),
                                    r.getComments(),
                                    r.getStaff() == null ? null : r.getStaff().getUserID()
                            ))
                            .toList()
            );
        } else {
            dto.setFilmCast(null);
            dto.setFilmDuration(null);
            dto.setState(null);
            dto.setSubmitterUserID(null);
            dto.setHandlerStaffUserID(null);
            dto.setApprovalNotes(null);
            dto.setRejectionReason(null);
            dto.setFinalSubmissionAt(null);
            dto.setReviews(null);
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public ScreeningViewDTO viewScreening(User requester, Long screeningID) {
        Screening s = getById(requester, screeningID);
        return toViewDTO(requester, s);
    }

    @Transactional(readOnly = true)
    public List<ScreeningViewDTO> searchScreeningsView(User requester,
                                                    Long programID,
                                                    String title,
                                                    String cast,
                                                    String genre,
                                                    LocalDateTime from,
                                                    LocalDateTime to) {
        return searchScreeningsView(requester, programID, title, cast, genre, from, to, false);
    }

    @Transactional(readOnly = true)
    public List<ScreeningViewDTO> searchScreeningsView(User requester,
                                                    Long programID,
                                                    String title,
                                                    String cast,
                                                    String genre,
                                                    LocalDateTime from,
                                                    LocalDateTime to,
                                                    boolean timetableView) {
        List<Screening> list = searchInProgram(requester, programID, title, cast, genre, from, to, timetableView);
        return list.stream().map(s -> toViewDTO(requester, s)).toList();
    }
}
