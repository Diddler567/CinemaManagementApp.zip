package com.example.backend;

import com.example.backend.entities.*;
import com.example.backend.repository.ProgramRepository;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProgramSearchVisibilityTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository userRepository;
    @Autowired ProgramRepository programRepository;
    @Autowired BCryptPasswordEncoder encoder;

    private TestAuthHelper h;
    private String adminToken;

    @BeforeEach
    void setup() throws Exception {
        h = new TestAuthHelper(mvc, om, userRepository);
        adminToken = h.login("admin", "admin123");
    }

    private User createActiveUser(String username) throws Exception {
        h.register(username, username.toUpperCase(), "Password1!");
        Long id = h.getUserId(username);
        h.adminSetActive(adminToken, id, true);
        return userRepository.findById(id).orElseThrow();
    }

    @Test
    void search_returns_only_visible_programs() throws Exception {
        User owner = createActiveUser("owner1");
        User outsider = createActiveUser("outsider1");

        Program p1 = new Program("P1", "Hidden program", LocalDate.now().plusDays(10), LocalDate.now().plusDays(11), owner);
        p1.setState(ProgramState.CREATED); // NOT ANNOUNCED
        programRepository.save(p1);

        Program p2 = new Program("P2", "Public program", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), owner);
        p2.setState(ProgramState.ANNOUNCED);
        programRepository.save(p2);

        String outsiderToken = h.login("outsider1", "Password1!");

        // OUTSIDER SEARCHES PROGRAMS
        String json = mvc.perform(get("/programs")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // SHOULD SEE P2 BUT NOT P1
        assertTrue(json.contains("P2"));
        assertFalse(json.contains("P1"));
    }
}

