package com.yusuf.ticketflow.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;

@Service
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> acquireScript;
    private final DefaultRedisScript<Long> releaseScript;

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        // Load the Lua scripts
        this.acquireScript = new DefaultRedisScript<>();
        this.acquireScript.setLocation(new ClassPathResource("scripts/acquire_lock.lua"));
        this.acquireScript.setResultType(Long.class);

        this.releaseScript = new DefaultRedisScript<>();
        this.releaseScript.setLocation(new ClassPathResource("scripts/release_lock.lua"));
        this.releaseScript.setResultType(Long.class);
    }

    public boolean acquireLock(String lockKey, String requestId, long expireTime) {
        Long result = redisTemplate.execute(
                acquireScript,
                Collections.singletonList(lockKey), // KEYS[1]
                requestId, // ARGV[1]
                String.valueOf(expireTime) // ARGV[2]
        );
        return result != null && result == 1L;
    }

    public void releaseLock(String lockKey, String requestId) {
        redisTemplate.execute(
                releaseScript,
                Collections.singletonList(lockKey),
                requestId);
    }
}