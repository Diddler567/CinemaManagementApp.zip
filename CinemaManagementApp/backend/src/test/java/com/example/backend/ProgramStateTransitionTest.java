package com.example.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProgramStateTransitionTest {

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
    void program_state_transition_preconditions_are_enforced() throws Exception {
        // REGISTER 3 USERS (REGISTER RATE LIMIT IS 3/MIN → KEEP EXACTLY 3)
        h.register("prog1", "Programmer One", "Password1!");
        h.register("staff1", "Staff One", "Password1!");
        h.register("sub01", "Submitter One", "Password1!");

        Long progId = h.getUserId("prog1");
        Long staffId = h.getUserId("staff1");
        Long subId = h.getUserId("sub01");

        h.adminSetActive(adminToken, progId, true);
        h.adminSetActive(adminToken, staffId, true);
        h.adminSetActive(adminToken, subId, true);

        String progToken = h.login("prog1", "Password1!");
        String staffToken = h.login("staff1", "Password1!");
        String subToken = h.login("sub01", "Password1!");

        // CREATE PROGRAM (CREATED)
        String programBody = """
                {"name":"P_PRECOND","description":"desc","startDate":"2026-01-01","endDate":"2026-01-10"}
                """;

        String programJson = mvc.perform(post("/programs")
                        .header("Authorization", "Bearer " + progToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(programBody))
                .andReturn().getResponse().getContentAsString();

        Long programID = om.readTree(programJson).get("programID").asLong();

        // ADD STAFF WHILE CREATED
        String roleBody = """
                {"userID": %d, "role":"STAFF"}
                """.formatted(staffId);

        mvc.perform(post("/programs/{id}/roles", programID)
                        .header("Authorization", "Bearer " + progToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleBody))
                .andExpect(result -> assertEquals(200, result.getResponse().getStatus()));

        // MOVE TO SUBMISSION
        mvc.perform(put("/programs/{id}/state", programID)
                        .header("Authorization", "Bearer " + progToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nextState\":\"SUBMISSION\"}"))
                .andExpect(result -> assertEquals(200, result.getResponse().getStatus()));

        // SUBMITTER CREATES SCREENING DRAFT (CREATED)
        String sBody = """
                {
                  "programID": %d,
                  "filmTitle": "Film A",
                  "filmCast": "Cast A",
                  "filmGenre": "Drama",
                  "filmDuration": 90,
                  "auditoriumName": "A1",
                  "startTime": "2026-01-02T10:00:00",
                  "endTime": "2026-01-02T11:40:00"
                }
                """.formatted(programID);

        String sCreateJson = mvc.perform(post("/screenings")
                        .header("Authorization", "Bearer " + subToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sBody))
                .andReturn().getResponse().getContentAsString();

        Long screeningID = om.readTree(sCreateJson).get("screeningID").asLong();

        // SUBMIT (CREATED -> SUBMITTED)
        mvc.perform(post("/screenings/{id}/submit", screeningID)
                        .header("Authorization", "Bearer " + subToken))
                .andExpect(result -> assertEquals(200, result.getResponse().getStatus()));

        // MOVE PROGRAM TO ASSIGNMENT
        mvc.perform(put("/programs/{id}/state", programID)
                        .header("Authorization", "Bearer " + progToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nextState\":\"ASSIGNMENT\"}"))
                .andExpect(result -> assertEquals(200, result.getResponse().getStatus()));

        // ASSIGNMENT -> REVIEW SHOULD FAIL (HANDLER MISSING)
        int scFail = mvc.perform(put("/programs/{id}/state", programID)
                        .header("Authorization", "Bearer " + progToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nextState\":\"REVIEW\"}"))
                .andReturn().getResponse().getStatus();
        assertTrue(scFail >= 400);

        // ASSIGN HANDLER (PROGRAMMER)
        mvc.perform(post("/screenings/{id}/assign-handler", screeningID)
                        .header("Authorization", "Bearer " + progToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staffUserID\":" + staffId + "}"))
                .andExpect(result -> assertEquals(200, result.getResponse().getStatus()));

        // ASSIGNMENT -> REVIEW SHOULD SUCCEED NOW
        mvc.perform(put("/programs/{id}/state", programID)
                        .header("Authorization", "Bearer " + progToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nextState\":\"REVIEW\"}"))
                .andExpect(result -> assertEquals(200, result.getResponse().getStatus()));

        // REVIEW -> SCHEDULING SHOULD FAIL (STILL SUBMITTED, NOT REVIEWED)
        int scFail2 = mvc.perform(put("/programs/{id}/state", programID)
                        .header("Authorization", "Bearer " + progToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nextState\":\"SCHEDULING\"}"))
                .andReturn().getResponse().getStatus();
        assertTrue(scFail2 >= 400);

        // STAFF ADDS REVIEW (SUBMITTED -> REVIEWED)
        mvc.perform(post("/screenings/{id}/reviews", screeningID)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":8,\"comments\":\"ok\"}"))
                .andExpect(result -> assertEquals(200, result.getResponse().getStatus()));

        // REVIEW -> SCHEDULING SHOULD SUCCEED NOW
        mvc.perform(put("/programs/{id}/state", programID)
                        .header("Authorization", "Bearer " + progToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nextState\":\"SCHEDULING\"}"))
                .andExpect(result -> assertEquals(200, result.getResponse().getStatus()));

        // SCHEDULING -> FINAL_PUBLICATION SHOULD FAIL (REVIEWED UNDECIDED BY SUBMITTER)
        int scFail3 = mvc.perform(put("/programs/{id}/state", programID)
                        .header("Authorization", "Bearer " + progToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nextState\":\"FINAL_SUBMISSION\"}")) // ALLOW PDF ALIAS
                .andReturn().getResponse().getStatus();
        assertTrue(scFail3 >= 400);

        // SUBMITTER APPROVES IN SCHEDULING (REVIEWED -> APPROVED)
        mvc.perform(post("/screenings/{id}/scheduling/approve", screeningID)
                        .header("Authorization", "Bearer " + subToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvalNotes\":\"ok\"}"))
                .andExpect(result -> assertEquals(200, result.getResponse().getStatus()));

        // NOW SCHEDULING -> FINAL_PUBLICATION SHOULD SUCCEED
        String okJson = mvc.perform(put("/programs/{id}/state", programID)
                        .header("Authorization", "Bearer " + progToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nextState\":\"FINAL_SUBMISSION\"}"))
                .andReturn().getResponse().getContentAsString();

        JsonNode ok = om.readTree(okJson);
        assertTrue(ok.toString().contains("FINAL_PUBLICATION"));
    }
}

