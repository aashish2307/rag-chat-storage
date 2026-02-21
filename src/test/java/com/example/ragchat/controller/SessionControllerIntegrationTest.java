package com.example.ragchat.controller;

import com.example.ragchat.model.dto.CreateSessionRequest;
import com.example.ragchat.model.dto.UpdateSessionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionControllerIntegrationTest {

    private static final String API_KEY = "test-api-key";
    private static final String USER_ID = "test-user";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createSession_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/sessions")
                        .header("X-API-Key", API_KEY)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSessionRequest("My Chat"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("My Chat"))
                .andExpect(jsonPath("$.favorite").value(false))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createSession_withoutApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/sessions")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSessionRequest("x"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listSessions_returnsPaginated() throws Exception {
        mockMvc.perform(get("/api/v1/sessions")
                        .header("X-API-Key", API_KEY)
                        .header("X-User-Id", USER_ID)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void getSession_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/00000000-0000-0000-0000-000000000000")
                        .header("X-API-Key", API_KEY)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateSession_partialUpdate() throws Exception {
        // Create first
        String createResp = mockMvc.perform(post("/api/v1/sessions")
                        .header("X-API-Key", API_KEY)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSessionRequest("Original"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createResp).get("id").asText();

        mockMvc.perform(patch("/api/v1/sessions/" + id)
                        .header("X-API-Key", API_KEY)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateSessionRequest("Renamed", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.favorite").value(true));
    }

    @Test
    void deleteSession_returns204() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/sessions")
                        .header("X-API-Key", API_KEY)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSessionRequest("To delete"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createResp).get("id").asText();

        mockMvc.perform(delete("/api/v1/sessions/" + id)
                        .header("X-API-Key", API_KEY)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNoContent());
    }
}
