package com.householdops.app.security;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.householdops.app.staff.StaffMemberRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
    private UUID miamiHouseholdId;

    @BeforeEach
    void setUp() {
        householdId = staffMemberRepository.findByEmailIgnoreCase("owner@householdops.dev")
                .orElseThrow(() -> new IllegalStateException("Demo seed data not present -- run the app once to seed it"))
                .getHousehold().getId();
        miamiHouseholdId = staffMemberRepository.findByEmailIgnoreCase("manager-miami@householdops.dev")
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

    @Test
    void staffCannotCreateVendor() throws Exception {
        String token = login("staff@householdops.dev");

        mockMvc.perform(post("/api/households/{id}/vendors", householdId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Blocked Vendor Co.\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanCreateVendor() throws Exception {
        String token = login("owner@householdops.dev");

        mockMvc.perform(post("/api/households/{id}/vendors", householdId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Vendor " + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void staffCannotImportInventoryCsv() throws Exception {
        String token = login("staff@householdops.dev");
        MockMultipartFile file = new MockMultipartFile(
                "file", "inventory.csv", "text/csv",
                "name,category,unit,currentQuantity,reorderThreshold,reorderQuantity\n".getBytes());

        mockMvc.perform(multipart("/api/households/{id}/inventory/import", householdId)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanImportInventoryCsv() throws Exception {
        String token = login("owner@householdops.dev");
        MockMultipartFile file = new MockMultipartFile(
                "file", "inventory.csv", "text/csv",
                "name,category,unit,currentQuantity,reorderThreshold,reorderQuantity\n".getBytes());

        mockMvc.perform(multipart("/api/households/{id}/inventory/import", householdId)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void staffCannotViewPortfolio() throws Exception {
        String token = login("staff@householdops.dev");

        mockMvc.perform(get("/api/portfolio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanViewPortfolioAcrossGrantedHouseholds() throws Exception {
        String token = login("owner@householdops.dev");

        String body = mockMvc.perform(get("/api/portfolio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body).contains("Aspen House", "Miami Beach Villa");
    }

    @Test
    void staffCannotSwitchHousehold() throws Exception {
        String token = login("staff@householdops.dev");

        mockMvc.perform(post("/api/auth/switch-household")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"householdId\":\"" + miamiHouseholdId + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCannotSwitchToUngrantedHousehold() throws Exception {
        String token = login("owner@householdops.dev");
        UUID ungrantedHouseholdId = UUID.randomUUID();

        mockMvc.perform(post("/api/auth/switch-household")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"householdId\":\"" + ungrantedHouseholdId + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanSwitchToGrantedHouseholdAndActOnIt() throws Exception {
        String token = login("owner@householdops.dev");

        // Before switching, the Aspen-scoped token can't touch Miami's tasks.
        mockMvc.perform(get("/api/households/{id}/tasks", miamiHouseholdId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        String switchBody = mockMvc.perform(post("/api/auth/switch-household")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"householdId\":\"" + miamiHouseholdId + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String switchedToken = objectMapper.readTree(switchBody).get("accessToken").asText();
        org.assertj.core.api.Assertions.assertThat(objectMapper.readTree(switchBody).get("householdId").asText())
                .isEqualTo(miamiHouseholdId.toString());

        // After switching, the new token grants access to Miami and no longer to Aspen.
        mockMvc.perform(get("/api/households/{id}/tasks", miamiHouseholdId)
                        .header("Authorization", "Bearer " + switchedToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/households/{id}/tasks", householdId)
                        .header("Authorization", "Bearer " + switchedToken))
                .andExpect(status().isForbidden());
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
