package com.example.ragchat.service;

import com.example.ragchat.exception.ResourceNotFoundException;
import com.example.ragchat.model.dto.AddMessageRequest;
import com.example.ragchat.model.dto.MessageResponse;
import com.example.ragchat.model.entity.Message;
import com.example.ragchat.model.entity.Session;
import com.example.ragchat.repository.MessageRepository;
import com.example.ragchat.repository.SessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final SessionRepository sessionRepository;

    public MessageService(MessageRepository messageRepository, SessionRepository sessionRepository) {
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public MessageResponse add(String userId, UUID sessionId, AddMessageRequest request) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        Message message = new Message();
        message.setSession(session);
        message.setSender(request.sender());
        message.setContent(request.content().trim());
        message.setContext(request.context());
        message = messageRepository.save(message);
        return toResponse(message);
    }

    public Page<MessageResponse> getBySession(String userId, UUID sessionId, Pageable pageable) {
        if (!sessionRepository.existsByIdAndUserId(sessionId, userId)) {
            throw new ResourceNotFoundException("Session", sessionId);
        }
        return messageRepository.findBySessionIdAndSessionUserId(sessionId, userId, pageable)
                .map(MessageService::toResponse);
    }

    static MessageResponse toResponse(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getSender(),
                m.getContent(),
                m.getContext(),
                m.getCreatedAt()
        );
    }
}
