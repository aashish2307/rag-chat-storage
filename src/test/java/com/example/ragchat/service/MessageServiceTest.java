package com.example.ragchat.service;

import com.example.ragchat.exception.ResourceNotFoundException;
import com.example.ragchat.model.dto.AddMessageRequest;
import com.example.ragchat.model.dto.MessageResponse;
import com.example.ragchat.model.entity.Message;
import com.example.ragchat.model.entity.MessageSender;
import com.example.ragchat.model.entity.Session;
import com.example.ragchat.repository.MessageRepository;
import com.example.ragchat.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private SessionRepository sessionRepository;
    @InjectMocks
    private MessageService messageService;

    private static final String USER_ID = "user-1";
    private static final UUID SESSION_ID = UUID.randomUUID();

    @Test
    void add_throwsWhenSessionNotFound() {
        when(sessionRepository.findByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(Optional.empty());
        AddMessageRequest request = new AddMessageRequest(MessageSender.user, "Hi", null);

        assertThatThrownBy(() -> messageService.add(USER_ID, SESSION_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session");
    }

    @Test
    void add_savesMessageAndReturnsResponse() {
        Session session = new Session();
        session.setId(SESSION_ID);
        session.setUserId(USER_ID);
        when(sessionRepository.findByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(Optional.of(session));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            return m;
        });
        AddMessageRequest request = new AddMessageRequest(MessageSender.assistant, "Hello", null);

        MessageResponse response = messageService.add(USER_ID, SESSION_ID, request);

        assertThat(response.sender()).isEqualTo(MessageSender.assistant);
        assertThat(response.content()).isEqualTo("Hello");
        verify(messageRepository).save(argThat(m -> session.equals(m.getSession()) && "Hello".equals(m.getContent())));
    }

    @Test
    void getBySession_throwsWhenSessionNotFound() {
        when(sessionRepository.existsByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> messageService.getBySession(USER_ID, SESSION_ID, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getBySession_returnsPaginatedMessages() {
        when(sessionRepository.existsByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(true);
        Message msg = new Message();
        msg.setId(UUID.randomUUID());
        msg.setSender(MessageSender.user);
        msg.setContent("Hi");
        msg.setCreatedAt(Instant.now());
        Session s = new Session();
        s.setId(SESSION_ID);
        msg.setSession(s);
        when(messageRepository.findBySessionIdAndSessionUserId(eq(SESSION_ID), eq(USER_ID), any()))
                .thenReturn(new PageImpl<>(List.of(msg), PageRequest.of(0, 20), 1));

        Page<MessageResponse> page = messageService.getBySession(USER_ID, SESSION_ID, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).content()).isEqualTo("Hi");
        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}
