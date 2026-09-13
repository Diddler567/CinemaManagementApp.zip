package com.example.backend;

import com.example.backend.entities.User;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TokenOwnershipSecurityTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository userRepository;

    private TestAuthHelper h;
    private String adminToken;

    @BeforeEach
    void setup() throws Exception {
        h = new TestAuthHelper(mvc, om, userRepository);
        adminToken = h.login("admin", "admin123");
    }

    @Test
    void user_trying_to_access_other_user_deactivates_both_and_returns_TOKEN_NOT_OWNER() throws Exception {
        h.register("alice", "Alice", "Password1!");
        h.register("bob01", "Bob", "Password1!");

        Long aliceId = h.getUserId("alice");
        Long bobId = h.getUserId("bob01");

        h.adminSetActive(adminToken, aliceId, true);
        h.adminSetActive(adminToken, bobId, true);

        String aliceToken = h.login("alice", "Password1!");

        // ΜΟΝΟ 1 REQUEST (ΤΟ 1Ο ΉΔΗ INVALIDATES TOKEN + DEACTIVATES BOTH)
        String json = mvc.perform(get("/users/{id}", bobId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        assertEquals("TOKEN_NOT_OWNER", om.readTree(json).path("details").path("code").asText());

        User alice = userRepository.findById(aliceId).orElseThrow();
        User bob = userRepository.findById(bobId).orElseThrow();

        assertFalse(alice.isActive(), "Alice must be deactivated after misuse");
        assertFalse(bob.isActive(), "Bob must be deactivated after misuse");
    }
}
