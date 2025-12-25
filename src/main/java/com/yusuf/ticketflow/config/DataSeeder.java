package com.yusuf.ticketflow.config;

import com.yusuf.ticketflow.entity.Ticket;
import com.yusuf.ticketflow.repository.TicketRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final TicketRepository ticketRepository;
    private final StringRedisTemplate redisTemplate;

    public DataSeeder(TicketRepository ticketRepository, StringRedisTemplate redisTemplate) {
        this.ticketRepository = ticketRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // 2. Clear the Redis Queue
        redisTemplate.delete("queue:ticket:1");

        // 3. Reset the Ticket Stock
        Ticket ticket = ticketRepository.findById(1L).orElse(new Ticket(1L, "Super Bowl Final", 0));
        ticket.setStock(100);
        ticketRepository.save(ticket);

        System.out.println("TEST DATA RESET: Queue cleared & Stock set to 100.");
    }
}