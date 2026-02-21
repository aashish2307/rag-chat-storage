package com.example.ragchat.service;

import com.example.ragchat.config.RedisCacheConfig;
import com.example.ragchat.config.SessionListCacheEvictor;
import com.example.ragchat.exception.ResourceNotFoundException;
import com.example.ragchat.model.dto.CreateSessionRequest;
import com.example.ragchat.model.dto.CachedSessionList;
import com.example.ragchat.model.dto.SessionResponse;
import com.example.ragchat.model.dto.UpdateSessionRequest;
import com.example.ragchat.model.entity.Session;
import com.example.ragchat.repository.SessionRepository;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final org.springframework.cache.Cache sessionListCache;
    private final SessionListCacheEvictor cacheEvictor;

    public SessionService(SessionRepository sessionRepository,
                          @org.springframework.beans.factory.annotation.Autowired(required = false) CacheManager cacheManager,
                          @org.springframework.beans.factory.annotation.Autowired(required = false) SessionListCacheEvictor cacheEvictor) {
        this.sessionRepository = sessionRepository;
        this.sessionListCache = cacheManager != null ? cacheManager.getCache(RedisCacheConfig.SESSION_LIST_CACHE) : null;
        this.cacheEvictor = cacheEvictor;
    }

    @Transactional
    public SessionResponse create(String userId, CreateSessionRequest request) {
        Session session = new Session();
        session.setUserId(userId);
        session.setTitle(request.getTitleOrDefault());
        session.setFavorite(false);
        session = sessionRepository.save(session);
        if (cacheEvictor != null) cacheEvictor.evictForUser(userId);
        return toResponse(session);
    }

    public Page<SessionResponse> listByUser(String userId, Boolean favorite, Pageable pageable) {
        String cacheKey = userId + "::" + (favorite != null ? favorite : "all")
                + "::" + pageable.getPageNumber() + "::" + pageable.getPageSize();
        if (sessionListCache != null) {
            org.springframework.cache.Cache.ValueWrapper wrapper = sessionListCache.get(cacheKey);
            if (wrapper != null) {
                CachedSessionList cached = (CachedSessionList) wrapper.get();
                if (cached != null) {
                    return new PageImpl<>(cached.content(), pageable, cached.totalElements());
                }
            }
        }
        Page<Session> page = favorite != null
                ? sessionRepository.findByUserIdAndFavorite(userId, favorite, pageable)
                : sessionRepository.findByUserId(userId, pageable);
        List<SessionResponse> content = page.getContent().stream().map(SessionService::toResponse).toList();
        if (sessionListCache != null) {
            sessionListCache.put(cacheKey, new CachedSessionList(
                    content, page.getTotalElements(), page.getTotalPages(), page.getSize(), page.getNumber()));
        }
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    public SessionResponse getById(String userId, UUID sessionId) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        return toResponse(session);
    }

    @Transactional
    public SessionResponse update(String userId, UUID sessionId, UpdateSessionRequest request) {
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        if (request.title() != null && !request.title().isBlank()) {
            session.setTitle(request.title().trim());
        }
        if (request.isFavorite() != null) {
            session.setFavorite(request.isFavorite());
        }
        session = sessionRepository.save(session);
        if (cacheEvictor != null) cacheEvictor.evictForUser(userId);
        return toResponse(session);
    }

    @Transactional
    public void delete(String userId, UUID sessionId) {
        if (!sessionRepository.existsByIdAndUserId(sessionId, userId)) {
            throw new ResourceNotFoundException("Session", sessionId);
        }
        sessionRepository.deleteById(sessionId);
        if (cacheEvictor != null) cacheEvictor.evictForUser(userId);
    }

    static SessionResponse toResponse(Session s) {
        return new SessionResponse(
                s.getId(),
                s.getTitle(),
                s.isFavorite(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
