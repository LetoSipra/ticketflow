package com.yusuf.ticketflow.service;

import com.yusuf.ticketflow.entity.Ticket;
import com.yusuf.ticketflow.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BookingService {

    private final TicketRepository ticketRepository;
    private final RedisLockService redisLockService; // Redis Service

    public BookingService(TicketRepository ticketRepository, RedisLockService redisLockService) {
        this.ticketRepository = ticketRepository;
        this.redisLockService = redisLockService;
    }

    // @Transactional
    public String bookTicket(Long ticketId) {
        // 2. Define a unique Lock Key
        String lockKey = "lock:ticket:" + ticketId;
        String requestId = UUID.randomUUID().toString(); // My unique ID

        try {
            // 3. Try to acquire the lock (Wait 0ms, expire in 10s)
            boolean acquired = redisLockService.acquireLock(lockKey, requestId, 10000);

            if (!acquired) {
                return "Failed: System busy, please try again."; // Lock contention
            }

            // 4. CRITICAL SECTION (Safe Zone)
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new RuntimeException("Ticket not found"));

            if (ticket.getStock() > 0) {
                ticket.setStock(ticket.getStock() - 1);
                ticketRepository.save(ticket);
                return "Success! Ticket booked.";
            } else {
                return "Failed: Sold out!";
            }

        } finally {
            // 5. Always release the lock!
            redisLockService.releaseLock(lockKey, requestId);
        }
    }
}