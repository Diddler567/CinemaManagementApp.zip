package com.example.backend;

import com.example.backend.entities.User;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TestAuthHelper {

    private static final AtomicInteger IP_SEQ = new AtomicInteger(10);

    private final MockMvc mvc;
    private final ObjectMapper om;
    private final UserRepository userRepository;

    public TestAuthHelper(MockMvc mvc, ObjectMapper om, UserRepository userRepository) {
        this.mvc = mvc;
        this.om = om;
        this.userRepository = userRepository;
    }

    private String nextTestIp() {
        // UNIQUE-ISH IP PER HELPER CALL -> AVOIDS RATE LIMIT COLLISIONS ACROSS TESTS
        int n = IP_SEQ.getAndIncrement();
        return "10.0.0." + n;
    }

    public String login(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);

        String json = mvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", nextTestIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return om.readTree(json).get("token").asText();

    }

    public void register(String username, String fullname, String password) throws Exception {
        String body = """
                {"username":"%s","fullname":"%s","password":"%s","confirmPassword":"%s"}
                """.formatted(username, fullname, password, password);

        mvc.perform(post("/auth/register")
                        .header("X-Forwarded-For", nextTestIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    public void registerRaw(String username, String fullname, String password, String confirmPassword, int expectedStatusAtLeast) throws Exception {
        String body = """
                {"username":"%s","fullname":"%s","password":"%s","confirmPassword":"%s"}
                """.formatted(username, fullname, password, confirmPassword);

        mvc.perform(post("/auth/register")
                        .header("X-Forwarded-For", nextTestIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    if (sc < expectedStatusAtLeast) {
                        throw new AssertionError("Expected status >= " + expectedStatusAtLeast + " but got " + sc);
                    }
                });
    }

    public Long getUserId(String username) {
        User u = userRepository.findByUsername(username).orElseThrow();
        return u.getUserID();
    }

    public void adminSetActive(String adminToken, Long userId, boolean active) throws Exception {
        String body = """
                {"active": %s}
                """.formatted(active);

        mvc.perform(put("/users/{id}/status", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
