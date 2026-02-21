package com.example.ragchat.controller;

import com.example.ragchat.model.dto.AddMessageRequest;
import com.example.ragchat.model.dto.MessageResponse;
import com.example.ragchat.model.dto.PageResponse;
import com.example.ragchat.service.MessageService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/messages")
@Tag(name = "Messages", description = "Chat messages within a session")
public class MessageController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @Operation(summary = "Add message to session")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Created"), @ApiResponse(responseCode = "404", description = "Session not found") })
    @PostMapping
    public ResponseEntity<MessageResponse> add(
            @RequestHeader(USER_ID_HEADER) String userId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AddMessageRequest request) {
        MessageResponse created = messageService.add(userId, sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "List messages (paginated)", description = "Page/size optional (default: page=0, size=20).")
    @ApiResponses({ @ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", description = "Session not found") })
    @GetMapping
    public PageResponse<MessageResponse> list(
            @RequestHeader(USER_ID_HEADER) String userId,
            @PathVariable UUID sessionId,
            @Parameter(description = "Pagination (optional)", required = false) @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(messageService.getBySession(userId, sessionId, pageable));
    }
}
