package com.example.backend.controller;

import com.example.backend.dto.ScreeningViewDTO;
import com.example.backend.service.ScreeningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/public/screenings")
public class PublicScreeningController {

    private final ScreeningService screeningService;

    public PublicScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    //PUBLIC VIEW BY SCREENINGID:
    // ALLOWED ONLY IF PROGRAM ANNOUNCED AND SCREENING SCHEDULED (ENFORCED IN SERVICE)
    @GetMapping("/{id}")
    public ResponseEntity<?> view(@PathVariable Long id) {
        ScreeningViewDTO dto = screeningService.viewScreening(null, id);
        return ResponseEntity.ok(dto);
    }

    //PUBLIC SEARCH WITHIN AN ANNOUNCED PROGRAM:
    //RETURNS SCHEDULED-ONLY, REDACTED (SERVICE ENFORCES)
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam("programID") Long programID,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "genre", required = false) String genre,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "view", required = false) String view
        ) {
        boolean timetableView = view != null && view.equalsIgnoreCase("timetable");

        LocalDateTime fromDt = (from == null || from.isBlank()) ? null : LocalDateTime.parse(from.trim());
        LocalDateTime toDt = (to == null || to.isBlank()) ? null : LocalDateTime.parse(to.trim());

        //CAST ΦΊΛΤΡΟ ΔΕΝ ΤΟ ΔΊΝΩ ΣΤΟ PUBLIC (ΣΥΝΉΘΩΣ ΔΕΝ ΧΡΕΙΆΖΕΤΑΙ), ΑΛΛΆ ΑΝ ΘΕΣ ΤΟ ΑΝΟΊΓΟΥΜΕ.
        List<ScreeningViewDTO> out = screeningService.searchScreeningsView(
            null, programID, title, null, genre, fromDt, toDt, timetableView
        );


        return ResponseEntity.ok(out);
    }
}
