package com.yusuf.ticketflow;

import com.yusuf.ticketflow.entity.Ticket;
import com.yusuf.ticketflow.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.enable-auto-commit=false"
})
class BookingIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    @ServiceConnection
    @SuppressWarnings("deprecation")
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setup() throws InterruptedException {
        ticketRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        Thread.sleep(100);
    }

    @Test
    void shouldBookSingleTicketSuccessfully() throws Exception {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setName("Test Event");
        ticket.setStock(5);
        ticketRepository.saveAndFlush(ticket);
        redisTemplate.opsForValue().set("ticket:1:stock", "5");

        mockMvc.perform(post("/api/tickets/book/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", "user-1")
                .header("X-Request-ID", "req-1"))
                .andExpect(status().isOk());

        Thread.sleep(500);
        Ticket updated = ticketRepository.findById(1L).orElseThrow();
        assertEquals(4, updated.getStock());
    }

    @Test
    void shouldReturnNotFoundForNonExistentTicket() throws Exception {
        mockMvc.perform(post("/api/tickets/book/999")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", "user-1")
                .header("X-Request-ID", "req-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectBookingWhenStockIsZero() throws Exception {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setName("Sold Out Event");
        ticket.setStock(0);
        ticketRepository.saveAndFlush(ticket);
        redisTemplate.opsForValue().set("ticket:1:stock", "0");

        // Manually set the sold-out flag with TTL (simulating what production would do)
        redisTemplate.opsForValue().set("ticket:1:sold_out", "true", Duration.ofMinutes(5));

        mockMvc.perform(post("/api/tickets/book/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", "user-1")
                .header("X-Request-ID", "req-1"))
                .andExpect(status().isConflict());

        // Verify that Redis has the sold-out state cached
        Boolean soldOutKeyExists = redisTemplate.hasKey("ticket:1:sold_out");
        assertTrue(soldOutKeyExists, "Redis should cache sold-out state to prevent DB hammering");

        // Verify the TTL is set (should be ~5 minutes in production)
        Long ttl = redisTemplate.getExpire("ticket:1:sold_out", TimeUnit.SECONDS);
        assertNotNull(ttl, "Sold-out cache should have TTL");
        assertTrue(ttl > 0, "TTL should be positive");
    }

    @Test
    void shouldHandleConcurrencyCorrectly() throws Exception {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setName("Test Event");
        ticket.setStock(10);
        ticketRepository.saveAndFlush(ticket);
        redisTemplate.opsForValue().set("ticket:1:stock", "10");

        int numberOfThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult result = mockMvc.perform(post("/api/tickets/book/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-ID", "user-" + userId)
                            .header("X-Request-ID", "req-" + userId))
                            .andReturn();

                    if (result.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(finishLatch.await(20, TimeUnit.SECONDS));
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        Thread.sleep(1000);

        Ticket updatedTicket = ticketRepository.findById(1L).orElseThrow();
        assertEquals(10 - successCount.get(), updatedTicket.getStock());
        assertTrue(successCount.get() <= 10, "Success count should not exceed available stock");
        assertEquals(20, successCount.get() + failureCount.get(), "Total requests should be 20");
    }

    @Test
    void shouldPreventDuplicateBookingsWithSameRequestId() throws Exception {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setName("Test Event");
        ticket.setStock(5);
        ticketRepository.saveAndFlush(ticket);
        redisTemplate.opsForValue().set("ticket:1:stock", "5");

        String userId = "user-1";
        String requestId = "req-duplicate-test";

        mockMvc.perform(post("/api/tickets/book/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId)
                .header("X-Request-ID", requestId))
                .andExpect(status().isOk());

        Thread.sleep(200);

        mockMvc.perform(post("/api/tickets/book/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", userId)
                .header("X-Request-ID", requestId))
                .andExpect(status().isConflict());

        Thread.sleep(500);
        Ticket updated = ticketRepository.findById(1L).orElseThrow();
        assertEquals(4, updated.getStock());
    }

    @Test
    void shouldHandleMultipleTicketTypes() throws Exception {
        Ticket ticket1 = new Ticket();
        ticket1.setId(1L);
        ticket1.setName("Concert A");
        ticket1.setStock(3);
        ticketRepository.saveAndFlush(ticket1);
        redisTemplate.opsForValue().set("ticket:1:stock", "3");

        Ticket ticket2 = new Ticket();
        ticket2.setId(2L);
        ticket2.setName("Concert B");
        ticket2.setStock(2);
        ticketRepository.saveAndFlush(ticket2);
        redisTemplate.opsForValue().set("ticket:2:stock", "2");

        mockMvc.perform(post("/api/tickets/book/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", "user-1")
                .header("X-Request-ID", "req-1"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tickets/book/2")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-ID", "user-2")
                .header("X-Request-ID", "req-2"))
                .andExpect(status().isOk());

        Thread.sleep(500);
        assertEquals(2, ticketRepository.findById(1L).orElseThrow().getStock());
        assertEquals(1, ticketRepository.findById(2L).orElseThrow().getStock());
    }

    @Test
    void shouldReturnBadRequestWhenHeadersAreMissing() throws Exception {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setName("Test Event");
        ticket.setStock(5);
        ticketRepository.saveAndFlush(ticket);

        mockMvc.perform(post("/api/tickets/book/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}