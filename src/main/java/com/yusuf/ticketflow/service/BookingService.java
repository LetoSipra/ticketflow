package com.yusuf.ticketflow.service;

import com.yusuf.ticketflow.dto.TicketConfirmationEvent;
import com.yusuf.ticketflow.entity.Ticket;
import com.yusuf.ticketflow.repository.TicketRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
public class BookingService {

    private final TicketRepository ticketRepository;
    private final RedisLockService redisLockService;
    private final NotificationProducer notificationProducer;
    private final StringRedisTemplate redisTemplate;

    public BookingService(TicketRepository ticketRepository,
            RedisLockService redisLockService,
            NotificationProducer notificationProducer,
            StringRedisTemplate redisTemplate) {
        this.ticketRepository = ticketRepository;
        this.redisLockService = redisLockService;
        this.notificationProducer = notificationProducer;
        this.redisTemplate = redisTemplate;
    }

    public String bookTicket(Long ticketId, String requestId) {
        String lockKey = "lock:ticket:" + ticketId;
        String idempotencyKey = "processed:" + requestId;

        // 1. IDEMPOTENCY CHECK
        if (Boolean.TRUE.equals(redisTemplate.hasKey(idempotencyKey))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already booked this! Check your email.");
        }

        // 2. ACQUIRE LOCK
        boolean acquired = redisLockService.acquireLock(lockKey, requestId, 10000);

        if (!acquired) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "System busy, please try again.");
        }

        try {
            // CRITICAL SECTION
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

            // 3. SOLD OUT CHECK
            if (ticket.getStock() <= 0) {
                // CRITICAL: Set the flag so the Controller can block future requests faster
                redisTemplate.opsForValue().set("ticket:" + ticketId + ":sold_out", "true");

                throw new ResponseStatusException(HttpStatus.CONFLICT, "Failed: Sold out!");
            }

            // 4. Update DB (Stock is guaranteed > 0 here)
            ticket.setStock(ticket.getStock() - 1);
            ticketRepository.save(ticket);

            // 5. Fire Kafka Event
            TicketConfirmationEvent event = new TicketConfirmationEvent(
                    "user-" + requestId + "@example.com",
                    ticketId.toString(),
                    "The Big Concert",
                    99.99);
            notificationProducer.sendTicketConfirmation(event);

            // 6. SAVE IDEMPOTENCY KEY
            redisTemplate.opsForValue().set(idempotencyKey, "BOOKED", Duration.ofMinutes(10));

            return "Success! Ticket booked. Email sent.";

        } finally {
            // Release Lock
            redisLockService.releaseLock(lockKey, requestId);
        }
    }
}