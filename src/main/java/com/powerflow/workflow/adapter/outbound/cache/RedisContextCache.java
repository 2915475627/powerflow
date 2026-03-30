package com.powerflow.workflow.adapter.outbound.cache;

import com.powerflow.workflow.domain.model.Context;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisContextCache {

    private static final String KEY_PREFIX = "context:";
    private static final Duration TTL = Duration.ofHours(1);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisContextCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String executionId, Context context) {
        redisTemplate.opsForValue().set(KEY_PREFIX + executionId, context.toMap(), TTL);
    }

    public Optional<Context> get(String executionId) {
        Object value = redisTemplate.opsForValue().get(KEY_PREFIX + executionId);
        if (value instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) value;
            return Optional.of(new Context(map));
        }
        return Optional.empty();
    }

    public void delete(String executionId) {
        redisTemplate.delete(KEY_PREFIX + executionId);
    }
}
