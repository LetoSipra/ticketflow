package com.yusuf.ticketflow.config;

import com.yusuf.ticketflow.entity.Ticket;
import com.yusuf.ticketflow.repository.TicketRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final TicketRepository ticketRepository;

    public DataSeeder(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // If ticket with ID 1 doesn't exist, create it with 100 stock
        if (!ticketRepository.existsById(1L)) {
            Ticket ticket = new Ticket(1L, "Super Bowl Final", 100);
            ticketRepository.save(ticket);
            System.out.println("TEST DATA INITIALIZED: Ticket #1 created with 100 stock.");
        }
    }
}