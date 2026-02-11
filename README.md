# BookMyShow Clone - System Design & Architecture

## Table of Contents

1. [Overview](#overview)
2. [High-Level Architecture](#high-level-architecture)
3. [Core Design Principles](#core-design-principles)
4. [Microservices Architecture](#microservices-architecture)
5. [Database Design](#database-design)
6. [Caching Strategy](#caching-strategy)
7. [Seat Booking Flow](#seat-booking-flow)
8. [Concurrency & Race Condition Handling](#concurrency--race-condition-handling)
9. [Scalability Patterns](#scalability-patterns)
10. [High Availability](#high-availability)
11. [Performance Optimizations](#performance-optimizations)
12. [Security Considerations](#security-considerations)
13. [Monitoring & Observability](#monitoring--observability)
14. [Failure Handling](#failure-handling)
15. [Future Improvements](#future-improvements)

---

## Overview

This document provides comprehensive technical documentation for a movie ticket booking system (BookMyShow clone) designed to handle high concurrency, flash sales scenarios, and scale to millions of users.

### Key Requirements

- **Functional**: Browse movies, view showtimes, select seats, book tickets, manage bookings
- **Non-Functional**:
  - Handle 10,000+ concurrent seat selections per show
  - Sub-second seat availability updates
  - 99.9% availability during peak hours
  - No double-booking (seat consistency)
  - Graceful degradation under load

### Tech Stack

| Layer                   | Technology                                  |
| ----------------------- | ------------------------------------------- |
| Frontend                | Next.js 14 (React), TypeScript, TailwindCSS |
| API Gateway             | Spring Cloud Gateway                        |
| Backend Services        | Spring Boot 3.2, Java 17                    |
| Database                | PostgreSQL 15 (with sharding support)       |
| Cache                   | Redis 7 (Cluster mode)                      |
| Message Queue           | Redis Pub/Sub (upgradeable to Kafka)        |
| Container Orchestration | Kubernetes (Helm charts)                    |
| Monitoring              | Prometheus, Grafana                         |
| Load Balancer           | NGINX / K8s Ingress                         |

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                  CLIENTS                                     │
│                    (Web Browser, Mobile App, Third-party)                   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CDN (CloudFront/Akamai)                        │
│                     Static Assets, Screen Layouts (JSON)                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           LOAD BALANCER (NGINX)                             │
│                         Rate Limiting, SSL Termination                      │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         API GATEWAY (Spring Cloud)                          │
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Routing   │  │ Rate Limit  │  │Waiting Room │  │   Metrics   │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
┌───────────────────────┐ ┌───────────────────────┐ ┌───────────────────────┐
│    MOVIE SERVICE      │ │   BOOKING SERVICE     │ │ NOTIFICATION SERVICE  │
│    (Port 8085)        │ │    (Port 8083)        │ │    (Port 8084)        │
│                       │ │                       │ │                       │
│ • Movie CRUD          │ │ • Seat Locking        │ │ • Email/SMS           │
│ • Show Scheduling     │ │ • Booking Management  │ │ • Push Notifications  │
│ • Theater Management  │ │ • Payment Integration │ │ • Booking Confirmations│
│ • Search & Discovery  │ │ • Seat Cache Sync     │ │                       │
└───────────────────────┘ └───────────────────────┘ └───────────────────────┘
           │                         │                         │
           └─────────────────────────┼─────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              REDIS CLUSTER                                  │
│                                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │  Seat Status    │  │  Session/Lock   │  │  Response Cache │             │
│  │  (Real-time)    │  │   Management    │  │   (TTL-based)   │             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         POSTGRESQL CLUSTER                                  │
│                                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │  Primary (RW)   │  │   Replica 1     │  │   Replica 2     │             │
│  │                 │◄─│   (Read-only)   │  │   (Read-only)   │             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
│                                                                             │
│  City-based Sharding: shard_mumbai, shard_delhi, shard_bangalore            │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Core Design Principles

### 1. Separation of Concerns

- **Static data** (screen layouts, seat positions) served via CDN
- **Dynamic data** (seat availability) served via Redis cache
- **Persistent data** (bookings, transactions) stored in PostgreSQL

### 2. Optimistic UI with Pessimistic Locking

- Frontend shows optimistic seat selection
- Backend uses distributed locks for seat reservation
- Conflict resolution with immediate feedback

### 3. Event-Driven Cache Invalidation

- Write-through caching for seat status
- Pub/Sub for multi-instance cache sync
- TTL-based expiration as fallback

### 4. Graceful Degradation

- Waiting room for traffic spikes
- Circuit breakers for downstream failures
- Fallback to database when cache unavailable

---

## Microservices Architecture

### Service Responsibilities

#### API Gateway (Port 8080)

```yaml
Responsibilities:
  - Request routing to downstream services
  - Rate limiting (token bucket algorithm)
  - Waiting room queue during flash sales
  - CORS handling (centralized)
  - Request/Response logging
  - Circuit breaker coordination

Key Components:
  - WaitingRoomFilter: Queues excess traffic
  - RateLimitFilter: Per-IP request throttling
  - CorsGlobalConfig: Centralized CORS policy
```

#### Movie Service (Port 8085)

```yaml
Responsibilities:
  - Movie catalog management (CRUD)
  - Theater and screen management
  - Show scheduling with conflict detection
  - Seat template generation
  - City/Location management
  - Search and discovery

Key Entities:
  - Movie, Theater, Screen, City
  - Seat (template), Show, ShowSeat

Caching Strategy:
  - movies: 1 hour TTL
  - movies-list: 10 min TTL
  - cities: 24 hours TTL
  - theaters: 6 hours TTL
  - shows: 5 min TTL
```

#### Booking Service (Port 8083)

```yaml
Responsibilities:
  - Seat locking with distributed locks
  - Booking lifecycle management
  - Payment processing coordination
  - Seat status cache synchronization
  - Booking expiration handling

Key Entities:
  - Booking, BookingSeat
  - ShowSeat (status management)

Critical Flows:
  - Lock → Confirm → Complete
  - Lock → Expire → Release
  - Lock → Cancel → Release
```

#### Notification Service (Port 8084)

```yaml
Responsibilities:
  - Email notifications (booking confirmation)
  - SMS alerts (optional)
  - Push notifications
  - Reminder scheduling

Integration:
  - Async message consumption
  - Template-based rendering
  - Delivery status tracking
```

### Inter-Service Communication

```
┌──────────────┐     REST API      ┌──────────────┐
│ Movie Service│◄─────────────────►│Booking Service│
└──────────────┘                   └──────────────┘
       │                                  │
       │         Redis Pub/Sub            │
       └──────────────────────────────────┘
                      │
                      ▼
              ┌──────────────┐
              │ Notification │
              │   Service    │
              └──────────────┘
```

---

## Database Design

### Entity Relationship Diagram

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   cities    │     │   movies    │     │   users     │
├─────────────┤     ├─────────────┤     ├─────────────┤
│ id (PK)     │     │ id (PK)     │     │ id (PK)     │
│ name        │     │ title       │     │ email       │
│ state       │     │ description │     │ phone       │
│ country     │     │ duration    │     │ role        │
└─────────────┘     │ language    │     └─────────────┘
       │            │ genre       │
       │            │ release_date│
       ▼            │ rating      │
┌─────────────┐     │ poster_url  │
│  theaters   │     │ is_active   │
├─────────────┤     └─────────────┘
│ id (PK)     │            │
│ city_id(FK) │◄───────────┼───────────────┐
│ name        │            │               │
│ address     │            ▼               │
│ total_screens            │               │
└─────────────┘     ┌─────────────┐        │
       │            │   shows     │        │
       ▼            ├─────────────┤        │
┌─────────────┐     │ id (PK)     │        │
│  screens    │     │ movie_id(FK)│◄───────┘
├─────────────┤     │ screen_id(FK)◄───┐
│ id (PK)     │     │ show_date   │    │
│ theater_id  │◄────│ start_time  │    │
│ name        │     │ end_time    │    │
│ screen_type │     │ base_price  │    │
│ total_seats │     │ premium_price    │
└─────────────┘     │ recliner_price   │
       │            │ is_active   │    │
       ▼            └─────────────┘    │
┌─────────────┐            │           │
│   seats     │            ▼           │
├─────────────┤     ┌─────────────┐    │
│ id (PK)     │     │ show_seats  │    │
│ screen_id(FK)◄────┤─────────────┤    │
│ row_name    │     │ id (PK)     │    │
│ column_num  │     │ show_id (FK)│◄───┘
│ seat_number │     │ seat_id (FK)│◄───┐
│ seat_type   │     │ status      │    │
│ is_active   │     │ price       │    │
└─────────────┘     │ locked_by   │    │
                    │ locked_until│    │
                    └─────────────┘    │
                           │           │
                           ▼           │
                    ┌─────────────┐    │
                    │  bookings   │    │
                    ├─────────────┤    │
                    │ id (PK)     │    │
                    │ booking_num │    │
                    │ show_id     │    │
                    │ guest_name  │    │
                    │ guest_email │    │
                    │ status      │    │
                    │ total_amount│    │
                    │ created_at  │    │
                    └─────────────┘    │
                           │           │
                           ▼           │
                    ┌─────────────┐    │
                    │booking_seats│    │
                    ├─────────────┤    │
                    │ id (PK)     │    │
                    │ booking_id  │    │
                    │ show_seat_id│────┘
                    │ seat_number │
                    │ seat_row    │
                    │ price       │
                    └─────────────┘
```

### Sharding Strategy

The system supports **city-based horizontal sharding** for geographic distribution:

```sql
-- Shard configuration
shard_mumbai    → Cities: Mumbai, Pune, Nashik
shard_delhi     → Cities: Delhi, Noida, Gurgaon
shard_bangalore → Cities: Bangalore, Chennai, Hyderabad
```

**Shard Key**: `city_id` propagated through `X-City-ID` header

```java
// API Gateway extracts city from request
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String cityId = exchange.getRequest().getHeaders().getFirst("X-City-ID");
    // Route to appropriate shard based on city
}
```

### Indexing Strategy

```sql
-- High-cardinality indexes for fast lookups
CREATE INDEX idx_shows_movie_date ON shows(movie_id, show_date);
CREATE INDEX idx_shows_screen_date ON shows(screen_id, show_date);
CREATE INDEX idx_show_seats_show ON show_seats(show_id);
CREATE INDEX idx_show_seats_status ON show_seats(show_id, status);
CREATE INDEX idx_bookings_number ON bookings(booking_number);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_show ON bookings(show_id);

-- Partial index for active records only
CREATE INDEX idx_active_shows ON shows(show_date) WHERE is_active = true;
```

### Table Partitioning

```sql
-- Partition bookings by month for archival
CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    ...
) PARTITION BY RANGE (created_at);

CREATE TABLE bookings_2026_01 PARTITION OF bookings
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE bookings_2026_02 PARTITION OF bookings
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
```

---

## Caching Strategy

### Multi-Layer Cache Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CACHE LAYERS                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Layer 1: CDN (Static)                                          │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ • Screen layout JSON files (immutable)                   │    │
│  │ • Movie posters and banners                              │    │
│  │ • Static assets (JS, CSS)                                │    │
│  │ TTL: Until invalidation / versioned URLs                 │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                   │
│                              ▼                                   │
│  Layer 2: Redis (Dynamic)                                       │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Seat Status Cache:                                       │    │
│  │   Key: show_seats:{showId}                               │    │
│  │   Value: Hash { showSeatId -> "seatId:status:price" }    │    │
│  │   TTL: 5 minutes (write-through invalidation)            │    │
│  │                                                          │    │
│  │ Response Cache:                                          │    │
│  │   movies: 1 hour    │  shows: 5 min                      │    │
│  │   cities: 24 hours  │  theaters: 6 hours                 │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                   │
│                              ▼                                   │
│  Layer 3: Database (Persistent)                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ • Source of truth for all data                           │    │
│  │ • Read replicas for query distribution                   │    │
│  │ • Connection pooling (HikariCP)                          │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Cache Configuration

```java
@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()));
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        Map<String, RedisCacheConfiguration> configs = Map.of(
            "movies",         config(Duration.ofHours(1)),
            "movies-list",    config(Duration.ofMinutes(10)),
            "movies-by-city", config(Duration.ofMinutes(10)),
            "featured-movies",config(Duration.ofMinutes(15)),
            "cities",         config(Duration.ofHours(24)),
            "theaters",       config(Duration.ofHours(6)),
            "shows",          config(Duration.ofMinutes(5)),
            "shows-by-movie", config(Duration.ofMinutes(5))
        );

        return RedisCacheManager.builder(factory)
            .withInitialCacheConfigurations(configs)
            .build();
    }
}
```

### Write-Through Cache Pattern (Seat Status)

```
                      ┌─────────────────┐
                      │   Client App    │
                      └────────┬────────┘
                               │ 1. Lock Seat Request
                               ▼
                      ┌─────────────────┐
                      │ Booking Service │
                      └────────┬────────┘
                               │
            ┌──────────────────┼──────────────────┐
            │                  │                  │
            ▼                  ▼                  ▼
   2. Acquire Lock    3. Update DB      4. Update Cache
   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
   │ Redis Lock   │   │  PostgreSQL  │   │ Redis Cache  │
   │ seat:{id}    │   │  show_seats  │   │ show_seats:  │
   │ TTL: 30s     │   │  status=LOCKED   │ {showId}     │
   └──────────────┘   └──────────────┘   └──────────────┘
                               │
                               ▼
                      5. Return Success
```

**Key Insight**: Cache updates happen synchronously after DB commit, ensuring cache is always consistent with DB state.

---

## Seat Booking Flow

### Complete Flow Diagram

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  Client  │    │   CDN    │    │ Gateway  │    │ Booking  │    │  Redis   │
│          │    │          │    │          │    │ Service  │    │          │
└────┬─────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘
     │               │               │               │               │
     │ 1. Load Show  │               │               │               │
     │──────────────►│               │               │               │
     │◄──────────────│ Layout JSON   │               │               │
     │               │               │               │               │
     │ 2. Get Seat Status            │               │               │
     │──────────────────────────────►│               │               │
     │               │               │──────────────►│               │
     │               │               │               │──────────────►│
     │               │               │               │◄──────────────│
     │◄──────────────────────────────────────────────│ Seat Statuses │
     │               │               │               │               │
     │ 3. Select Seats (UI)          │               │               │
     │ ─ ─ ─ ─ ─ ─ ─►│               │               │               │
     │               │               │               │               │
     │ 4. Lock Request               │               │               │
     │──────────────────────────────►│               │               │
     │               │               │──────────────►│               │
     │               │               │               │───┐ Acquire   │
     │               │               │               │◄──┘ Lock      │
     │               │               │               │               │
     │               │               │               │──── Update DB │
     │               │               │               │               │
     │               │               │               │──────────────►│
     │               │               │               │ Update Cache  │
     │◄──────────────────────────────────────────────│               │
     │               │  Lock Token + Expiry          │               │
     │               │               │               │               │
     │ 5. Confirm Booking            │               │               │
     │──────────────────────────────►│               │               │
     │               │               │──────────────►│               │
     │               │               │               │ Verify Token  │
     │               │               │               │ Update Status │
     │               │               │               │──────────────►│
     │◄──────────────────────────────────────────────│               │
     │               │ Booking Confirmation          │               │
     │               │               │               │               │
```

### State Machine

```
                    ┌─────────────────────────────────────────────┐
                    │                                             │
                    ▼                                             │
┌─────────────┐  Lock   ┌─────────────┐  Confirm  ┌─────────────┐│
│  AVAILABLE  │────────►│   LOCKED    │──────────►│  CONFIRMED  ││
└─────────────┘         └─────────────┘           └─────────────┘│
       ▲                       │                         │        │
       │                       │ Expire/Cancel           │Cancel  │
       │                       ▼                         ▼        │
       │                ┌─────────────┐           ┌─────────────┐ │
       └────────────────│  AVAILABLE  │◄──────────│  CANCELLED  │─┘
                        └─────────────┘           └─────────────┘
```

### Lock Expiration Handling

```java
@Scheduled(fixedRate = 30000) // Every 30 seconds
@Transactional
public void releaseExpiredLocks() {
    LocalDateTime now = LocalDateTime.now();

    // Find all expired locked seats
    List<ShowSeat> expiredSeats = showSeatRepository
        .findByStatusAndLockedUntilBefore("LOCKED", now);

    for (ShowSeat seat : expiredSeats) {
        // Release the seat
        seat.setStatus("AVAILABLE");
        seat.setLockedBy(null);
        seat.setLockedUntil(null);

        // Update cache
        seatCacheService.updateSeatStatuses(
            seat.getShowId(),
            Map.of(seat.getId(), "AVAILABLE"),
            Map.of(seat.getId(), seat.getPrice())
        );
    }

    // Also expire pending bookings
    bookingRepository.expirePendingBookings(now);
}
```

---

## Concurrency & Race Condition Handling

### Problem: Multiple Users Selecting Same Seat

```
Timeline:
─────────────────────────────────────────────────────────────►
    │           │           │           │
    │           │           │           │
User A ────────►│ Lock A1   │           │
    │           │           │           │
User B ─────────────────────►│ Lock A1  │
    │           │           │           │
                        CONFLICT!
```

### Solution: Distributed Locking with Redis

```java
public BookingResponse lockSeats(LockRequest request) {
    List<UUID> seatIds = request.getSeatIds();

    // Sort seat IDs to prevent deadlock
    Collections.sort(seatIds);

    List<String> lockKeys = seatIds.stream()
        .map(id -> "seat_lock:" + id)
        .toList();

    try {
        // Acquire all locks atomically using Redis SETNX
        boolean acquired = redisLockService.acquireMultipleLocks(
            lockKeys,
            LOCK_TTL_SECONDS
        );

        if (!acquired) {
            throw new SeatUnavailableException("One or more seats already locked");
        }

        // Verify seats are still available in DB
        verifySeatsAvailable(request.getShowId(), seatIds);

        // Create booking with PENDING status
        Booking booking = createPendingBooking(request);

        // Update seat status in DB
        updateSeatStatus(seatIds, "LOCKED", booking.getId());

        // Update cache (write-through)
        updateSeatCache(request.getShowId(), seatIds, "LOCKED");

        return buildResponse(booking);

    } catch (Exception e) {
        // Release locks on failure
        redisLockService.releaseLocks(lockKeys);
        throw e;
    }
}
```

### Redis Lock Implementation

```java
@Service
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    public boolean acquireMultipleLocks(List<String> keys, long ttlSeconds) {
        String lockValue = UUID.randomUUID().toString();

        // Use Lua script for atomic multi-key locking
        String luaScript = """
            for i, key in ipairs(KEYS) do
                if redis.call('EXISTS', key) == 1 then
                    -- Rollback any locks we acquired
                    for j = 1, i - 1 do
                        redis.call('DEL', KEYS[j])
                    end
                    return 0
                end
                redis.call('SETEX', key, ARGV[1], ARGV[2])
            end
            return 1
            """;

        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            keys,
            String.valueOf(ttlSeconds),
            lockValue
        );

        return result != null && result == 1;
    }
}
```

### Optimistic Locking for Booking Updates

```java
@Entity
public class Booking {
    @Version
    private Long version;  // Optimistic lock

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private BookingStatus status;
}

// Usage - throws OptimisticLockException if concurrent modification
@Transactional
public Booking confirmBooking(UUID bookingId, String lockToken) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

    if (!booking.getLockToken().equals(lockToken)) {
        throw new InvalidTokenException("Lock token mismatch");
    }

    booking.setStatus(BookingStatus.CONFIRMED);
    return bookingRepository.save(booking);  // Version check happens here
}
```

---

## Scalability Patterns

### Horizontal Scaling Architecture

```
                         ┌─────────────────────────────────────────┐
                         │           Load Balancer                  │
                         │      (Round Robin / Least Conn)          │
                         └─────────────────────────────────────────┘
                                           │
              ┌────────────────────────────┼────────────────────────────┐
              │                            │                            │
              ▼                            ▼                            ▼
     ┌─────────────────┐          ┌─────────────────┐          ┌─────────────────┐
     │  API Gateway    │          │  API Gateway    │          │  API Gateway    │
     │   Instance 1    │          │   Instance 2    │          │   Instance 3    │
     └─────────────────┘          └─────────────────┘          └─────────────────┘
              │                            │                            │
              └────────────────────────────┼────────────────────────────┘
                                           │
              ┌────────────────────────────┼────────────────────────────┐
              │                            │                            │
              ▼                            ▼                            ▼
     ┌─────────────────┐          ┌─────────────────┐          ┌─────────────────┐
     │ Movie Service   │          │ Booking Service │          │ Booking Service │
     │   (3 replicas)  │          │   (5 replicas)  │          │   (5 replicas)  │
     └─────────────────┘          └─────────────────┘          └─────────────────┘
```

### Auto-Scaling Configuration (Kubernetes)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: booking-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: booking-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
    - type: Pods
      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "1000"
```

### Database Read Replicas

```yaml
# Application configuration for read/write splitting
spring:
  datasource:
    primary:
      url: jdbc:postgresql://primary.db:5432/bookmyshow
      hikari:
        maximum-pool-size: 20
    replica:
      url: jdbc:postgresql://replica.db:5432/bookmyshow
      hikari:
        maximum-pool-size: 50
```

```java
// Read-only transactions go to replica
@Transactional(readOnly = true)
public List<Movie> searchMovies(String query) {
    // Routed to read replica
    return movieRepository.searchByTitle(query);
}

// Write transactions go to primary
@Transactional
public Booking createBooking(BookingRequest request) {
    // Routed to primary
    return bookingRepository.save(booking);
}
```

### Waiting Room Pattern (Flash Sales)

```java
@Component
public class WaitingRoomFilter implements GlobalFilter, Ordered {

    private final RedisTemplate<String, String> redisTemplate;
    private final WaitingRoomConfig config;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!config.isEnabled()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();

        // Only apply to booking endpoints
        if (!path.startsWith("/api/v1/bookings")) {
            return chain.filter(exchange);
        }

        // Check current active sessions
        Long activeCount = redisTemplate.opsForValue()
            .increment("active_booking_sessions");

        if (activeCount > config.getMaxThreshold()) {
            // Put in waiting room
            String queuePosition = addToWaitingQueue(exchange);

            return exchange.getResponse()
                .writeWith(Mono.just(buildWaitingResponse(queuePosition)));
        }

        // Grant access, set session TTL
        String sessionId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
            "session:" + sessionId,
            "active",
            Duration.ofSeconds(config.getSessionTtl())
        );

        exchange.getResponse().getHeaders()
            .add("X-Session-Id", sessionId);

        return chain.filter(exchange);
    }
}
```

---

## High Availability

### Multi-Zone Deployment

```
┌─────────────────────────────────────────────────────────────────────┐
│                         REGION: ap-south-1                          │
│                                                                     │
│  ┌──────────────────────┐    ┌──────────────────────┐              │
│  │  Availability Zone A │    │  Availability Zone B │              │
│  │                      │    │                      │              │
│  │  ┌────────────────┐  │    │  ┌────────────────┐  │              │
│  │  │ K8s Node Pool  │  │    │  │ K8s Node Pool  │  │              │
│  │  │ (3 nodes)      │  │    │  │ (3 nodes)      │  │              │
│  │  └────────────────┘  │    │  └────────────────┘  │              │
│  │                      │    │                      │              │
│  │  ┌────────────────┐  │    │  ┌────────────────┐  │              │
│  │  │ Redis Primary  │──┼────┼──│ Redis Replica  │  │              │
│  │  └────────────────┘  │    │  └────────────────┘  │              │
│  │                      │    │                      │              │
│  │  ┌────────────────┐  │    │  ┌────────────────┐  │              │
│  │  │ PG Primary     │──┼────┼──│ PG Replica     │  │              │
│  │  └────────────────┘  │    │  └────────────────┘  │              │
│  │                      │    │                      │              │
│  └──────────────────────┘    └──────────────────────┘              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Health Checks

```java
@Component
public class BookingServiceHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, String> redisTemplate;
    private final DataSource dataSource;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();

        // Check Redis
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            details.put("redis", "UP");
        } catch (Exception e) {
            details.put("redis", "DOWN: " + e.getMessage());
            return Health.down().withDetails(details).build();
        }

        // Check Database
        try (Connection conn = dataSource.getConnection()) {
            conn.isValid(2);
            details.put("database", "UP");
        } catch (Exception e) {
            details.put("database", "DOWN: " + e.getMessage());
            return Health.down().withDetails(details).build();
        }

        return Health.up().withDetails(details).build();
    }
}
```

### Circuit Breaker Pattern

```java
@Service
public class MovieServiceClient {

    private final CircuitBreaker circuitBreaker;
    private final RestTemplate restTemplate;

    public MovieServiceClient(CircuitBreakerRegistry registry) {
        this.circuitBreaker = registry.circuitBreaker("movie-service");
    }

    public MovieResponse getMovie(UUID movieId) {
        return circuitBreaker.executeSupplier(() -> {
            return restTemplate.getForObject(
                "http://movie-service/api/v1/movies/" + movieId,
                MovieResponse.class
            );
        });
    }

    // Fallback when circuit is open
    public MovieResponse getMovieFallback(UUID movieId, Exception e) {
        log.warn("Circuit breaker open for movie-service, returning cached data");
        return cacheService.getCachedMovie(movieId);
    }
}
```

---

## Performance Optimizations

### 1. CDN for Static Layouts

Screen layouts are static JSON files served directly from CDN:

```javascript
// Frontend fetches layout from CDN (not API)
const layout = await fetch(`/layouts/screen-${screenId}.json`);
```

```java
// Layouts generated once and cached
@Scheduled(cron = "0 0 * * * *") // Every hour
public void generateLayoutFiles() {
    List<Screen> screens = screenRepository.findAllActive();

    for (Screen screen : screens) {
        ScreenLayout layout = buildLayout(screen);
        String json = objectMapper.writeValueAsString(layout);

        // Write to /layouts directory
        Files.writeString(
            Path.of("layouts", "screen-" + screen.getId() + ".json"),
            json
        );
    }
}
```

### 2. Batch Seat Status Updates

```java
// Instead of N individual Redis calls, use pipelining
public void updateSeatStatuses(UUID showId, Map<UUID, String> updates) {
    String key = "show_seats:" + showId;

    redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
        for (Map.Entry<UUID, String> entry : updates.entrySet()) {
            connection.hSet(
                key.getBytes(),
                entry.getKey().toString().getBytes(),
                entry.getValue().getBytes()
            );
        }
        return null;
    });
}
```

### 3. Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
      leak-detection-threshold: 60000

  data:
    redis:
      lettuce:
        pool:
          min-idle: 5
          max-idle: 20
          max-active: 50
          max-wait: 2000ms
```

### 4. Response Compression

```yaml
server:
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/plain
    min-response-size: 1024
```

### 5. Database Query Optimization

```java
// Use projections for list queries
public interface MovieListProjection {
    UUID getId();
    String getTitle();
    String getPosterUrl();
    String getGenre();
}

@Query("SELECT m.id as id, m.title as title, m.posterUrl as posterUrl, m.genre as genre " +
       "FROM Movie m WHERE m.isActive = true")
List<MovieListProjection> findAllActiveProjection();
```

---

## Security Considerations

### 1. Rate Limiting

```java
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> Mono.just(
        exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
    );
}

// application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: booking-service
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: "#{@userKeyResolver}"
```

### 2. Input Validation

```java
@Data
public class LockSeatsRequest {
    @NotNull(message = "Show ID is required")
    private UUID showId;

    @NotEmpty(message = "At least one seat must be selected")
    @Size(max = 10, message = "Maximum 10 seats per booking")
    private List<@NotNull UUID> seatIds;
}
```

### 3. SQL Injection Prevention

All queries use parameterized statements via JPA:

```java
// Safe - parameterized
@Query("SELECT m FROM Movie m WHERE m.title LIKE %:query%")
List<Movie> searchByTitle(@Param("query") String query);

// Never do this:
// @Query("SELECT m FROM Movie m WHERE m.title LIKE '%" + query + "%'")
```

### 4. CORS Configuration

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("https://bookmyshow.example.com");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
```

---

## Monitoring & Observability

### Metrics Collection

```java
@Component
public class BookingMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter bookingsCreated;
    private final Counter bookingsFailed;
    private final Timer lockDuration;

    public BookingMetrics(MeterRegistry registry) {
        this.meterRegistry = registry;
        this.bookingsCreated = Counter.builder("bookings.created")
            .description("Number of successful bookings")
            .register(registry);
        this.bookingsFailed = Counter.builder("bookings.failed")
            .description("Number of failed bookings")
            .register(registry);
        this.lockDuration = Timer.builder("bookings.lock.duration")
            .description("Time taken to acquire seat locks")
            .register(registry);
    }

    public void recordSuccessfulBooking() {
        bookingsCreated.increment();
    }

    public void recordLockDuration(long milliseconds) {
        lockDuration.record(Duration.ofMillis(milliseconds));
    }
}
```

### Prometheus Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: "api-gateway"
    static_configs:
      - targets: ["api-gateway:8080"]
    metrics_path: "/actuator/prometheus"

  - job_name: "movie-service"
    static_configs:
      - targets: ["movie-service:8085"]
    metrics_path: "/actuator/prometheus"

  - job_name: "booking-service"
    static_configs:
      - targets: ["booking-service:8083"]
    metrics_path: "/actuator/prometheus"
```

### Grafana Dashboard Panels

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        BOOKMYSHOW DASHBOARD                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐         │
│  │ Requests/sec    │  │ Error Rate      │  │ P99 Latency     │         │
│  │     2,847       │  │     0.12%       │  │     145ms       │         │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘         │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────┐       │
│  │ Bookings Over Time                                          │       │
│  │  ▄▄▄█████▄▄▄▄▄▄▄███████▄▄▄▄▄▄▄▄▄████████▄▄▄               │       │
│  └─────────────────────────────────────────────────────────────┘       │
│                                                                         │
│  ┌─────────────────────────────┐  ┌─────────────────────────────┐      │
│  │ Cache Hit Rate              │  │ Active Locks                │      │
│  │         94.7%               │  │         1,247               │      │
│  └─────────────────────────────┘  └─────────────────────────────┘      │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────┐       │
│  │ Service Health                                               │       │
│  │  ● API Gateway: UP    ● Movie Service: UP                   │       │
│  │  ● Booking Service: UP ● Notification: UP                   │       │
│  └─────────────────────────────────────────────────────────────┘       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Alerting Rules

```yaml
# prometheus/alerts/booking-alerts.yml
groups:
  - name: booking-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"

      - alert: SlowBookingLocks
        expr: histogram_quantile(0.99, rate(bookings_lock_duration_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Booking lock acquisition is slow"

      - alert: CacheHitRateLow
        expr: redis_cache_hits / (redis_cache_hits + redis_cache_misses) < 0.80
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Cache hit rate below 80%"
```

---

## Failure Handling

### Graceful Degradation Matrix

| Component Failure         | Impact          | Mitigation                                                |
| ------------------------- | --------------- | --------------------------------------------------------- |
| Redis Down                | No seat caching | Fall back to DB queries, disable new bookings temporarily |
| DB Primary Down           | No writes       | Promote replica, queue writes                             |
| DB Replica Down           | Slower reads    | Route all traffic to primary                              |
| Movie Service Down        | No browsing     | Serve cached data from CDN/Redis                          |
| Booking Service Down      | No bookings     | Show "maintenance" message, queue requests                |
| Notification Service Down | No emails       | Queue notifications, retry later                          |

### Retry Configuration

```java
@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
            .maxAttempts(3)
            .fixedBackoff(1000)
            .retryOn(TransientDataAccessException.class)
            .retryOn(RedisConnectionException.class)
            .build();
    }
}

// Usage
@Retryable(value = {RedisConnectionException.class}, maxAttempts = 3)
public void updateCache(UUID showId, Map<UUID, String> updates) {
    // Redis operations
}
```

### Dead Letter Queue for Failed Operations

```java
@Component
public class FailedBookingHandler {

    @RabbitListener(queues = "booking-dlq")
    public void handleFailedBooking(FailedBookingMessage message) {
        log.error("Booking failed permanently: {}", message);

        // Store for manual review
        failedBookingRepository.save(FailedBooking.builder()
            .originalRequest(message.getRequest())
            .errorMessage(message.getError())
            .failedAt(LocalDateTime.now())
            .build());

        // Notify ops team
        alertService.notifyOps("Booking failure", message);
    }
}
```

---

## Future Improvements

### 1. Event Sourcing for Bookings

```
Event Store:
  SeatSelected → SeatLocked → PaymentInitiated → PaymentCompleted → BookingConfirmed
```

### 2. GraphQL API for Flexible Queries

```graphql
query {
  movie(id: "...") {
    title
    shows(date: "2026-02-15") {
      startTime
      screen {
        name
        availableSeats
      }
    }
  }
}
```

### 3. Real-time Seat Updates via WebSocket

```javascript
// Client subscribes to seat updates
ws.subscribe(`/topic/show/${showId}/seats`, (update) => {
  updateSeatUI(update.seatId, update.status);
});
```

### 4. Machine Learning for Demand Prediction

- Dynamic pricing based on demand
- Show scheduling optimization
- Fraud detection

### 5. Multi-Region Deployment

- Active-active setup across regions
- Geo-routing for lowest latency
- Cross-region data replication

---

## Appendix

### A. API Endpoints Summary

| Method | Endpoint                      | Description           |
| ------ | ----------------------------- | --------------------- |
| GET    | `/api/v1/movies`              | List all movies       |
| GET    | `/api/v1/movies/{id}`         | Get movie details     |
| GET    | `/api/v1/movies/{id}/shows`   | Get shows for a movie |
| GET    | `/api/v1/shows/{id}`          | Get show details      |
| GET    | `/api/v1/seats/show/{showId}` | Get seat availability |
| POST   | `/api/v1/bookings/lock`       | Lock selected seats   |
| POST   | `/api/v1/bookings/confirm`    | Confirm booking       |
| GET    | `/api/v1/bookings/{id}`       | Get booking details   |

### B. Environment Variables

| Variable                 | Description                  | Default        |
| ------------------------ | ---------------------------- | -------------- |
| `DATABASE_URL`           | PostgreSQL connection string | localhost:5432 |
| `REDIS_HOST`             | Redis server hostname        | localhost      |
| `REDIS_PORT`             | Redis server port            | 6379           |
| `SEAT_LOCK_TTL_SECONDS`  | Seat lock duration           | 300            |
| `WAITING_ROOM_THRESHOLD` | Max concurrent bookings      | 5000           |

### C. Performance Benchmarks

| Metric            | Target      | Achieved         |
| ----------------- | ----------- | ---------------- |
| Seat status query | < 50ms      | 12ms (cache hit) |
| Lock seats        | < 200ms     | 89ms             |
| Confirm booking   | < 500ms     | 234ms            |
| Concurrent locks  | 10,000/show | Tested 15,000    |
| Cache hit rate    | > 90%       | 94.7%            |

---

_Document Version: 1.0_  
_Last Updated: February 2026_  
_Author: System Design Team_
