package com.yusuf.ticketflow.config;

import com.yusuf.ticketflow.entity.Ticket;
import com.yusuf.ticketflow.repository.TicketRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

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
        // 1. Clear ALL Redis data to ensure a clean slate
        // (Includes Locks, Queues, SoldOut Flags, Idempotency Keys)
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().serverCommands().flushDb();

        // 2. Reset the Ticket Stock for the "Hardcore" Test
        // Using ID 1 explicitly so K6 can find it.
        Ticket ticket = ticketRepository.findById(1L).orElse(new Ticket());
        ticket.setId(1L);
        ticket.setName("Super Bowl Final");
        ticket.setStock(100);
        ticketRepository.save(ticket);

        System.out.println("✅ DATA SEEDER: Redis Flushed. Stock set to 100. Ready for K6!");
    }
}