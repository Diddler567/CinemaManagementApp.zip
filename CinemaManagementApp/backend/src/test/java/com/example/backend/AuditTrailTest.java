package com.example.backend;

import com.example.backend.entities.AuditEvent;
import com.example.backend.repository.AuditEventRepository;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditTrailTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository userRepository;
    @Autowired AuditEventRepository auditRepo;

    private TestAuthHelper h;
    private String adminToken;

    @BeforeEach
    void setup() throws Exception {
        h = new TestAuthHelper(mvc, om, userRepository);
        adminToken = h.login("admin", "admin123");
    }

    @Test
    void program_state_change_creates_audit_event_with_context() throws Exception {
        h.register("audit_u", "Audit User", "Password1!");
        Long uid = h.getUserId("audit_u");
        h.adminSetActive(adminToken, uid, true);

        String token = h.login("audit_u", "Password1!");

        String programBody = """
                {"name":"AUDIT_P","description":"desc","startDate":"2026-01-01","endDate":"2026-01-10"}
                """;

        String programJson = mvc.perform(post("/programs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(programBody))
                .andReturn().getResponse().getContentAsString();

        Long programID = om.readTree(programJson).get("programID").asLong();

        mvc.perform(put("/programs/{id}/state", programID)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nextState\":\"SUBMISSION\"}"))
                .andExpect(result -> assertEquals(200, result.getResponse().getStatus()));

        List<AuditEvent> all = auditRepo.findAll();
        assertTrue(all.stream().anyMatch(e ->
                "PROGRAM_STATE_CHANGE".equals(e.getAction())
                        && e.getActorUserId() != null
                        && e.getPath() != null
                        && e.getMethod() != null
        ));
    }
}

