package com.yusuf.ticketflow.controller;

import com.yusuf.ticketflow.entity.Ticket;
import com.yusuf.ticketflow.repository.TicketRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final TicketRepository ticketRepository;
    private final StringRedisTemplate redisTemplate;

    public SimulationController(TicketRepository ticketRepository, StringRedisTemplate redisTemplate) {
        this.ticketRepository = ticketRepository;
        this.redisTemplate = redisTemplate;
    }

    // 1. GET INITIAL STATE
    // Returns the current stock of the "Golden Ticket"
    @GetMapping("/state")
    public ResponseEntity<Ticket> getState() {
        return ResponseEntity.ok(
                ticketRepository.findById(1L).orElseThrow(() -> new RuntimeException("Simulation not initialized")));
    }

    // 2. UPDATE CONFIG
    // Sets the stock for the next test run
    @PostMapping("/config")
    public ResponseEntity<String> configureSimulation(@RequestParam int stock) {
        if (stock < 100) {
            return ResponseEntity.badRequest().body("Stock must be at least 100 for high-concurrency tests.");
        }

        // 1. Update Database
        Ticket ticket = ticketRepository.findById(1L).orElse(new Ticket());
        ticket.setId(1L);
        ticket.setStock(stock);
        ticketRepository.save(ticket);

        // 2. CRITICAL: Delete the "Sold Out" flag in Redis
        redisTemplate.delete("ticket:1:sold_out");

        return ResponseEntity.ok("Simulation configured. Stock set to: " + stock + ". Sold Out flag cleared.");
    }

    // 3. (Reset Everything)
    @PostMapping("/reset")
    public ResponseEntity<String> resetEnvironment(
            @RequestParam(defaultValue = "100") int stock) {
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().serverCommands().flushDb();
        ticketRepository.deleteAll();

        Ticket ticket = new Ticket();

        // MUST set this if your Entity doesn't auto-generate IDs
        ticket.setId(1L);

        ticket.setName("Super Bowl Final");
        ticket.setStock(stock);

        Ticket savedTicket = ticketRepository.save(ticket);

        return ResponseEntity.ok(String.format(
                "Environment Reset! 🧹\n" +
                        "- Redis Flushed\n" +
                        "- DB Cleared\n" +
                        "- New Ticket Created: ID %d with Stock %d",
                savedTicket.getId(), savedTicket.getStock()));
    }
}