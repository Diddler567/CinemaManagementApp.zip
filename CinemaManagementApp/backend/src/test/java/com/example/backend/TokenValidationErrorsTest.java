package com.example.backend;

import com.example.backend.entities.AuthenticationToken;
import com.example.backend.entities.User;
import com.example.backend.repository.AuthenticationTokenRepository;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TokenValidationErrorsTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository userRepository;
    @Autowired AuthenticationTokenRepository tokenRepository;

    private TestAuthHelper h;
    private String adminToken;

    @BeforeEach
    void setup() throws Exception {
        h = new TestAuthHelper(mvc, om, userRepository);
        adminToken = h.login("admin", "admin123");
    }

    @Test
    void missing_token_returns_TOKEN_MISSING() throws Exception {
        String json = mvc.perform(get("/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        int sc = mvc.perform(get("/users/me")).andReturn().getResponse().getStatus();
        assertEquals(401, sc);

        JsonNode root = om.readTree(json);
        assertEquals("TOKEN_MISSING", root.path("details").path("code").asText());
    }

    @Test
    void invalid_token_returns_TOKEN_INVALID() throws Exception {
        String json = mvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + "not-a-real-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        int sc = mvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + "not-a-real-token"))
                .andReturn().getResponse().getStatus();
        assertEquals(401, sc);

        JsonNode root = om.readTree(json);
        assertEquals("TOKEN_INVALID", root.path("details").path("code").asText());
    }

    @Test
    void expired_token_returns_TOKEN_EXPIRED_and_is_invalidated() throws Exception {
        // CREATE & ACTIVATE USER
        h.register("u_exp", "Expired Token User", "Password1!");
        Long uid = h.getUserId("u_exp");
        h.adminSetActive(adminToken, uid, true);

        User u = userRepository.findById(uid).orElseThrow();

        // INSERT AN ALREADY-EXPIRED TOKEN (STILL VALID=TRUE)
        AuthenticationToken t = new AuthenticationToken("expired-token-value", u, Instant.now().minusSeconds(60));
        t.setValid(true);
        tokenRepository.save(t);

        String json = mvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + "expired-token-value")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        int sc = mvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + "expired-token-value"))
                .andReturn().getResponse().getStatus();
        assertEquals(401, sc);

        JsonNode root = om.readTree(json);
        assertEquals("TOKEN_EXPIRED", root.path("details").path("code").asText());

        // TOKEN MUST BE INVALIDATED
        assertTrue(tokenRepository.findByTokenValueAndIsValidTrue("expired-token-value").isEmpty());
    }
}

