package com.example.backend;

import com.example.backend.entities.*;
import com.example.backend.repository.ProgramRepository;
import com.example.backend.repository.ScreeningRepository;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ScreeningSearchSortTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired ProgramRepository programRepository;
    @Autowired ScreeningRepository screeningRepository;
    @Autowired UserRepository userRepository;

    @Test
    void visitor_search_sorts_by_genre_then_title_and_timetable_by_startTime_and_words_match() throws Exception {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        Program p = new Program("ANNOUNCED_P", "desc",
                LocalDate.now(), LocalDate.now().plusDays(10), admin);
        p.setState(ProgramState.ANNOUNCED);
        p = programRepository.save(p);

        // DEFAULT SORT: GENRE ASC, THEN TITLE ASC
        Screening s1 = new Screening(p, admin, "Zeta", "Someone", "Action", 90);
        s1.setState(ScreeningState.SCHEDULED);
        s1.setAuditoriumName("A1");
        s1.setStartTime(LocalDateTime.parse("2026-01-01T12:00:00"));
        s1.setEndTime(LocalDateTime.parse("2026-01-01T13:40:00"));
        screeningRepository.save(s1);

        Screening s2 = new Screening(p, admin, "Alpha", "Someone", "Action", 90);
        s2.setState(ScreeningState.SCHEDULED);
        s2.setAuditoriumName("A1");
        s2.setStartTime(LocalDateTime.parse("2026-01-01T10:00:00"));
        s2.setEndTime(LocalDateTime.parse("2026-01-01T11:40:00"));
        screeningRepository.save(s2);

        Screening s3 = new Screening(p, admin, "Beta", "Someone", "Comedy", 90);
        s3.setState(ScreeningState.SCHEDULED);
        s3.setAuditoriumName("A2");
        s3.setStartTime(LocalDateTime.parse("2026-01-01T09:00:00"));
        s3.setEndTime(LocalDateTime.parse("2026-01-01T10:40:00"));
        screeningRepository.save(s3);

        // WORDS MATCHING EXAMPLE
        Screening s4 = new Screening(p, admin, "Star Wars", "Mark Hamill Carrie Fisher", "Sci Fi", 120);
        s4.setState(ScreeningState.SCHEDULED);
        s4.setAuditoriumName("A3");
        s4.setStartTime(LocalDateTime.parse("2026-01-02T20:00:00"));
        s4.setEndTime(LocalDateTime.parse("2026-01-02T22:30:00"));
        screeningRepository.save(s4);

        // -------- DEFAULT VIEW (GENRE -> TITLE) AS VISITOR (NO TOKEN) --------
        String json = mvc.perform(get("/screenings/search")
                        .param("programID", String.valueOf(p.getProgramID())))
                .andReturn().getResponse().getContentAsString();

        JsonNode arr = om.readTree(json);
        assertTrue(arr.isArray());

        // EXPECT FIRST 3 IN: ACTION/ALPHA, ACTION/ZETA, COMEDY/BETA (STAR WARS MIGHT APPEAR LATER DUE TO GENRE)
        String g0 = arr.get(0).path("filmGenre").asText();
        String t0 = arr.get(0).path("filmTitle").asText();
        String g1 = arr.get(1).path("filmGenre").asText();
        String t1 = arr.get(1).path("filmTitle").asText();

        assertEquals("Action", g0);
        assertEquals("Alpha", t0);
        assertEquals("Action", g1);
        assertEquals("Zeta", t1);

        // -------- TIMETABLE VIEW (START_TIME) --------
        String json2 = mvc.perform(get("/screenings/search")
                        .param("programID", String.valueOf(p.getProgramID()))
                        .param("view", "timetable"))
                .andReturn().getResponse().getContentAsString();

        JsonNode arr2 = om.readTree(json2);
        assertTrue(arr2.isArray());

        // EARLIEST START IS S3 AT 09:00
        String firstTitle = arr2.get(0).path("filmTitle").asText();
        assertEquals("Beta", firstTitle);

        // -------- WORDS MATCHING: TITLE="STAR WAR" MATCHES "STAR WARS" --------
        String json3 = mvc.perform(get("/screenings/search")
                        .param("programID", String.valueOf(p.getProgramID()))
                        .param("title", "star war"))
                .andReturn().getResponse().getContentAsString();

        JsonNode arr3 = om.readTree(json3);
        assertTrue(arr3.isArray());
        assertEquals(1, arr3.size());
        assertEquals("Star Wars", arr3.get(0).path("filmTitle").asText());
    }
}

