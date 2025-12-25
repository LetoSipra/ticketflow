# 🎫 TicketFlow

A high-concurrency ticket booking engine built to handle "Thundering Herd" traffic.
This project simulates a Ticketmaster-style system where 10,000+ users compete for a limited inventory of 100 tickets.

## 🚀 The Challenge

Standard databases fail under massive concurrent write loads (Race Conditions).
TicketFlow solves this using a multi-layered locking strategy and fair queuing.

## 🛠️ Tech Stack

- **Core:** Java 17, Spring Boot 3.2
- **Database:** PostgreSQL (Optimistic Locking)
- **Concurrency:** Redis (Distributed Locks + Lua Scripts)
- **Queueing:** Kafka (Async Notifications)
- **Traffic Control:** Redis Sorted Sets (Token Bucket Algorithm)
- **Testing:** K6 (Load Testing), Testcontainers

## 🗺️ Implementation Roadmap

### Phase 1: The Trap (Naive Implementation) 🚧

- [x] Setup Project Infrastructure (Docker, Spring Boot)
- [x] Implement Basic `POST /book` (Unsafe)
- [ ] **Proof:** Crash the system with K6 (Overselling)

### Phase 2: The Fortress (Concurrency Control) 🔒

- [ ] Implement Redis Distributed Lock (`SETNX`)
- [ ] Write Lua Script for atomicity
- [ ] **Proof:** Run K6 again (Zero Overselling)

### Phase 3: The Bouncer (Traffic Management) 🚦

- [ ] Implement Virtual Waiting Room (Redis ZSET)
- [ ] Implement Token Bucket Rate Limiter
- [ ] **Proof:** Handle 5k req/sec with constant DB load

### Phase 4: The Clean Up (Reliability) 🧹

- [ ] Add Scheduler for expired lock cleanup
- [ ] Decouple Email Service using Kafka Events
