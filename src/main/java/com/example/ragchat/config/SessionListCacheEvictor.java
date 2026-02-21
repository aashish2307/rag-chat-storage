package com.example.ragchat.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Evicts session list cache entries for a user (on create/update/delete session).
 */
@Component
@ConditionalOnBean(StringRedisTemplate.class)
public class SessionListCacheEvictor {

    private static final String KEY_PREFIX = RedisCacheConfig.SESSION_LIST_CACHE + "::";

    private final StringRedisTemplate redisTemplate;

    public SessionListCacheEvictor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Evict all session list cache entries for the given user.
     */
    public void evictForUser(String userId) {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + userId + "::*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
