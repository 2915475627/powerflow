package com.powerflow.web.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class RedisWorkflowLockAdapter {

    private final StringRedisTemplate redisTemplate;
    private static final String LOCK_PREFIX = "powerflow:lock:";

    public RedisWorkflowLockAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String acquireLock(String workflowId, Duration timeout) {
        String lockKey = LOCK_PREFIX + workflowId;
        String lockValue = UUID.randomUUID().toString();

        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, timeout);

        return Boolean.TRUE.equals(acquired) ? lockValue : null;
    }

    public boolean releaseLock(String workflowId, String lockValue) {
        String lockKey = LOCK_PREFIX + workflowId;
        String currentValue = redisTemplate.opsForValue().get(lockKey);

        if (lockValue.equals(currentValue)) {
            redisTemplate.delete(lockKey);
            return true;
        }
        return false;
    }

    public boolean tryReleaseLock(String workflowId) {
        String lockKey = LOCK_PREFIX + workflowId;
        redisTemplate.delete(lockKey);
        return true;
    }
}
