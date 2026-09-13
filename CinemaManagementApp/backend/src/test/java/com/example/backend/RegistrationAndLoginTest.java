package com.example.backend;

import com.example.backend.entities.User;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RegistrationAndLoginTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;

    private final ObjectMapper om = new ObjectMapper();
    private TestAuthHelper h;
    private String adminToken;

    @BeforeEach
    void setup() throws Exception {
        h = new TestAuthHelper(mvc, om, userRepository);

        // SEEDED BY DATALOADER: ADMIN / ADMIN123
        adminToken = h.login("admin", "admin123");
    }

    @Test
    void register_creates_inactive_user_and_login_fails_until_activated() throws Exception {
        h.register("u_inactive", "Inactive User", "Password1!");

        User u = userRepository.findByUsername("u_inactive").orElseThrow();
        assertFalse(u.isActive(), "Newly registered users must be inactive");

        // LOGIN SHOULD FAIL WHILE INACTIVE
        String body = """
                {"username":"u_inactive","password":"Password1!"}
                """;

        mvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    assertTrue(sc >= 400, "Expected failure status, got " + sc);
                });

        // ACTIVATE AND LOGIN OK
        h.adminSetActive(adminToken, u.getUserID(), true);
        String token = h.login("u_inactive", "Password1!");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void three_failed_logins_deactivate_user() throws Exception {
        h.register("u_fail", "Fail User", "Password1!");
        Long id = h.getUserId("u_fail");
        h.adminSetActive(adminToken, id, true);

        // 3 WRONG PASSWORDS
        for (int i = 0; i < 3; i++) {
            String body = """
                    {"username":"u_fail","password":"WRONGPASS"}
                    """;

            mvc.perform(post("/auth/login")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(result -> {
                        int sc = result.getResponse().getStatus();
                        assertTrue(sc >= 400, "Expected failure status, got " + sc);
                    });
        }

        User u = userRepository.findById(id).orElseThrow();
        assertFalse(u.isActive(), "After 3 failed logins user must be deactivated");
    }
}