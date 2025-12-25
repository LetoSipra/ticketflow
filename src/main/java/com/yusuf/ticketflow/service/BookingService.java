package com.yusuf.ticketflow.service;

import com.yusuf.ticketflow.entity.Ticket;
import com.yusuf.ticketflow.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final TicketRepository ticketRepository;

    public BookingService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    // Fails to handle concurrency properly
    @Transactional
    public String bookTicket(Long ticketId) {
        // 1. Fetch the ticket from DB
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // 2. Check stock (The "Check-Then-Act" Race Condition)
        if (ticket.getStock() > 0) {
            // 3. Decrement stock
            ticket.setStock(ticket.getStock() - 1);

            // 4. Save to DB
            ticketRepository.save(ticket);
            return "Success! Ticket booked.";
        } else {
            return "Failed: Sold out!";
        }
    }
}