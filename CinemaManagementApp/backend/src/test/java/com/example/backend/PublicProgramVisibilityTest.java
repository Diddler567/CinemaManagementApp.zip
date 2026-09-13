package com.example.backend;

import com.example.backend.entities.*;
import com.example.backend.repository.ProgramRepository;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PublicProgramVisibilityTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired ProgramRepository programRepository;
    @Autowired UserRepository userRepository;

    @Test
    void public_programs_only_announced() throws Exception {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        Program hidden = new Program("HIDDEN", "hidden", LocalDate.now(), LocalDate.now().plusDays(1), admin);
        hidden.setState(ProgramState.CREATED);
        programRepository.save(hidden);

        Program pub = new Program("PUBLIC", "public", LocalDate.now(), LocalDate.now().plusDays(1), admin);
        pub.setState(ProgramState.ANNOUNCED);
        programRepository.save(pub);

        String json = mvc.perform(get("/public/programs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(json.contains("PUBLIC"));
        assertFalse(json.contains("HIDDEN"));
    }
}

