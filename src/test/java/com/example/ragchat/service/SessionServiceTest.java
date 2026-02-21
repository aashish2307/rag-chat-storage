package com.example.ragchat.service;

import com.example.ragchat.config.SessionListCacheEvictor;
import com.example.ragchat.exception.ResourceNotFoundException;
import com.example.ragchat.model.dto.CreateSessionRequest;
import com.example.ragchat.model.dto.SessionResponse;
import com.example.ragchat.model.dto.UpdateSessionRequest;
import com.example.ragchat.model.entity.Session;
import com.example.ragchat.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private SessionListCacheEvictor cacheEvictor;
    @InjectMocks
    private SessionService sessionService;

    private static final String USER_ID = "user-1";

    @Test
    void create_setsTitleAndUserId() {
        CreateSessionRequest request = new CreateSessionRequest("My Chat");
        Session saved = new Session();
        saved.setId(UUID.randomUUID());
        saved.setUserId(USER_ID);
        saved.setTitle("My Chat");
        saved.setFavorite(false);
        saved.setCreatedAt(Instant.now());
        saved.setUpdatedAt(Instant.now());
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> {
            Session s = inv.getArgument(0);
            s.setId(saved.getId());
            s.setCreatedAt(saved.getCreatedAt());
            s.setUpdatedAt(saved.getUpdatedAt());
            return s;
        });

        SessionResponse response = sessionService.create(USER_ID, request);

        assertThat(response.title()).isEqualTo("My Chat");
        assertThat(response.favorite()).isFalse();
        verify(sessionRepository).save(argThat(s -> USER_ID.equals(s.getUserId()) && "My Chat".equals(s.getTitle())));
        verify(cacheEvictor).evictForUser(USER_ID);
    }

    @Test
    void create_usesDefaultTitleWhenBlank() {
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> {
            Session s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setCreatedAt(Instant.now());
            s.setUpdatedAt(Instant.now());
            return s;
        });

        sessionService.create(USER_ID, new CreateSessionRequest(null));

        verify(sessionRepository).save(argThat(s -> "New Chat".equals(s.getTitle())));
    }

    @Test
    void getById_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(sessionRepository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getById(USER_ID, id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session");
    }

    @Test
    void update_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(sessionRepository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.update(USER_ID, id, new UpdateSessionRequest("New", true)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(sessionRepository.existsByIdAndUserId(id, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> sessionService.delete(USER_ID, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_evictsCache() {
        UUID id = UUID.randomUUID();
        when(sessionRepository.existsByIdAndUserId(id, USER_ID)).thenReturn(true);

        sessionService.delete(USER_ID, id);

        verify(sessionRepository).deleteById(id);
        verify(cacheEvictor).evictForUser(USER_ID);
    }

    @Test
    void listByUser_returnsPageFromRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Session session = new Session();
        session.setId(UUID.randomUUID());
        session.setUserId(USER_ID);
        session.setTitle("Chat");
        session.setFavorite(false);
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        when(sessionRepository.findByUserId(eq(USER_ID), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(session), pageable, 1));

        Page<SessionResponse> page = sessionService.listByUser(USER_ID, null, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).title()).isEqualTo("Chat");
        assertThat(page.getTotalElements()).isEqualTo(1);
        verify(sessionRepository).findByUserId(USER_ID, pageable);
    }

    @Test
    void listByUser_withFavorite_callsFindByUserIdAndFavorite() {
        Pageable pageable = PageRequest.of(0, 10);
        when(sessionRepository.findByUserIdAndFavorite(eq(USER_ID), eq(true), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        sessionService.listByUser(USER_ID, true, pageable);

        verify(sessionRepository).findByUserIdAndFavorite(USER_ID, true, pageable);
    }

    @Test
    void getById_returnsSessionWhenFound() {
        UUID id = UUID.randomUUID();
        Session session = new Session();
        session.setId(id);
        session.setUserId(USER_ID);
        session.setTitle("Found");
        session.setFavorite(true);
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        when(sessionRepository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.of(session));

        SessionResponse response = sessionService.getById(USER_ID, id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.title()).isEqualTo("Found");
        assertThat(response.favorite()).isTrue();
    }

    @Test
    void update_updatesTitleAndFavoriteAndEvictsCache() {
        UUID id = UUID.randomUUID();
        Session session = new Session();
        session.setId(id);
        session.setUserId(USER_ID);
        session.setTitle("Old");
        session.setFavorite(false);
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        when(sessionRepository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateSessionRequest request = new UpdateSessionRequest("New Title", true);

        SessionResponse response = sessionService.update(USER_ID, id, request);

        assertThat(response.title()).isEqualTo("New Title");
        assertThat(response.favorite()).isTrue();
        verify(sessionRepository).save(argThat(s -> "New Title".equals(s.getTitle()) && s.isFavorite()));
        verify(cacheEvictor).evictForUser(USER_ID);
    }

    @Test
    void update_ignoresNullTitle() {
        UUID id = UUID.randomUUID();
        Session session = new Session();
        session.setId(id);
        session.setUserId(USER_ID);
        session.setTitle("Keep");
        session.setFavorite(false);
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        when(sessionRepository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

        sessionService.update(USER_ID, id, new UpdateSessionRequest(null, true));

        verify(sessionRepository).save(argThat(s -> "Keep".equals(s.getTitle()) && s.isFavorite()));
    }
}
