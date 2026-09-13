package com.example.backend.controller;

import com.example.backend.entities.Program;
import com.example.backend.entities.ProgramState;
import com.example.backend.repository.ProgramRepository;
import com.example.backend.spec.ProgramSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/public/programs")
public class PublicProgramController {

    private final ProgramRepository programRepository;

    public PublicProgramController(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }

    //PUBLIC LIST/SEARCH ONLY FOR ANNOUNCED PROGRAMS
    @GetMapping
    public ResponseEntity<?> search(
        @RequestParam(name = "name", required = false) String name,
        @RequestParam(name = "description", required = false) String description,

        //ΚΡΑΤΆΜΕ ΚΑΙ ΤΟ Q (ΠΡΟΑΙΡΕΤΙΚΌ) ΓΙΑ FREE-TEXT ΣΕ NAME/DESCRIPTION/FILMTITLE/AUDITORIUM
        @RequestParam(name = "q", required = false) String q,

        @RequestParam(name = "startFrom", required = false) String startFrom,
        @RequestParam(name = "startTo", required = false) String startTo,
        @RequestParam(name = "endFrom", required = false) String endFrom,
        @RequestParam(name = "endTo", required = false) String endTo
    ) {
        LocalDate sFrom = parseDateOrNull(startFrom);
        LocalDate sTo   = parseDateOrNull(startTo);
        LocalDate eFrom = parseDateOrNull(endFrom);
        LocalDate eTo   = parseDateOrNull(endTo);

        Specification<Program> spec = Specification
            .where(ProgramSpecifications.stateEquals(ProgramState.ANNOUNCED));

        //DATES(AND SEMANTICS)
        if (sFrom != null) spec = spec.and(ProgramSpecifications.startDateFrom(sFrom));
        if (sTo != null)   spec = spec.and(ProgramSpecifications.startDateTo(sTo));
        if (eFrom != null) spec = spec.and(ProgramSpecifications.endDateFrom(eFrom));
        if (eTo != null)   spec = spec.and(ProgramSpecifications.endDateTo(eTo));

        //TEXT FILTERS (AND SEMANTICS ΜΕΤΑΞΎ ΤΟΥΣ)
        if (name != null && !name.isBlank()) {
            spec = spec.and(ProgramSpecifications.nameContains(name));
        }
        if (description != null && !description.isBlank()) {
            spec = spec.and(ProgramSpecifications.descriptionContains(description));
        }

        //ΠΡΟΑΙΡΕΤΙΚΌ FREE-TEXT — AND ΜΕ ΤΑ ΥΠΌΛΟΙΠΑ
        if (q != null && !q.isBlank()) {
            spec = spec.and(ProgramSpecifications.freeTextAny(q));
        }

        Sort sort = Sort.by(Sort.Order.asc("startDate"), Sort.Order.asc("name"));
        List<Program> list = programRepository.findAll(spec, sort);

        return ResponseEntity.ok(list.stream().map(PublicProgramDTO::from).toList());
    }


    //PUBLIC DETAILS ONLY FOR ANNOUNCED
    @GetMapping("/{id}")
    public ResponseEntity<?> details(@PathVariable Long id) {
        Program p = programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Program not found"));

        if (p.getState() != ProgramState.ANNOUNCED) {
            throw new RuntimeException("Forbidden");
        }

        return ResponseEntity.ok(PublicProgramDTO.from(p));
    }

    private static LocalDate parseDateOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return LocalDate.parse(t);
    }

    //PUBLIC DTO (REDACTED)
    public static class PublicProgramDTO {
        private Long programID;
        private String name;
        private String description;
        private LocalDate startDate;
        private LocalDate endDate;

        public static PublicProgramDTO from(Program p) {
            PublicProgramDTO dto = new PublicProgramDTO();
            dto.programID = p.getProgramID();
            dto.name = p.getName();
            dto.description = p.getDescription();
            dto.startDate = p.getStartDate();
            dto.endDate = p.getEndDate();
            return dto;
        }

        public Long getProgramID() { return programID; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
    }
}
