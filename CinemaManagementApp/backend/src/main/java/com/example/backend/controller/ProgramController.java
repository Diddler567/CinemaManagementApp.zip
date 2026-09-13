package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.entities.ProgramRole;
import com.example.backend.entities.ProgramRoleType;
import com.example.backend.entities.ProgramState;
import com.example.backend.entities.User;
import com.example.backend.security.AuthUtils;
import com.example.backend.service.ProgramService;
import com.example.backend.service.ScreeningService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/programs")
public class ProgramController {

    private final ProgramService programService;
    private final ScreeningService screeningService;

    public ProgramController(ProgramService programService, ScreeningService screeningService) {
        this.programService = programService;
        this.screeningService = screeningService;
    }

    //CREATE
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateProgramRequest req,
                                    HttpServletRequest request) {
        User me = AuthUtils.requireAuthUser(request);
        ProgramDetailsDTO dto = programService.createProgram(me, req);
        return ResponseEntity.ok(dto);
    }

    //UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateProgramRequest req,
                                    HttpServletRequest request) {
        User me = AuthUtils.requireAuthUser(request);
        ProgramDetailsDTO dto = programService.updateProgram(me, id, req);
        return ResponseEntity.ok(dto);
    }

    //DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    HttpServletRequest request) {
        User me = AuthUtils.requireAuthUser(request);
        programService.deleteProgram(me, id);
        return ResponseEntity.ok(new SimpleMessageResponse("Program deleted."));
    }

    //STATE TRANSITION
    @PutMapping("/{id}/state")
    public ResponseEntity<?> changeState(@PathVariable Long id,
                                        @Valid @RequestBody ChangeProgramStateRequest req,
                                        HttpServletRequest request) {
        User me = AuthUtils.requireAuthUser(request);

        ProgramState next = ProgramState.fromString(req.getNextState());


        ProgramDetailsDTO dto = programService.changeState(me, id, next);
        return ResponseEntity.ok(dto);
    }

    //DETAILS
    @GetMapping("/{id}")
    public ResponseEntity<?> details(@PathVariable Long id,
                                    HttpServletRequest request) {
        User me = AuthUtils.requireAuthUser(request);
        ProgramDetailsDTO dto = programService.getProgramDetails(me, id);
        return ResponseEntity.ok(dto);
    }

    //SEARCH
    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "description", required = false) String description,
            @RequestParam(name = "filmTitle", required = false) String filmTitle,
            @RequestParam(name = "auditorium", required = false) String auditorium,

            @RequestParam(name = "state", required = false) String state,

            @RequestParam(name = "startFrom", required = false) String startFrom,
            @RequestParam(name = "startTo", required = false) String startTo,
            @RequestParam(name = "endFrom", required = false) String endFrom,
            @RequestParam(name = "endTo", required = false) String endTo,

            HttpServletRequest request
        ) {
        User me = AuthUtils.requireAuthUser(request);

        ProgramState st = null;
        if (state != null && !state.isBlank()) {
            st = ProgramState.fromString(state);
        }

        LocalDate sFrom = (startFrom == null || startFrom.isBlank()) ? null : LocalDate.parse(startFrom.trim());
        LocalDate sTo   = (startTo == null || startTo.isBlank()) ? null : LocalDate.parse(startTo.trim());
        LocalDate eFrom = (endFrom == null || endFrom.isBlank()) ? null : LocalDate.parse(endFrom.trim());
        LocalDate eTo   = (endTo == null || endTo.isBlank()) ? null : LocalDate.parse(endTo.trim());

        List<ProgramSearchResponseDTO> out = programService.searchPrograms(
            me,
            st,
            name,
            description,
            sFrom,
            sTo,
            eFrom,
            eTo,
            filmTitle,
            auditorium
        );

        return ResponseEntity.ok(out);
    }


    //ASSIGN ROLE
    @PostMapping("/{id}/roles")
    public ResponseEntity<?> assignRole(@PathVariable Long id,
                                        @Valid @RequestBody AssignRoleRequest body,
                                        HttpServletRequest request) {

        User me = AuthUtils.requireAuthUser(request);

        ProgramRoleType role = ProgramRoleType.valueOf(body.getRole().trim().toUpperCase());
        ProgramRole pr = programService.assignRole(id, body.getUserID(), role, me);

        return ResponseEntity.ok(new SimpleMessageResponse("Role set to: " + pr.getRole().name()));
    }
}