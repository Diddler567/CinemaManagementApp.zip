package com.example.backend.service;

import com.example.backend.audit.AuditService;
import com.example.backend.dto.CreateProgramRequest;
import com.example.backend.dto.ProgramDetailsDTO;
import com.example.backend.dto.ProgramSearchResponseDTO;
import com.example.backend.dto.UpdateProgramRequest;
import com.example.backend.entities.*;
import com.example.backend.repository.ProgramRepository;
import com.example.backend.repository.ProgramRoleRepository;
import com.example.backend.repository.ScreeningRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.spec.ProgramSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ProgramService {

    private final ProgramRepository programRepository;
    private final ProgramRoleRepository programRoleRepository;
    private final UserRepository userRepository;
    private final ScreeningRepository screeningRepository;
    private final AuditService auditService;

    public ProgramService(ProgramRepository programRepository,
                        ProgramRoleRepository programRoleRepository,
                        UserRepository userRepository,
                        ScreeningRepository screeningRepository,
                        AuditService auditService) {
        this.programRepository = programRepository;
        this.programRoleRepository = programRoleRepository;
        this.userRepository = userRepository;
        this.screeningRepository = screeningRepository;
        this.auditService = auditService;
    }

    private void forbidAdminDomainOps(User user) {
        if (user == null) throw new RuntimeException("Unauthorized");
        if (user.getPermanentRole() == PermanentRole.ADMIN) {
            throw new RuntimeException("Admins cannot perform domain operations");
        }
    }

    private boolean isProgramOwner(User requester, Program p) {
        return p.getCreator() != null && requester.getUserID().equals(p.getCreator().getUserID());
    }

    public boolean isProgrammer(Long programID, Long userID) {
        return programRoleRepository.existsByProgram_ProgramIDAndUser_UserIDAndRole(
                programID, userID, ProgramRoleType.PROGRAMMER
        );
    }

    public boolean isStaff(Long programID, Long userID) {
        return programRoleRepository.existsByProgram_ProgramIDAndUser_UserIDAndRole(
                programID, userID, ProgramRoleType.STAFF
        );
    }

    private void ensureCreatorProgrammer(Program p) {
        if (p.getCreator() == null) return;
        Long programID = p.getProgramID();
        Long creatorId = p.getCreator().getUserID();

        Optional<ProgramRole> existing = programRoleRepository.findByProgram_ProgramIDAndUser_UserID(programID, creatorId);
        if (existing.isPresent()) {
            ProgramRole pr = existing.get();
            pr.setRole(ProgramRoleType.PROGRAMMER);
            programRoleRepository.save(pr);
        } else {
            programRoleRepository.save(new ProgramRole(p, p.getCreator(), ProgramRoleType.PROGRAMMER));
        }
    }

    @Transactional
    public ProgramDetailsDTO createProgram(User creator, CreateProgramRequest req) {
        forbidAdminDomainOps(creator);

        if (req.getName() == null || req.getName().isBlank()) throw new RuntimeException("Name is required");
        if (programRepository.existsByName(req.getName().trim())) {
            throw new RuntimeException("Program name already exists");
        }

        Program p = new Program();
        p.setName(req.getName().trim());
        p.setDescription(req.getDescription());
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setCreator(creator);
        p.setState(ProgramState.CREATED);

        Program saved = programRepository.save(p);

        //CREATOR ALWAYS PROGRAMMER
        ensureCreatorProgrammer(saved);

        auditService.log(creator, "PROGRAM_CREATED", "PROGRAM", saved.getProgramID(),
                "name=" + saved.getName());

        return toDetailsDTO(saved, creator);
    }

    @Transactional
    public ProgramDetailsDTO updateProgram(User requester, Long programID, UpdateProgramRequest req) {
        forbidAdminDomainOps(requester);

        Program p = programRepository.findById(programID)
                .orElseThrow(() -> new RuntimeException("Program not found"));

        if (p.getState() == ProgramState.ANNOUNCED) {
            throw new RuntimeException("Cannot update ANNOUNCED program");
        }

        boolean canEdit = isProgramOwner(requester, p) || isProgrammer(programID, requester.getUserID());
        if (!canEdit) throw new RuntimeException("Forbidden");

        if (req.getName() != null && !req.getName().isBlank()) {
            String newName = req.getName().trim();
            if (!newName.equalsIgnoreCase(p.getName()) && programRepository.existsByName(newName)) {
                throw new RuntimeException("Program name already exists");
            }
            p.setName(newName);
        }

        if (req.getDescription() != null && !req.getDescription().isBlank()) {
            p.setDescription(req.getDescription().trim());
        }

        LocalDate newStart = (req.getStartDate() != null) ? req.getStartDate() : p.getStartDate();
        LocalDate newEnd = (req.getEndDate() != null) ? req.getEndDate() : p.getEndDate();

        if (newStart != null && newEnd != null && newStart.isAfter(newEnd)) {
            throw new RuntimeException("startDate must be <= endDate");
        }
        if (req.getStartDate() != null) p.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) p.setEndDate(req.getEndDate());

        Program saved = programRepository.save(p);

        //BULK ROLE SET
        if (req.getProgrammerUserIDs() != null) {
            setProgrammers(saved, req.getProgrammerUserIDs());
        }
        if (req.getStaffUserIDs() != null) {
            for (Long id : req.getStaffUserIDs()) {
                if (programRoleRepository.existsByProgram_ProgramIDAndUser_UserIDAndRole(programID, id, ProgramRoleType.PROGRAMMER)) {
                    throw new RuntimeException("Conflict: staff user already programmer");
                }
            }
            setStaff(saved, req.getStaffUserIDs());
        }

        ensureCreatorProgrammer(saved);

        auditService.log(requester, "PROGRAM_UPDATED", "PROGRAM", saved.getProgramID(),
                "name=" + saved.getName());

        return toDetailsDTO(saved, requester);
    }

    private void setProgrammers(Program program, List<Long> userIds) {
        Long programID = program.getProgramID();
        for (Long uid : userIds) {
            if (programRoleRepository.existsByProgram_ProgramIDAndUser_UserIDAndRole(programID, uid, ProgramRoleType.STAFF)) {
                throw new RuntimeException("Conflict: user already STAFF");
            }
            User u = userRepository.findById(uid).orElseThrow(() -> new RuntimeException("User not found"));
            ProgramRole pr = programRoleRepository.findByProgram_ProgramIDAndUser_UserID(programID, uid)
                    .orElse(new ProgramRole(program, u, ProgramRoleType.PROGRAMMER));
            pr.setRole(ProgramRoleType.PROGRAMMER);
            programRoleRepository.save(pr);
        }
    }

    private void setStaff(Program program, List<Long> userIds) {
        Long programID = program.getProgramID();
        if (program.getState() != ProgramState.CREATED) {
            throw new RuntimeException("STAFF set cannot be modified after SUBMISSION starts");
        }

        for (ProgramRole pr : programRoleRepository.findByProgram_ProgramIDAndRole(programID, ProgramRoleType.STAFF)) {
            if (!userIds.contains(pr.getUser().getUserID())) {
                programRoleRepository.delete(pr);
            }
        }

        for (Long uid : userIds) {
            if (programRoleRepository.existsByProgram_ProgramIDAndUser_UserIDAndRole(programID, uid, ProgramRoleType.PROGRAMMER)) {
                throw new RuntimeException("Conflict: user already PROGRAMMER");
            }
            User u = userRepository.findById(uid).orElseThrow(() -> new RuntimeException("User not found"));
            ProgramRole pr = programRoleRepository.findByProgram_ProgramIDAndUser_UserID(programID, uid)
                    .orElse(new ProgramRole(program, u, ProgramRoleType.STAFF));
            pr.setRole(ProgramRoleType.STAFF);
            programRoleRepository.save(pr);
        }
    }

    @Transactional
    public void deleteProgram(User requester, Long programID) {
        forbidAdminDomainOps(requester);

        Program p = programRepository.findById(programID)
                .orElseThrow(() -> new RuntimeException("Program not found"));

        boolean canDelete = isProgramOwner(requester, p) || isProgrammer(programID, requester.getUserID());
        if (!canDelete) throw new RuntimeException("Forbidden");

        if (p.getState() != ProgramState.CREATED) {
            throw new RuntimeException("Only CREATED programs can be deleted");
        }

        programRoleRepository.findByProgram_ProgramID(programID).forEach(programRoleRepository::delete);
        programRepository.delete(p);

        auditService.log(requester, "PROGRAM_DELETED", "PROGRAM", programID,
                "name=" + p.getName());
    }


    private void enforceTransitionPreconditions(Program p, ProgramState current, ProgramState next) {
        Long programID = p.getProgramID();
        List<Screening> screenings = screeningRepository.findByProgram_ProgramID(programID);

        
        
        if (current == ProgramState.ASSIGNMENT && next == ProgramState.REVIEW) {
            List<Long> missing = screenings.stream()
                    .filter(s -> s.getState() == ScreeningState.SUBMITTED)
                    .filter(s -> s.getHandlerStaff() == null)
                    .map(Screening::getScreeningID)
                    .toList();

            if (!missing.isEmpty()) {
                throw new RuntimeException("Cannot enter REVIEW: handler STAFF not assigned for screenings " + missing);
            }
        }

        
        if (current == ProgramState.REVIEW && next == ProgramState.SCHEDULING) {
            boolean hasPending = screenings.stream()
                    .anyMatch(s -> s.getState() == ScreeningState.SUBMITTED);

            if (hasPending) {
                throw new RuntimeException("Cannot enter SCHEDULING: there are screenings still pending review (SUBMITTED)");
            }
        }

        
        if (current == ProgramState.SCHEDULING && next == ProgramState.FINAL_PUBLICATION) {
            boolean hasUndecided = screenings.stream()
                .anyMatch(s -> s.getState() == ScreeningState.REVIEWED);

            if (hasUndecided) {
                throw new RuntimeException("Cannot enter FINAL_PUBLICATION: there are screenings still undecided by submitter (REVIEWED)");
            }
        }


        
        if (current == ProgramState.DECISION && next == ProgramState.ANNOUNCED) {
            List<Long> invalid = screenings.stream()
                    .filter(s -> s.getState() != ScreeningState.SCHEDULED && s.getState() != ScreeningState.REJECTED)
                    .map(Screening::getScreeningID)
                    .toList();

            if (!invalid.isEmpty()) {
                throw new RuntimeException("Cannot ANNOUNCE: screenings not finalized (must be SCHEDULED or REJECTED). Problem screenings: " + invalid);
            }
        }
    }

    
    private int autoRejectApprovedWithoutFinalSubmission(Long programID) {
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

    @Transactional
    public ProgramDetailsDTO changeState(User requester, Long programID, ProgramState next) {
        forbidAdminDomainOps(requester);

        Program p = programRepository.findById(programID)
                .orElseThrow(() -> new RuntimeException("Program not found"));

        boolean canEdit = isProgramOwner(requester, p) || isProgrammer(programID, requester.getUserID());
        if (!canEdit) throw new RuntimeException("Forbidden");

        ProgramState current = p.getState();
        if (!current.canTransitionTo(next)) {
            throw new RuntimeException("Invalid transition " + current + " -> " + next);
        }

        
        enforceTransitionPreconditions(p, current, next);

        p.setState(next);
        Program saved = programRepository.save(p);

        auditService.log(requester, "PROGRAM_STATE_CHANGE", "PROGRAM", programID,
                "from=" + current + " to=" + next);

        
        if (next == ProgramState.DECISION) {
            autoRejectApprovedWithoutFinalSubmission(programID);
        }

        return toDetailsDTO(saved, requester);
    }

    @Transactional
    public ProgramRole assignRole(Long programID, Long targetUserID, ProgramRoleType role, User requester) {
        forbidAdminDomainOps(requester);

        Program p = programRepository.findById(programID)
                .orElseThrow(() -> new RuntimeException("Program not found"));

        boolean canAssign = isProgramOwner(requester, p) || isProgrammer(programID, requester.getUserID());
        if (!canAssign) throw new RuntimeException("Forbidden");

        if (p.getState() == ProgramState.ANNOUNCED) {
            throw new RuntimeException("Cannot modify roles in ANNOUNCED program");
        }

        if (role == ProgramRoleType.STAFF && p.getState() != ProgramState.CREATED) {
            throw new RuntimeException("STAFF set cannot be modified after SUBMISSION starts");
        }

        User target = userRepository.findById(targetUserID)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        ProgramRoleType conflict = (role == ProgramRoleType.STAFF)
                ? ProgramRoleType.PROGRAMMER
                : ProgramRoleType.STAFF;

        if (programRoleRepository.existsByProgram_ProgramIDAndUser_UserIDAndRole(programID, targetUserID, conflict)) {
            throw new RuntimeException("Conflict: user already has role " + conflict);
        }

        Optional<ProgramRole> existing =
                programRoleRepository.findByProgram_ProgramIDAndUser_UserID(programID, targetUserID);

        ProgramRole pr;
        if (existing.isPresent()) {
            pr = existing.get();
            pr.setRole(role);
        } else {
            pr = new ProgramRole(p, target, role);
        }

        ProgramRole saved = programRoleRepository.save(pr);

        ensureCreatorProgrammer(p);

        auditService.log(requester, "PROGRAM_ROLE_ASSIGNED", "PROGRAM", programID,
                "targetUserID=" + targetUserID + " role=" + role);

        return saved;
    }

    @Transactional(readOnly = true)
    public ProgramDetailsDTO getProgramDetails(User requester, Long programID) {
        Program p = programRepository.findById(programID)
                .orElseThrow(() -> new RuntimeException("Program not found"));
        return toDetailsDTO(p, requester);
    }

    private List<String> getProgrammerUsernames(Long programID) {
        return programRoleRepository.findByProgram_ProgramIDAndRole(programID, ProgramRoleType.PROGRAMMER)
                .stream()
                .map(r -> r.getUser().getUsername())
                .toList();
    }

    private List<String> getStaffUsernames(Long programID) {
        return programRoleRepository.findByProgram_ProgramIDAndRole(programID, ProgramRoleType.STAFF)
            .stream()
            .map(r -> r.getUser().getUsername())
            .toList();
    }


    private ProgramDetailsDTO toDetailsDTO(Program p, User requester) {
        ProgramDetailsDTO dto = new ProgramDetailsDTO();
        dto.setProgramID(p.getProgramID());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setStartDate(p.getStartDate());
        dto.setEndDate(p.getEndDate());
        dto.setProgrammerUsernames(getProgrammerUsernames(p.getProgramID()));
        dto.setStaffUsernames(getStaffUsernames(p.getProgramID()));


        String myRole = "VISITOR";
        if (requester != null && requester.getPermanentRole() == PermanentRole.USER) {
            if (isProgramOwner(requester, p) || isProgrammer(p.getProgramID(), requester.getUserID())) myRole = "PROGRAMMER";
            else if (isStaff(p.getProgramID(), requester.getUserID())) myRole = "STAFF";
            else myRole = "USER";
        }
        dto.setMyRoleInProgram(myRole);

        if ("PROGRAMMER".equals(myRole)) {
            dto.setCreationDate(p.getCreationDate());
            dto.setState(p.getState());
            if (p.getCreator() != null) {
                dto.setCreatorUserID(p.getCreator().getUserID());
                dto.setCreatorUsername(p.getCreator().getUsername());
            }
        } else {
            dto.setCreationDate(null);
            dto.setState(null);
            dto.setCreatorUserID(null);
            dto.setCreatorUsername(null);
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<ProgramSearchResponseDTO> searchPrograms(User requester,
                                                        String q,
                                                        ProgramState state,
                                                        LocalDate startFrom,
                                                        LocalDate startTo) {

        if (requester == null) throw new RuntimeException("Unauthorized");
        if (requester.getPermanentRole() == PermanentRole.ADMIN) {
            throw new RuntimeException("Admins cannot perform domain operations");
        }

        Specification<Program> spec =
                Specification.where(ProgramSpecifications.visibleToUser(requester.getUserID()));

        if (state != null) {
            spec = spec.and(ProgramSpecifications.stateEquals(state));
        }
        if (startFrom != null) {
            spec = spec.and(ProgramSpecifications.startDateFrom(startFrom));
        }
        if (startTo != null) {
            spec = spec.and(ProgramSpecifications.startDateTo(startTo));
        }

        if (q != null && !q.isBlank()) {
            spec = spec.and(ProgramSpecifications.freeTextAny(q));
        }

        Sort sort = Sort.by(Sort.Order.asc("startDate"), Sort.Order.asc("name"));

        List<Program> list = programRepository.findAll(spec, sort);

        return list.stream().map(p -> {
            ProgramSearchResponseDTO dto = ProgramSearchResponseDTO.from(p);

            boolean isProg = isProgramOwner(requester, p) || isProgrammer(p.getProgramID(), requester.getUserID());
            if (!isProg) {
                dto.setState(null);
            }
            return dto;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<ProgramSearchResponseDTO> searchPrograms(
        User requester,
        ProgramState state,
        String name,
        String description,
        LocalDate startFrom,
        LocalDate startTo,
        LocalDate endFrom,
        LocalDate endTo,
        String filmTitle,
        String auditorium
    ) {
        if (requester == null) throw new RuntimeException("Unauthorized");
        if (requester.getPermanentRole() == PermanentRole.ADMIN) {
            throw new RuntimeException("Admins cannot perform domain operations");
        }

        Specification<Program> spec =
            Specification.where(ProgramSpecifications.visibleToUser(requester.getUserID()));

        if (state != null) {
            spec = spec.and(ProgramSpecifications.stateEquals(state));
        }

        if (name != null && !name.isBlank()) {
            spec = spec.and(ProgramSpecifications.nameContains(name));
        }

        if (description != null && !description.isBlank()) {
            spec = spec.and(ProgramSpecifications.descriptionContains(description));
        }

        if (startFrom != null) {
            spec = spec.and(ProgramSpecifications.startDateFrom(startFrom));
        }
        if (startTo != null) {
            spec = spec.and(ProgramSpecifications.startDateTo(startTo));
        }

        if (endFrom != null) {
            spec = spec.and(ProgramSpecifications.endDateFrom(endFrom));
        }
        if (endTo != null) {
            spec = spec.and(ProgramSpecifications.endDateTo(endTo));
        }

        if (filmTitle != null && !filmTitle.isBlank()) {
            spec = spec.and(ProgramSpecifications.existsScreeningWithFilmTitle(filmTitle));
        }

        if (auditorium != null && !auditorium.isBlank()) {
            spec = spec.and(ProgramSpecifications.existsScreeningWithAuditorium(auditorium));
        }

        Sort sort = Sort.by(Sort.Order.asc("startDate"), Sort.Order.asc("name"));
        List<Program> list = programRepository.findAll(spec, sort);

        return list.stream().map(p -> {
            ProgramSearchResponseDTO dto = ProgramSearchResponseDTO.from(p);

            boolean isProg = isProgramOwner(requester, p) || isProgrammer(p.getProgramID(), requester.getUserID());
            if (!isProg) {
                dto.setState(null); // ROLE-BASED REDACTION
            }
            return dto;
        }).toList();
    }
}
