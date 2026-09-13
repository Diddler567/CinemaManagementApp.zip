package com.example.backend;

import com.example.backend.entities.User;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordChangeSecurityTest {

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
    void three_failed_password_changes_deactivate_user_and_token_invalidated_each_time() throws Exception {
        h.register("u_pw1", "PW User", "Password1!");
        Long id = h.getUserId("u_pw1");
        h.adminSetActive(adminToken, id, true);

        for (int i = 1; i <= 3; i++) {
            String token = h.login("u_pw1", "Password1!");

            String body = """
                    {"oldPassword":"WRONG","newPassword":"Newpass1!","confirmNewPassword":"Newpass1!"}
                    """;

            int sc = mvc.perform(put("/users/me/password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn().getResponse().getStatus();

            assertTrue(sc >= 400, "Expected failure on wrong old password, got " + sc);

            // TOKEN MUST BE INVALID AFTER PASSWORD CHANGE ATTEMPT (EVEN IF FAILED)
            String json = mvc.perform(get("/users/me")
                            .header("Authorization", "Bearer " + token)
                            .accept(MediaType.APPLICATION_JSON))
                    .andReturn().getResponse().getContentAsString();

            int sc2 = mvc.perform(get("/users/me")
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getStatus();

            assertEquals(401, sc2);

            JsonNode root = om.readTree(json);
            String code = root.path("details").path("code").asText();
            assertTrue(code.equals("TOKEN_INVALID") || code.equals("TOKEN_EXPIRED") || code.equals("USER_INACTIVE"),
                    "Unexpected code: " + code);
        }

        User u = userRepository.findById(id).orElseThrow();
        assertFalse(u.isActive(), "After 3 failed password changes, user must be deactivated");

        // LOGIN SHOULD NOW FAIL (INACTIVE)
        String bodyLogin = """
                {"username":"u_pw1","password":"Password1!"}
                """;
        int scLogin = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyLogin))
                .andReturn().getResponse().getStatus();

        assertTrue(scLogin >= 400);
    }
}

