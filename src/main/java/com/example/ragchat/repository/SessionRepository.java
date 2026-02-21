package com.example.ragchat.repository;

import com.example.ragchat.model.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Page<Session> findByUserId(String userId, Pageable pageable);

    Page<Session> findByUserIdAndFavorite(String userId, boolean favorite, Pageable pageable);

    Optional<Session> findByIdAndUserId(UUID id, String userId);

    boolean existsByIdAndUserId(UUID id, String userId);
}
