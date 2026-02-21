package com.example.ragchat.controller;

import com.example.ragchat.model.dto.AddMessageRequest;
import com.example.ragchat.model.entity.MessageSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MessageControllerIntegrationTest {

    private static final String API_KEY = "test-api-key";
    private static final String USER_ID = "test-user";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addMessage_sessionNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/00000000-0000-0000-0000-000000000000/messages")
                        .header("X-API-Key", API_KEY)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddMessageRequest(MessageSender.user, "Hi", null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void addAndListMessages() throws Exception {
        // Create session
        String createResp = mockMvc.perform(post("/api/v1/sessions")
                        .header("X-API-Key", API_KEY)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.example.ragchat.model.dto.CreateSessionRequest("Chat"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(createResp).get("id").asText();

        // Add message
        mockMvc.perform(post("/api/v1/sessions/" + sessionId + "/messages")
                        .header("X-API-Key", API_KEY)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddMessageRequest(MessageSender.user, "Hello", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Hello"))
                .andExpect(jsonPath("$.sender").value("user"));

        // List messages (paginated)
        mockMvc.perform(get("/api/v1/sessions/" + sessionId + "/messages")
                        .header("X-API-Key", API_KEY)
                        .header("X-User-Id", USER_ID)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Hello"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void addMessage_validationError_returns400() throws Exception {
        String createResp = mockMvc.perform(post("/api/v1/sessions")
                        .header("X-API-Key", API_KEY)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(createResp).get("id").asText();

        // Missing required sender and content
        mockMvc.perform(post("/api/v1/sessions/" + sessionId + "/messages")
                        .header("X-API-Key", API_KEY)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
