package com.example.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RegistrationRateLimitTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    void register_rate_limit_triggers_429_on_4th_request() throws Exception {
        for (int i = 1; i <= 3; i++) {
            String body = """
                    {"username":"rluser%d","fullname":"RL","password":"Password1!","confirmPassword":"Password1!"}
                    """.formatted(i);

            int sc = mvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn().getResponse().getStatus();

            assertEquals(200, sc);
        }

        String body4 = """
                {"username":"rluser4","fullname":"RL","password":"Password1!","confirmPassword":"Password1!"}
                """;

        var res = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body4))
                .andReturn().getResponse();

        assertEquals(429, res.getStatus());
        assertNotNull(res.getHeader("Retry-After"));
        assertTrue(res.getContentAsString().contains("Rate limit exceeded"));
    }
}
