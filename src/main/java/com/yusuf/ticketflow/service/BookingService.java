package com.yusuf.ticketflow.service;

import com.yusuf.ticketflow.dto.TicketConfirmationEvent;
import com.yusuf.ticketflow.entity.Ticket;
import com.yusuf.ticketflow.repository.TicketRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class BookingService {

    private final TicketRepository ticketRepository;
    private final RedisLockService redisLockService;
    private final NotificationProducer notificationProducer;

    public BookingService(TicketRepository ticketRepository,
            RedisLockService redisLockService,
            NotificationProducer notificationProducer) {
        this.ticketRepository = ticketRepository;
        this.redisLockService = redisLockService;
        this.notificationProducer = notificationProducer;
    }

    public String bookTicket(Long ticketId) {
        String lockKey = "lock:ticket:" + ticketId;
        String requestId = UUID.randomUUID().toString();

        try {
            // Acquire Lock
            boolean acquired = redisLockService.acquireLock(lockKey, requestId, 10000);
            if (!acquired) {
                return "Failed: System busy, please try again.";
            }

            // CRITICAL SECTION
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new RuntimeException("Ticket not found"));

            if (ticket.getStock() > 0) {
                // A. Update DB
                ticket.setStock(ticket.getStock() - 1);
                ticketRepository.save(ticket);

                // B. FIRE KAFKA EVENT (The New Part)
                // We simulate a user email for demonstration purposes/testing.
                TicketConfirmationEvent event = new TicketConfirmationEvent(
                        "user-" + requestId + "@example.com", // Mock email
                        ticketId.toString(), // Ticket ID
                        "The Big Concert", // Event Name
                        99.99 // Price
                );

                notificationProducer.sendTicketConfirmation(event);

                return "Success! Ticket booked. Email sent.";
            } else {
                return "Failed: Sold out!";
            }

        } finally {
            // Release Lock
            redisLockService.releaseLock(lockKey, requestId);
        }
    }
}