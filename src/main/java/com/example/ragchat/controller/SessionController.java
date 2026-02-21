package com.example.ragchat.controller;

import com.example.ragchat.model.dto.CreateSessionRequest;
import com.example.ragchat.model.dto.PageResponse;
import com.example.ragchat.model.dto.SessionResponse;
import com.example.ragchat.model.dto.UpdateSessionRequest;
import com.example.ragchat.service.SessionService;
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
@RequestMapping("/api/v1/sessions")
@Tag(name = "Sessions", description = "Chat session management")
public class SessionController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Operation(summary = "Create session")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Created"), @ApiResponse(responseCode = "401", description = "Unauthorized") })
    @PostMapping
    public ResponseEntity<SessionResponse> create(
            @RequestHeader(USER_ID_HEADER) String userId,
            @Valid @RequestBody CreateSessionRequest request) {
        SessionResponse created = sessionService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "List sessions", description = "Paginated list; optional filter by favorite. Page/size are optional (default: page=0, size=20).")
    @GetMapping
    public PageResponse<SessionResponse> list(
            @RequestHeader(USER_ID_HEADER) String userId,
            @RequestParam(required = false) Boolean favorite,
            @Parameter(description = "Pagination (optional: page=0, size=20)", required = false) @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(sessionService.listByUser(userId, favorite, pageable));
    }

    @Operation(summary = "Get session by ID")
    @ApiResponses({ @ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404", description = "Session not found") })
    @GetMapping("/{id}")
    public SessionResponse getById(
            @RequestHeader(USER_ID_HEADER) String userId,
            @PathVariable UUID id) {
        return sessionService.getById(userId, id);
    }

    @Operation(summary = "Update session (partial: title and/or favorite)")
    @ApiResponses({ @ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404") })
    @PatchMapping("/{id}")
    public SessionResponse update(
            @RequestHeader(USER_ID_HEADER) String userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSessionRequest request) {
        return sessionService.update(userId, id, request);
    }

    @Operation(summary = "Delete session and all its messages")
    @ApiResponses({ @ApiResponse(responseCode = "204"), @ApiResponse(responseCode = "404") })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader(USER_ID_HEADER) String userId,
            @PathVariable UUID id) {
        sessionService.delete(userId, id);
    }
}
