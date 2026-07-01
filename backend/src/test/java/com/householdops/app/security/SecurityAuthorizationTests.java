package com.householdops.app.security;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.householdops.app.staff.StaffMemberRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real JWT filter chain end-to-end via MockMvc, against the
 * seeded demo household -- this is the single highest-leverage test class
 * for the security story: it proves role/household enforcement actually
 * works at the HTTP layer, not just that the annotations compile.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffMemberRepository staffMemberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID householdId;

    @BeforeEach
    void setUp() {
        householdId = staffMemberRepository.findByEmailIgnoreCase("owner@householdops.dev")
                .orElseThrow(() -> new IllegalStateException("Demo seed data not present -- run the app once to seed it"))
                .getHousehold().getId();
    }

    @Test
    void requestWithNoTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/households/{id}/tasks", householdId))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"owner@householdops.dev\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenGrantsAccessToOwnHousehold() throws Exception {
        String token = login("owner@householdops.dev");

        mockMvc.perform(get("/api/households/{id}/tasks", householdId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tokenDoesNotGrantAccessToAnotherHousehold() throws Exception {
        String token = login("owner@householdops.dev");
        UUID someoneElsesHouseholdId = UUID.randomUUID();

        mockMvc.perform(get("/api/households/{id}/tasks", someoneElsesHouseholdId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCannotAddNewStaff() throws Exception {
        String token = login("staff@householdops.dev");

        mockMvc.perform(post("/api/households/{id}/staff", householdId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newStaffJson("staff-blocked-" + UUID.randomUUID() + "@householdops.dev")))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanAddNewStaff() throws Exception {
        String token = login("owner@householdops.dev");

        mockMvc.perform(post("/api/households/{id}/staff", householdId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newStaffJson("staff-added-" + UUID.randomUUID() + "@householdops.dev")))
                .andExpect(status().isOk());
    }

    private String newStaffJson(String email) {
        return "{\"fullName\":\"Test Staff\",\"email\":\"" + email + "\",\"password\":\"password123\",\"role\":\"STAFF\"}";
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}
