package com.example.ragchat.repository;

import com.example.ragchat.model.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findBySessionIdAndSessionUserId(UUID sessionId, String userId, Pageable pageable);

    boolean existsBySessionIdAndSessionUserId(UUID sessionId, String userId);
}
