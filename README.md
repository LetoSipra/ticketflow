# 🎫 TicketFlow

A high-concurrency ticket booking engine built to handle "Thundering Herd" traffic.
This project simulates a Ticketmaster-style system where 10,000+ users compete for a limited inventory of 100 tickets.

## 🚀 The Challenge

Standard databases fail under massive concurrent write loads (Race Conditions).
TicketFlow solves this using a multi-layered locking strategy and fair queuing.

## 🛠️ Tech Stack

- **Core:** Java 17, Spring Boot 3.5.9
- **Database:** PostgreSQL (Optimistic Locking)
- **Concurrency:** Redis (Distributed Locks + Lua Scripts)
- **Queueing:** Kafka (Async Notifications)
- **Traffic Control:** Redis Sorted Sets (Virtual Waiting Room)
- **Observability:** Prometheus & Grafana (Real-time Latency & Lag Monitoring)
- **Testing:** K6 (Load Testing), Testcontainers

## 🗺️ Implementation Roadmap

### Phase 1: The Trap (Naive Implementation) 🚧

- [x] Setup Project Infrastructure (Docker, Spring Boot)
- [x] Implement Basic `POST /book` (Unsafe)
- [x] **Proof:** Crash the system with K6 (Overselling)

### Phase 2: The Fortress (Concurrency Control) 🔒

- [x] Implement Redis Distributed Lock (`SETNX`)
- [x] Write Lua Script for atomicity
- [x] **Proof:** Run K6 again (Zero Overselling)

### Phase 3: The Bouncer (Traffic Management) 🚦

- [x] Implement Virtual Waiting Room (Redis ZSET)
- [x] Enforce FIFO (First-In-First-Out) Queue Logic
- [x] **Proof:** Handle 1k+ req/sec with constant DB load

### Phase 4: The Clean Up (Reliability) 🧹

- [x] Decouple Email Service using Kafka Events
- [x] Implement Observability Dashboards (Throughput vs. Lag)
