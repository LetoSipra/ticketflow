package com.yusuf.ticketflow.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

//import java.util.concurrent.TimeUnit;

@Service
public class WaitingRoomService {

    private final StringRedisTemplate redisTemplate;
    // Only allow 20 users to be in the "Buying Zone" at once
    private static final int BATCH_SIZE = 20;

    public WaitingRoomService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isUserAllowed(String userId, Long ticketId) {
        String queueKey = "queue:ticket:" + ticketId;

        // 1. Add user to the queue if not already present
        redisTemplate.opsForZSet().addIfAbsent(queueKey, userId, System.currentTimeMillis());

        // 2. Find their rank in the queue
        Long rank = redisTemplate.opsForZSet().rank(queueKey, userId);

        // Reset TTL to 1 hour on each access to keep active users in the queue
        redisTemplate.expire(queueKey, Duration.ofHours(1));

        // 3. If Rank is within BATCH_SIZE, allow to proceed
        return rank != null && rank < BATCH_SIZE;
    }

    public void removeFromQueue(String userId, Long ticketId) {
        String queueKey = "queue:ticket:" + ticketId;
        redisTemplate.opsForZSet().remove(queueKey, userId);
    }

    public Long getPosition(String userId, Long ticketId) {
        String queueKey = "queue:ticket:" + ticketId;
        return redisTemplate.opsForZSet().rank(queueKey, userId);
    }
}