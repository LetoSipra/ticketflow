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
        Ticket ticket = ticketRepository.findById(1L).orElse(new Ticket(1L, "Super Bowl Final", 0));
        ticket.setStock(100);
        ticketRepository.save(ticket);

        System.out.println("✅ TEST DATA RESET: Ticket #1 stock set to 100.");

    }
}