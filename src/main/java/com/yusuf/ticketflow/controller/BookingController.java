package com.yusuf.ticketflow.controller;

import com.yusuf.ticketflow.service.BookingService;
import com.yusuf.ticketflow.service.WaitingRoomService;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class BookingController {

    private final BookingService bookingService;
    private final WaitingRoomService waitingRoomService;
    private final StringRedisTemplate redisTemplate;

    public BookingController(BookingService bookingService, WaitingRoomService waitingRoomService,
            StringRedisTemplate redisTemplate) {
        this.bookingService = bookingService;
        this.waitingRoomService = waitingRoomService;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/book/{ticketId}")
    public ResponseEntity<String> book(
            @PathVariable Long ticketId,
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader("X-Request-ID") String requestId) {

        // 0. OPTIMIZATION: Check the global "Sold Out" flag first
        if (redisTemplate.hasKey("ticket:" + ticketId + ":sold_out")) {
            return ResponseEntity.status(409).body("Sold out!");
        }

        // 1. Check if user is in the allowed queue
        if (!waitingRoomService.isUserAllowed(userId, ticketId)) {
            Long rank = waitingRoomService.getPosition(userId, ticketId);
            return ResponseEntity.status(429).body("Queue Position: " + (rank + 1) + ". Please wait.");
        }

        // 2. Proceed to Book
        String result = bookingService.bookTicket(ticketId, requestId);

        // 3. If successful (or failed due to SOLD OUT), remove from queue
        if (result.contains("Success") || result.contains("Sold out")) {
            waitingRoomService.removeFromQueue(userId, ticketId);
        }

        return ResponseEntity.ok(result);
    }
}