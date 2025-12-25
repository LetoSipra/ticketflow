package com.yusuf.ticketflow.controller;

import com.yusuf.ticketflow.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/book/{ticketId}")
    public ResponseEntity<String> book(@PathVariable Long ticketId) {
        String result = bookingService.bookTicket(ticketId);
        return ResponseEntity.ok(result);
    }
}