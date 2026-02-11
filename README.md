# BookMyShow Clone — Technical Documentation

## Executive Summary

This document provides comprehensive technical documentation for the BookMyShow Clone, a distributed movie ticket booking platform built using microservices architecture. The system is designed to handle high-concurrency scenarios typical of popular movie releases while maintaining data consistency and providing a seamless user experience.

**Key Technical Highlights:**

- **Microservices Architecture**: 4 backend services with clear separation of concerns
- **Three-Layer Concurrency Control**: Redisson distributed locks + PostgreSQL optimistic locking + Redis TTL
- **Write-Through Caching**: Redis cache with configurable TTLs per entity type
- **Event-Driven Notifications**: Apache Kafka for async email/SMS delivery
- **Surge Protection**: Virtual waiting room to handle flash sales
- **Admin Panel**: Full CRUD operations with cache management

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Service Architecture](#2-service-architecture)
3. [Database Design](#3-database-design)
4. [Caching Architecture](#4-caching-architecture)
5. [Seat Booking Concurrency Model](#5-seat-booking-concurrency-model)
6. [API Gateway & Traffic Management](#6-api-gateway--traffic-management)
7. [Admin Panel Architecture](#7-admin-panel-architecture)
8. [Event-Driven Architecture](#8-event-driven-architecture)
9. [Screen Layout System](#9-screen-layout-system)
10. [Scalability Patterns](#10-scalability-patterns)
11. [Infrastructure & Deployment](#11-infrastructure--deployment)
12. [Monitoring & Observability](#12-monitoring--observability)
13. [Performance Considerations](#13-performance-considerations)
14. [Security Architecture](#14-security-architecture)
15. [Configuration Reference](#15-configuration-reference)

---

## 1. Architecture Overview

### 1.1 High-Level System Diagram

```
                                    ┌─────────────────────────────────┐
                                    │         Load Balancer           │
                                    │     (Kubernetes Ingress)        │
                                    └───────────────┬─────────────────┘
                                                    │
                        ┌───────────────────────────┼───────────────────────────┐
                        │                           │                           │
                        ▼                           ▼                           ▼
              ┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
              │   Frontend      │       │   Frontend      │       │   Frontend      │
              │   (Next.js)     │       │   (Next.js)     │       │   (Next.js)     │
              │   Replica 1     │       │   Replica 2     │       │   Replica N     │
              └────────┬────────┘       └────────┬────────┘       └────────┬────────┘
                       │                         │                         │
                       └─────────────────────────┼─────────────────────────┘
                                                 │
                                    ┌────────────▼────────────┐
                                    │      API Gateway        │
                                    │  (Spring Cloud Gateway) │
                                    │                         │
                                    │  ┌───────────────────┐  │
                                    │  │ Waiting Room      │  │
                                    │  │ Filter (Surge)    │  │
                                    │  └───────────────────┘  │
                                    │  ┌───────────────────┐  │
                                    │  │ CORS Filter       │  │
                                    │  └───────────────────┘  │
                                    │  ┌───────────────────┐  │
                                    │  │ Route Predicates  │  │
                                    │  └───────────────────┘  │
                                    └────────────┬────────────┘
                                                 │
                ┌────────────────────────────────┼────────────────────────────────┐
                │                                │                                │
                ▼                                ▼                                ▼
    ┌───────────────────────┐      ┌───────────────────────┐      ┌───────────────────────┐
    │    Movie Service      │      │   Booking Service     │      │ Notification Service  │
    │    (Port 8085)        │      │    (Port 8083)        │      │    (Port 8086)        │
    │                       │      │                       │      │                       │
    │  • Movie Catalog      │      │  • Seat Locking       │      │  • Kafka Consumer     │
    │  • Theater/Screen     │      │  • Booking CRUD       │      │  • Email/SMS          │
    │  • Show Scheduling    │      │  • Lock Expiry        │      │  • Notification DB    │
    │  • Admin APIs         │      │  • Admin Bookings     │      │                       │
    │  • Layout Generation  │      │                       │      │                       │
    └───────────┬───────────┘      └───────────┬───────────┘      └───────────┬───────────┘
                │                              │                              │
                │         ┌────────────────────┼────────────────────┐         │
                │         │                    │                    │         │
                ▼         ▼                    ▼                    ▼         ▼
    ┌─────────────────────────┐    ┌─────────────────────────┐    ┌─────────────────────────┐
    │       PostgreSQL        │    │         Redis           │    │        Kafka            │
    │       (Port 5432)       │    │       (Port 6379)       │    │      (Port 9092)        │
    │                         │    │                         │    │                         │
    │  • Movies, Shows        │    │  • Distributed Locks    │    │  • booking.confirmed    │
    │  • Theaters, Screens    │    │  • Seat Lock Tokens     │    │  • booking.cancelled    │
    │  • Seats, ShowSeats     │    │  • Seat Status Cache    │    │                         │
    │  • Bookings             │    │  • Entity Caches        │    │                         │
    │  • Notifications        │    │  • Waiting Room State   │    │                         │
    └─────────────────────────┘    └─────────────────────────┘    └─────────────────────────┘
```

### 1.2 Technology Stack

| Layer           | Technology           | Version | Purpose                   |
| --------------- | -------------------- | ------- | ------------------------- |
| **Frontend**    | Next.js              | 14.x    | React SSR framework       |
|                 | TypeScript           | 5.x     | Type safety               |
|                 | Tailwind CSS         | 3.x     | Styling                   |
|                 | Zustand              | 4.x     | State management          |
| **API Gateway** | Spring Cloud Gateway | 3.2.1   | Reactive routing, filters |
| **Backend**     | Java                 | 17      | Primary language          |
|                 | Spring Boot          | 3.2.1   | Microservice framework    |
|                 | Spring Data JPA      | 3.2.1   | ORM                       |
|                 | Redisson             | 3.25    | Distributed locking       |
| **Database**    | PostgreSQL           | 15      | Primary data store        |
|                 | Flyway               | 9.x     | Schema migrations         |
| **Cache**       | Redis                | 7       | Caching + locking         |
| **Messaging**   | Apache Kafka         | 3.5     | Event streaming           |
| **Monitoring**  | Prometheus           | 2.48    | Metrics                   |
|                 | Grafana              | 10.2    | Dashboards                |
|                 | Jaeger               | 1.52    | Distributed tracing       |
| **Container**   | Docker               | 24.x    | Containerization          |
|                 | Kubernetes           | 1.28    | Orchestration             |
|                 | Helm                 | 3.x     | Package manager           |

---

## 2. Service Architecture

### 2.1 Movie Service (Port 8085)

The **catalog and scheduling service** responsible for all movie-related data.

#### Responsibilities

- Movie catalog management (CRUD, search, featured)
- City and theater management
- Screen and seat template management
- Show scheduling with conflict detection
- Static screen layout JSON generation
- Admin APIs for content management

#### Key Components

```
movie-service/
├── controller/
│   ├── MovieController.java       # Public movie APIs
│   ├── TheaterController.java     # Public theater APIs
│   ├── ShowController.java        # Public show APIs
│   ├── LayoutController.java      # Static layout JSON
│   ├── AdminMovieController.java  # Admin CRUD
│   ├── AdminShowController.java   # Show scheduling
│   ├── AdminTheaterController.java # Theater/screen mgmt
│   └── AdminDashboardController.java # Stats & cache
├── service/
│   ├── MovieService.java          # Business logic
│   ├── ShowService.java
│   ├── TheaterService.java
│   ├── AdminMovieService.java
│   ├── AdminShowService.java      # + seat generation
│   ├── AdminTheaterService.java   # + seat template gen
│   └── CacheManagementService.java
├── repository/
│   ├── MovieRepository.java
│   ├── ShowRepository.java
│   ├── ScreenRepository.java
│   ├── SeatRepository.java        # Seat templates
│   └── ShowSeatRepository.java    # Per-show seats
├── config/
│   ├── CacheConfig.java           # Redis cache TTLs
│   └── WebConfig.java             # CORS, static files
└── scheduled/
    └── ShowActivationJob.java     # Deactivate past shows
```

#### API Endpoints

| Method | Endpoint                         | Description          | Cache                   |
| ------ | -------------------------------- | -------------------- | ----------------------- |
| GET    | `/api/v1/movies`                 | Paginated movie list | `movies-list` (10m)     |
| GET    | `/api/v1/movies/{id}`            | Movie details        | `movies` (1h)           |
| GET    | `/api/v1/movies/featured`        | Featured movies      | `featured-movies` (15m) |
| GET    | `/api/v1/movies/city/{cityId}`   | Movies in city       | `movies-by-city` (10m)  |
| GET    | `/api/v1/cities`                 | All cities           | `cities` (24h)          |
| GET    | `/api/v1/theaters/city/{cityId}` | Theaters in city     | `theaters` (6h)         |
| GET    | `/api/v1/shows/{id}`             | Show details         | `shows` (5m)            |
| GET    | `/api/v1/movies/{id}/shows`      | Shows for movie      | `shows-by-movie` (5m)   |
| GET    | `/layouts/screen-{id}.json`      | Static seat layout   | CDN/browser cached      |

### 2.2 Booking Service (Port 8083)

The **transactional core** handling all seat locking and booking operations.

#### Responsibilities

- Seat availability queries with real-time status
- Distributed seat locking with three-layer protection
- Booking lifecycle management (lock → confirm → cancel)
- Lock expiry enforcement (scheduled job)
- Write-through cache updates
- Admin booking management

#### Key Components

```
booking-service/
├── controller/
│   ├── BookingController.java     # Lock, confirm, cancel
│   ├── SeatController.java        # Seat status queries
│   └── AdminBookingController.java # Admin operations
├── service/
│   ├── BookingService.java        # Core booking logic
│   ├── SeatLockService.java       # Distributed locking
│   ├── SeatCacheService.java      # Write-through cache
│   └── AdminBookingService.java
├── repository/
│   ├── BookingRepository.java
│   ├── BookingSeatRepository.java
│   └── ShowSeatRepository.java
├── config/
│   ├── RedissonConfig.java        # Lock configuration
│   ├── KafkaProducerConfig.java   # Event publishing
│   └── SecurityConfig.java
├── scheduled/
│   └── LockExpiryJob.java         # Expire stale locks
└── kafka/
    └── BookingEventPublisher.java # Kafka events
```

#### API Endpoints

| Method | Endpoint                        | Description               |
| ------ | ------------------------------- | ------------------------- |
| GET    | `/api/v1/seats/show/{showId}`   | All seats with layout     |
| GET    | `/api/v1/seats/status/{showId}` | Status-only (lightweight) |
| POST   | `/api/v1/bookings/lock`         | Lock seats (8-min TTL)    |
| POST   | `/api/v1/bookings/confirm`      | Confirm with guest info   |
| POST   | `/api/v1/bookings/{id}/cancel`  | Cancel booking            |
| GET    | `/api/v1/bookings/{id}`         | Booking by UUID           |
| GET    | `/api/v1/bookings/number/{num}` | Booking by BMS number     |

### 2.3 Notification Service (Port 8086)

The **async communication service** consuming events and sending notifications.

#### Responsibilities

- Kafka event consumption
- Email delivery (SMTP)
- SMS delivery (Twilio, optional)
- Notification record storage
- Retry handling for failed deliveries

#### Kafka Topics

| Topic               | Trigger           | Action                  |
| ------------------- | ----------------- | ----------------------- |
| `booking.confirmed` | Booking confirmed | Send confirmation email |
| `booking.cancelled` | Booking cancelled | Send cancellation email |

### 2.4 API Gateway (Port 8080)

The **entry point** for all client requests, built on Spring Cloud Gateway.

#### Responsibilities

- Request routing to downstream services
- Waiting room surge protection
- CORS header management
- Request/response logging
- Health check aggregation

---

## 3. Database Design

### 3.1 Entity Relationship Diagram

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│    cities    │◄─────│   theaters   │◄─────│   screens    │◄─────│    seats     │
│              │ 1:N  │              │ 1:N  │              │ 1:N  │  (template)  │
│  id          │      │  id          │      │  id          │      │  id          │
│  name        │      │  name        │      │  name        │      │  screen_id   │
│  state       │      │  city_id     │      │  theater_id  │      │  row_name    │
│  is_active   │      │  address     │      │  screen_type │      │  seat_number │
└──────────────┘      │  total_screens│      │  total_seats │      │  column_num  │
                      └──────────────┘      └──────┬───────┘      │  seat_type   │
                                                   │              └──────────────┘
                                                   │
                      ┌──────────────┐             │
                      │    movies    │             │
                      │              │             │
                      │  id          │             │
                      │  title       │       ┌─────▼──────────┐
                      │  duration    │       │     shows      │
                      │  language    │◄──────│                │
                      │  genre       │ 1:N   │  id            │
                      │  release_date│       │  movie_id      │
                      │  poster_url  │       │  screen_id     │
                      │  is_active   │       │  show_date     │
                      └──────────────┘       │  start_time    │
                                             │  base_price    │
                                             │  premium_price │
                                             │  recliner_price│
                                             └────────┬───────┘
                                                      │
                                                      │ 1:N
                                             ┌────────▼───────┐
                                             │   show_seats   │
                                             │  (per-show)    │
                                             │                │
                                             │  id            │
                                             │  show_id       │
                                             │  seat_id       │
                                             │  status        │
                                             │  price         │
                                             │  locked_at     │
                                             │  version       │
                                             └────────┬───────┘
                                                      │
                                                      │ N:1
                      ┌──────────────┐       ┌────────▼───────┐
                      │booking_seats │◄──────│    bookings    │
                      │              │ 1:N   │                │
                      │  id          │       │  id            │
                      │  booking_id  │       │  booking_number│
                      │  show_seat_id│       │  show_id       │
                      │  seat_row    │       │  status        │
                      │  seat_number │       │  total_amount  │
                      │  seat_type   │       │  final_amount  │
                      │  price       │       │  guest_name    │
                      └──────────────┘       │  guest_email   │
                                             │  lock_token    │
                                             │  expires_at    │
                                             │  version       │
                                             └────────────────┘
```

### 3.2 Key Tables

#### `seats` — Template Table

Physical seat layout definition per screen. Static data that serves as a template.

```sql
CREATE TABLE seats (
    id              UUID PRIMARY KEY,
    screen_id       UUID NOT NULL REFERENCES screens(id),
    row_name        VARCHAR(5) NOT NULL,      -- 'A', 'B', 'C'...
    seat_number     VARCHAR(10) NOT NULL,     -- 'A1', 'A2'...
    column_number   INT NOT NULL,             -- Physical position
    seat_type       seat_type NOT NULL,       -- REGULAR, PREMIUM, RECLINER
    is_active       BOOLEAN DEFAULT TRUE
);
```

#### `show_seats` — Per-Show Instances

Created for every seat × every show. High-volume table (~188K rows for 1,470 shows).

```sql
CREATE TABLE show_seats (
    id          UUID PRIMARY KEY,
    show_id     UUID NOT NULL REFERENCES shows(id),
    seat_id     UUID NOT NULL REFERENCES seats(id),
    status      seat_status DEFAULT 'AVAILABLE',  -- AVAILABLE, LOCKED, BOOKED
    price       DECIMAL(10,2) NOT NULL,
    locked_by   VARCHAR(255),                     -- Lock token UUID
    locked_at   TIMESTAMP WITH TIME ZONE,
    version     BIGINT DEFAULT 0,                 -- Optimistic locking

    UNIQUE(show_id, seat_id)
);

-- Critical indexes
CREATE INDEX idx_show_seats_show_id ON show_seats(show_id);
CREATE INDEX idx_show_seats_status ON show_seats(show_id, status);
CREATE INDEX idx_show_seats_locked ON show_seats(status, locked_at)
    WHERE status = 'LOCKED';
```

#### `bookings` — Guest Booking Records

```sql
CREATE TABLE bookings (
    id              UUID PRIMARY KEY,
    booking_number  VARCHAR(20) UNIQUE,       -- 'BMS-A1B2C3D4'
    show_id         UUID NOT NULL,
    status          booking_status NOT NULL,  -- PENDING, CONFIRMED, CANCELLED, EXPIRED
    total_amount    DECIMAL(10,2),
    convenience_fee DECIMAL(10,2),            -- 4.5% service charge
    final_amount    DECIMAL(10,2),
    guest_name      VARCHAR(100),
    guest_email     VARCHAR(255),
    guest_phone     VARCHAR(20),
    lock_token      VARCHAR(255),
    expires_at      TIMESTAMP WITH TIME ZONE,
    version         BIGINT DEFAULT 0,

    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_bookings_number ON bookings(booking_number);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_expires ON bookings(expires_at) WHERE status = 'PENDING';
```

### 3.3 Schema Migrations (Flyway)

| Version | File                                      | Description                     |
| ------- | ----------------------------------------- | ------------------------------- |
| V1      | `V1__initial_schema.sql`                  | Core tables, enums, constraints |
| V2      | `V2__add_indexes.sql`                     | Performance indexes             |
| V3      | `V3__add_partitions.sql`                  | Table partitioning for shows    |
| V4      | `V4__add_payment_columns.sql`             | Payment fields (legacy)         |
| V5      | `V5__drop_auth_foreign_keys.sql`          | Remove user FK constraints      |
| V6      | `V6__remove_users_payments_add_guest.sql` | Guest model migration           |

---

## 4. Caching Architecture

### 4.1 Cache Topology

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              REDIS (Port 6379)                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    DISTRIBUTED LOCKING (Redisson)                    │   │
│  │                                                                      │   │
│  │  Key: seat:lock:show:{showId}          → Redisson internal mutex    │   │
│  │  Key: seat:lock:token:{lockToken}      → "{showId}|{seatIds...}"    │   │
│  │       TTL: 8 minutes                                                 │   │
│  │                                                                      │   │
│  │  Key: waiting_room:session:{token}     → "1"                        │   │
│  │       TTL: 5 minutes                                                 │   │
│  │  Key: waiting_room:active_users        → counter                    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    SEAT STATUS CACHE (Write-Through)                 │   │
│  │                                                                      │   │
│  │  Key: show_seats:{showId}              → Hash                       │   │
│  │       Field: {showSeatId}              → "{seatId}:{status}:{price}"│   │
│  │       TTL: 5 minutes (auto-refresh on access)                       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    ENTITY CACHES (Spring Cache)                      │   │
│  │                                                                      │   │
│  │  Cache Name         │ TTL      │ Key Pattern                        │   │
│  │  ─────────────────  │ ──────── │ ──────────────────────────────     │   │
│  │  movies             │ 1 hour   │ movies::{movieId}                  │   │
│  │  movies-list        │ 10 min   │ movies-list::{page}_{size}_{city}  │   │
│  │  movies-by-city     │ 10 min   │ movies-by-city::{cityId}           │   │
│  │  featured-movies    │ 15 min   │ featured-movies::SimpleKey[]       │   │
│  │  cities             │ 24 hours │ cities::SimpleKey[]                │   │
│  │  theaters           │ 6 hours  │ theaters::{cityId}                 │   │
│  │  shows              │ 5 min    │ shows::{showId}                    │   │
│  │  shows-by-movie     │ 5 min    │ shows-by-movie::{movieId}_{date}   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Write-Through Cache Pattern

The seat status cache uses a **write-through** pattern to ensure consistency:

```
                    ┌─────────────┐
                    │   Client    │
                    └──────┬──────┘
                           │ POST /lock
                           ▼
                    ┌─────────────┐
                    │   Service   │
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
    │ 1. Acquire   │ │ 2. Update    │ │ 3. Update    │
    │ Redisson Lock│ │ PostgreSQL   │ │ Redis Cache  │
    └──────────────┘ │ (source of   │ │ (write-      │
                     │  truth)      │ │  through)    │
                     └──────────────┘ └──────────────┘
                           │               │
                           │   ON COMMIT   │
                           └───────┬───────┘
                                   │
                                   ▼
                           ┌──────────────┐
                           │ 4. Release   │
                           │ Redisson Lock│
                           └──────────────┘
```

**SeatCacheService Implementation:**

```java
// Cache format: {seatId}:{status}:{price}
// Example: "550e8400-e29b-41d4-a716-446655440000:LOCKED:350.00"

public void updateSeatStatuses(UUID showId, Map<UUID, String> statusUpdates,
                               Map<UUID, BigDecimal> prices) {
    String key = "show_seats:" + showId;

    // Only update if cache exists (don't create stale partial cache)
    if (!redisTemplate.hasKey(key)) return;

    // Read existing to preserve seatId
    Map<Object, Object> existing = redisTemplate.opsForHash().entries(key);

    Map<String, String> updates = new HashMap<>();
    for (Map.Entry<UUID, String> entry : statusUpdates.entrySet()) {
        String showSeatId = entry.getKey().toString();
        String existingValue = existing.get(showSeatId).toString();
        String seatId = existingValue.split(":")[0];  // Preserve seatId

        updates.put(showSeatId,
            seatId + ":" + entry.getValue() + ":" + prices.get(entry.getKey()));
    }

    redisTemplate.opsForHash().putAll(key, updates);
}
```

### 4.3 Cache Invalidation Strategy

| Event                           | Caches Invalidated                                           |
| ------------------------------- | ------------------------------------------------------------ |
| Movie created/updated           | `movies`, `movies-list`, `movies-by-city`, `featured-movies` |
| Show created                    | `shows-by-movie`                                             |
| Show deleted                    | `shows`, `shows-by-movie`                                    |
| Seats locked/confirmed/released | `show_seats:{showId}` (updated, not evicted)                 |
| Theater created/updated         | `theaters`                                                   |

### 4.4 Admin Cache Management

The admin panel provides cache management capabilities:

```java
@DeleteMapping("/cache/{cacheName}")
public ResponseEntity<?> clearCache(@PathVariable String cacheName) {
    cacheManagementService.clearCache(cacheName);
    return ResponseEntity.ok(Map.of("message", "Cache cleared"));
}

@DeleteMapping("/cache")
public ResponseEntity<?> clearAllCaches() {
    cacheManagementService.clearAllCaches();
    return ResponseEntity.ok(Map.of("message", "All caches cleared"));
}

@GetMapping("/cache/status")
public ResponseEntity<?> getCacheStatus() {
    return ResponseEntity.ok(cacheManagementService.getCacheStatus());
}
```

---

## 5. Seat Booking Concurrency Model

### 5.1 The Problem

When a blockbuster movie opens for booking, hundreds of users may attempt to book the same seats simultaneously. The system must guarantee:

1. **No double-booking**: A seat can only be sold to one person
2. **Fair locking**: First-come-first-served
3. **No deadlocks**: Abandoned locks must be released
4. **Consistency**: All layers agree on seat status

### 5.2 Three-Layer Concurrency Control

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    LAYER 1: REDISSON DISTRIBUTED LOCK                       │
│                                                                             │
│  Purpose: Serialize concurrent requests for the same show                  │
│  Scope:   Per-show mutex (all seat operations on a show are sequential)    │
│  Config:  Wait timeout: 5s, Lease duration: 10s                            │
│                                                                             │
│  Key: seat:lock:show:{showId}                                              │
│                                                                             │
│  RLock lock = redissonClient.getLock("seat:lock:show:" + showId);          │
│  lock.tryLock(5, 10, TimeUnit.SECONDS);                                    │
│  try {                                                                      │
│      // Critical section                                                    │
│  } finally {                                                                │
│      lock.unlock();                                                         │
│  }                                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    LAYER 2: POSTGRESQL OPTIMISTIC LOCKING                   │
│                                                                             │
│  Purpose: Catch any concurrent modifications that slip past Layer 1        │
│  Scope:   Per-row version counter                                          │
│  Config:  @Version annotation on ShowSeat and Booking entities             │
│                                                                             │
│  @Entity                                                                    │
│  public class ShowSeat {                                                    │
│      @Version                                                               │
│      private Long version;  // Auto-incremented on UPDATE                  │
│  }                                                                          │
│                                                                             │
│  UPDATE show_seats SET status = 'LOCKED', version = version + 1            │
│  WHERE id = ? AND version = ?;  -- Fails if version changed                │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    LAYER 3: REDIS TTL AUTO-EXPIRY                           │
│                                                                             │
│  Purpose: Guarantee lock release even if service crashes                   │
│  Scope:   Per-lock-token TTL                                                │
│  Config:  TTL = 8 minutes (booking.seat-lock.timeout-minutes)              │
│                                                                             │
│  Key: seat:lock:token:{lockToken}                                          │
│  Value: "{showId}|{seatId1},{seatId2}..."                                  │
│  TTL: 480 seconds                                                           │
│                                                                             │
│  redisTemplate.opsForValue().set(tokenKey, value,                          │
│      Duration.ofMinutes(seatLockTimeoutMinutes));                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SAFETY NET: LOCK EXPIRY JOB                              │
│                                                                             │
│  Purpose: Clean up orphaned locks (Redis/DB mismatch)                      │
│  Schedule: Every 60 seconds                                                 │
│                                                                             │
│  @Scheduled(fixedRate = 60000)                                              │
│  public void expireLockedSeats() {                                          │
│      // Find seats locked > 8 minutes ago                                   │
│      // Set status = AVAILABLE, clear lockedBy/lockedAt                    │
│      // Set booking status = EXPIRED                                        │
│  }                                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.3 Race Condition Walkthrough

**Scenario**: Users A and B both click "Book" for seat F-12 at the same millisecond.

```
Time     User A                              User B
─────    ──────────────────────────────      ──────────────────────────────
T+0ms    POST /bookings/lock                 POST /bookings/lock
         {showId: X, seatIds: [F-12]}        {showId: X, seatIds: [F-12]}

T+1ms    Redisson: tryLock("show:X")         Redisson: tryLock("show:X")
         → ACQUIRED ✓                         → WAITING... (up to 5s)

T+2ms    DB Query: SELECT * FROM show_seats
         WHERE id = F-12 AND status = 'AVAILABLE'
         → Found ✓

T+3ms    DB Update: UPDATE show_seats
         SET status = 'LOCKED', version = 1
         WHERE id = F-12 AND version = 0
         → Success ✓

T+4ms    Redis: SET seat:lock:token:abc
         → "X|F-12", TTL=8min

T+5ms    lock.unlock()
         Return: {lockToken: "abc", expiresAt: ...}

T+6ms                                        → ACQUIRED ✓ (lock released)

T+7ms                                        DB Query: SELECT * FROM show_seats
                                             WHERE id = F-12 AND status = 'AVAILABLE'
                                             → NOT FOUND (status = LOCKED)

T+8ms                                        throw SeatUnavailableException
                                             HTTP 409: "Seats no longer available"

Result   User A: Sees 8-minute countdown     User B: Sees error, picks new seats
```

### 5.4 Booking State Machine

```
                                     ┌─────────────┐
                                     │  AVAILABLE  │
                                     │   (seats)   │
                                     └──────┬──────┘
                                            │
                                            │ POST /lock
                                            │ Lock seats, create booking
                                            ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                              PENDING                                        │
│                                                                            │
│  • Seats: status = LOCKED                                                  │
│  • Booking: status = PENDING                                               │
│  • Redis: lock token stored with 8-min TTL                                 │
│  • Frontend: 8-minute countdown timer displayed                            │
└──────────────────────────────┬─────────────────────────────────────────────┘
                               │
           ┌───────────────────┼───────────────────┐
           │                   │                   │
           ▼                   ▼                   ▼
    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
    │  CONFIRMED   │    │   EXPIRED    │    │  CANCELLED   │
    │              │    │              │    │              │
    │ POST /confirm│    │ Timer runs   │    │ POST /cancel │
    │ + guest info │    │ out (8 min)  │    │              │
    └──────────────┘    └──────────────┘    └──────────────┘
           │                   │                   │
           ▼                   ▼                   ▼
    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
    │ Seats: BOOKED│    │Seats:AVAILABLE│   │Seats:AVAILABLE│
    │ Kafka event  │    │ Lock released │    │ Lock released │
    │ Email sent   │    │               │    │ Kafka event  │
    └──────────────┘    └──────────────┘    └──────────────┘
```

---

## 6. API Gateway & Traffic Management

### 6.1 Route Configuration

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Public Movie Service Routes
        - id: movie-service-movies
          uri: http://movie-service:8085
          predicates:
            - Path=/api/v1/movies/**

        - id: movie-service-cities
          uri: http://movie-service:8085
          predicates:
            - Path=/api/v1/cities/**

        - id: movie-service-theaters
          uri: http://movie-service:8085
          predicates:
            - Path=/api/v1/theaters/**

        - id: movie-service-shows
          uri: http://movie-service:8085
          predicates:
            - Path=/api/v1/shows/**

        - id: movie-service-layouts
          uri: http://movie-service:8085
          predicates:
            - Path=/layouts/**

        # Admin Routes - Movie Service
        - id: admin-dashboard
          uri: http://movie-service:8085
          predicates:
            - Path=/api/admin/dashboard/**

        - id: admin-movies
          uri: http://movie-service:8085
          predicates:
            - Path=/api/admin/movies/**

        - id: admin-shows
          uri: http://movie-service:8085
          predicates:
            - Path=/api/admin/shows/**

        - id: admin-theaters
          uri: http://movie-service:8085
          predicates:
            - Path=/api/admin/theaters/**

        - id: admin-cache
          uri: http://movie-service:8085
          predicates:
            - Path=/api/admin/cache/**

        # Booking Service Routes
        - id: booking-service
          uri: http://booking-service:8083
          predicates:
            - Path=/api/v1/bookings/**

        - id: seat-service
          uri: http://booking-service:8083
          predicates:
            - Path=/api/v1/seats/**

        # Admin Routes - Booking Service
        - id: admin-bookings
          uri: http://booking-service:8083
          predicates:
            - Path=/api/admin/bookings/**
```

### 6.2 Waiting Room (Surge Protection)

The **Waiting Room Filter** protects backend services during flash sales by capping concurrent active users.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         WAITING ROOM ARCHITECTURE                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Configuration:                                                             │
│    waiting-room.enabled: true                                               │
│    waiting-room.max-threshold: 5000   # Max concurrent users                │
│    waiting-room.session-ttl-seconds: 300  # 5-minute session               │
│                                                                             │
│  Flow:                                                                      │
│  ┌─────────┐                                                                │
│  │ Request │                                                                │
│  └────┬────┘                                                                │
│       │                                                                     │
│       ▼                                                                     │
│  ┌─────────────────────┐                                                    │
│  │ Has X-Surge-Token?  │───── Yes ────► Valid in Redis? ───── Yes ──►      │
│  └─────────┬───────────┘                      │                    │       │
│            │ No                               │ No                 │       │
│            ▼                                  ▼                    │       │
│  ┌─────────────────────┐              ┌──────────────┐            │       │
│  │ Count active users  │              │ Treat as new │            │       │
│  │ (Redis counter)     │              │    user      │            │       │
│  └─────────┬───────────┘              └──────┬───────┘            │       │
│            │                                 │                     │       │
│            ▼                                 │                     │       │
│  ┌─────────────────────────────┐             │                     │       │
│  │ active_users >= MAX (5000)?│─────────────┼─────────────────────┘       │
│  └─────────┬──────────────────┘             │                             │
│            │                                 │                             │
│     Yes    │    No                          │                             │
│            ▼                                 ▼                             │
│  ┌─────────────────┐              ┌──────────────────────┐                │
│  │ HTTP 302        │              │ Generate token       │                │
│  │ Redirect to     │              │ Store in Redis (5m)  │                │
│  │ /waiting-room   │              │ Increment counter    │                │
│  └─────────────────┘              │ Add X-Surge-Token    │                │
│                                   │ Allow request        │───────────────►│
│                                   └──────────────────────┘        ALLOW   │
│                                                                             │
│  Excluded Paths (always allowed):                                          │
│    - /waiting-room                                                          │
│    - /actuator/**                                                           │
│    - /layouts/**                                                            │
│    - /favicon.ico                                                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.3 CORS Configuration

CORS is handled at the **Gateway level only** to avoid duplicate headers:

```java
@Bean
public CorsWebFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("http://localhost:3000");
    config.addAllowedMethod("*");
    config.addAllowedHeader("*");
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsWebFilter(source);
}
```

**Important**: Backend services do NOT include `@CrossOrigin` annotations to prevent duplicate `Access-Control-Allow-Origin` headers.

---

## 7. Admin Panel Architecture

### 7.1 Admin Panel Overview

The admin panel provides full CRUD operations for managing the movie booking platform.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ADMIN PANEL                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐  ┌─────────────────────────────────────────────────┐  │
│  │                 │  │                                                 │  │
│  │  📊 Dashboard   │  │  Dashboard                                      │  │
│  │                 │  │  ├── Total Movies (active/inactive)             │  │
│  │  🎬 Movies      │  │  ├── Total Theaters                             │  │
│  │                 │  │  ├── Total Shows (today)                        │  │
│  │  🎭 Shows       │  │  ├── Bookings Today                             │  │
│  │                 │  │  ├── Revenue Today / This Month                 │  │
│  │  🏢 Theaters    │  │  └── Booking Status Breakdown                   │  │
│  │                 │  │                                                 │  │
│  │  📋 Bookings    │  │  Movies Management                              │  │
│  │                 │  │  ├── Create / Edit / Delete movies              │  │
│  │  💾 Cache       │  │  ├── Toggle active status                       │  │
│  │                 │  │  └── Paginated list with search                 │  │
│  │                 │  │                                                 │  │
│  │                 │  │  Shows Scheduling                               │  │
│  │                 │  │  ├── Schedule shows (movie + screen + time)     │  │
│  │                 │  │  ├── Conflict detection                         │  │
│  │                 │  │  ├── Auto-generate show_seats                   │  │
│  │                 │  │  └── Date-based filtering                       │  │
│  │                 │  │                                                 │  │
│  │                 │  │  Theaters & Screens                             │  │
│  │                 │  │  ├── Create theaters with city assignment       │  │
│  │                 │  │  ├── Add screens to theaters                    │  │
│  │                 │  │  ├── Auto-generate seat templates               │  │
│  │                 │  │  └── Expandable theater → screens view          │  │
│  │                 │  │                                                 │  │
│  │                 │  │  Bookings                                       │  │
│  │                 │  │  ├── Search by booking number / guest           │  │
│  │                 │  │  ├── Filter by status                           │  │
│  │                 │  │  ├── Admin cancel with reason                   │  │
│  │                 │  │  └── Booking details modal                      │  │
│  │                 │  │                                                 │  │
│  │                 │  │  Cache Management                               │  │
│  │                 │  │  ├── View cache status (active/empty)           │  │
│  │                 │  │  ├── Clear individual caches                    │  │
│  │                 │  │  ├── Clear all caches                           │  │
│  └─────────────────┘  │  └── TTL reference table                        │  │
│                       └─────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.2 Auto-Generation Features

#### Screen Creation → Seat Template Generation

When a screen is created via admin, seat templates are automatically generated:

```java
private int generateSeatsForScreen(Screen screen, int totalSeats) {
    int seatsPerRow = 10;
    int totalRows = (int) Math.ceil((double) totalSeats / seatsPerRow);

    List<Seat> seats = new ArrayList<>();
    for (int row = 0; row < totalRows; row++) {
        char rowName = (char) ('A' + row);

        // Seat type based on row position
        SeatType seatType;
        if (row < 2) {
            seatType = SeatType.REGULAR;      // Front rows
        } else if (row < 7) {
            seatType = SeatType.PREMIUM;      // Middle rows
        } else {
            seatType = SeatType.RECLINER;     // Back rows
        }

        for (int col = 1; col <= seatsPerRow; col++) {
            seats.add(Seat.builder()
                .screen(screen)
                .rowName(String.valueOf(rowName))
                .columnNumber(col)
                .seatNumber(rowName + String.valueOf(col))
                .seatType(seatType)
                .build());
        }
    }
    seatRepository.saveAll(seats);
    return seats.size();
}
```

#### Show Creation → ShowSeat Generation

When a show is scheduled, `show_seats` records are created from the seat template:

```java
private int generateShowSeats(Show show, BigDecimal basePrice,
                              BigDecimal premiumPrice, BigDecimal reclinerPrice) {
    List<Seat> seats = seatRepository.findByScreenIdAndIsActiveTrueOrderByRowNameAscColumnNumberAsc(
        show.getScreen().getId());

    List<ShowSeat> showSeats = new ArrayList<>();
    for (Seat seat : seats) {
        BigDecimal price = switch (seat.getSeatType()) {
            case RECLINER -> reclinerPrice != null ? reclinerPrice
                           : basePrice.multiply(BigDecimal.valueOf(2.5));
            case PREMIUM -> premiumPrice != null ? premiumPrice
                          : basePrice.multiply(BigDecimal.valueOf(1.5));
            case VIP -> reclinerPrice != null ? reclinerPrice
                      : basePrice.multiply(BigDecimal.valueOf(3.0));
            default -> basePrice;
        };

        showSeats.add(ShowSeat.builder()
            .showId(show.getId())
            .seatId(seat.getId())
            .status("AVAILABLE")
            .price(price)
            .build());
    }
    showSeatRepository.saveAll(showSeats);
    return showSeats.size();
}
```

### 7.3 Admin API Endpoints

| Service | Method | Endpoint                               | Description                  |
| ------- | ------ | -------------------------------------- | ---------------------------- |
| Movie   | GET    | `/api/admin/dashboard/stats`           | Dashboard statistics         |
| Movie   | GET    | `/api/admin/movies`                    | Paginated movie list         |
| Movie   | POST   | `/api/admin/movies`                    | Create movie                 |
| Movie   | PUT    | `/api/admin/movies/{id}`               | Update movie                 |
| Movie   | DELETE | `/api/admin/movies/{id}`               | Soft delete movie            |
| Movie   | PATCH  | `/api/admin/movies/{id}/toggle-active` | Toggle active status         |
| Movie   | GET    | `/api/admin/shows`                     | Paginated show list          |
| Movie   | POST   | `/api/admin/shows`                     | Create show + generate seats |
| Movie   | DELETE | `/api/admin/shows/{id}`                | Delete show + seats          |
| Movie   | GET    | `/api/admin/theaters`                  | All theaters                 |
| Movie   | POST   | `/api/admin/theaters`                  | Create theater               |
| Movie   | GET    | `/api/admin/theaters/{id}/screens`     | Screens for theater          |
| Movie   | POST   | `/api/admin/theaters/screens`          | Create screen + seats        |
| Movie   | GET    | `/api/admin/theaters/cities`           | All cities                   |
| Movie   | GET    | `/api/admin/cache/status`              | Cache status                 |
| Movie   | DELETE | `/api/admin/cache/{name}`              | Clear specific cache         |
| Movie   | DELETE | `/api/admin/cache`                     | Clear all caches             |
| Booking | GET    | `/api/admin/bookings`                  | Paginated booking list       |
| Booking | GET    | `/api/admin/bookings/stats`            | Booking statistics           |
| Booking | POST   | `/api/admin/bookings/{id}/cancel`      | Admin cancel                 |

---

## 8. Event-Driven Architecture

### 8.1 Kafka Configuration

```java
// Producer Config (Booking Service)
@Bean
public ProducerFactory<String, String> producerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.ACKS_CONFIG, "all");           // Wait for all replicas
    config.put(ProducerConfig.RETRIES_CONFIG, 3);            // Retry on failure
    config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);  // No duplicates
    return new DefaultKafkaProducerFactory<>(config);
}
```

### 8.2 Event Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              EVENT FLOW                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Booking Service                    Kafka                 Notification Svc  │
│  ───────────────                    ─────                 ────────────────  │
│                                                                             │
│  1. Booking confirmed               │                                       │
│     ↓                               │                                       │
│  2. kafkaTemplate.send(             │                                       │
│       "booking.confirmed",          │                                       │
│       bookingEvent                  │                                       │
│     )                               │                                       │
│     ↓                               │                                       │
│  ─────────────────────────────────► │ ────────────────────────────────────►│
│                                     │                                       │
│                                     │  3. @KafkaListener(                   │
│                                     │       topics = "booking.confirmed"    │
│                                     │     )                                 │
│                                     │     handleBookingConfirmed()          │
│                                     │     ↓                                 │
│                                     │  4. Create Notification record        │
│                                     │     ↓                                 │
│                                     │  5. Send confirmation email           │
│                                     │     (SMTP / SendGrid)                 │
│                                     │                                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 8.3 Event Payload

```json
// booking.confirmed
{
  "bookingId": "550e8400-e29b-41d4-a716-446655440000",
  "bookingNumber": "BMS-A1B2C3D4",
  "showId": "660e8400-e29b-41d4-a716-446655440000",
  "guestName": "John Doe",
  "guestEmail": "john@example.com",
  "totalAmount": 1500.0,
  "finalAmount": 1567.5,
  "seatCount": 3
}
```

---

## 9. Screen Layout System

### 9.1 Static Layout JSON

Screen layouts are pre-generated as static JSON files for frontend rendering:

```
/layouts/screen-{screenId}.json
```

**Example Layout:**

```json
{
  "screenId": "550e8400-e29b-41d4-a716-446655440000",
  "screenName": "IMAX Screen 1",
  "screenType": "IMAX",
  "totalSeats": 150,
  "rows": [
    {
      "rowName": "A",
      "seats": [
        {"seatId": "uuid1", "seatNumber": "A1", "columnNumber": 1, "seatType": "RECLINER"},
        {"seatId": "uuid2", "seatNumber": "A2", "columnNumber": 2, "seatType": "RECLINER"},
        ...
      ]
    },
    {
      "rowName": "B",
      "seats": [...]
    }
  ]
}
```

### 9.2 Layout Generation

Layouts are generated on startup and when screens are created:

```java
@PostConstruct
public void generateAllLayouts() {
    List<Screen> screens = screenRepository.findAll();
    for (Screen screen : screens) {
        generateLayoutForScreen(screen.getId());
    }
    log.info("Generated layouts for {} screens", screens.size());
}

public void generateLayoutForScreen(UUID screenId) {
    List<Seat> seats = seatRepository.findByScreenIdAndIsActiveTrueOrderByRowNameAscColumnNumberAsc(screenId);

    // Group by row
    Map<String, List<SeatDTO>> rowMap = seats.stream()
        .collect(Collectors.groupingBy(
            Seat::getRowName,
            LinkedHashMap::new,
            Collectors.mapping(this::toSeatDTO, Collectors.toList())
        ));

    LayoutDTO layout = LayoutDTO.builder()
        .screenId(screenId)
        .screenName(screen.getName())
        .screenType(screen.getScreenType())
        .totalSeats(seats.size())
        .rows(rowMap.entrySet().stream()
            .map(e -> RowDTO.builder().rowName(e.getKey()).seats(e.getValue()).build())
            .toList())
        .build();

    // Write to file
    Path path = layoutPath.resolve("screen-" + screenId + ".json");
    objectMapper.writeValue(path.toFile(), layout);
}
```

### 9.3 CDN-Decoupled Seat Selection

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        CDN-DECOUPLED FLOW                                   │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  Step 1: Load Layout (CDN-cached, immutable)                               │
│  ───────────────────────────────────────────                               │
│                                                                            │
│  Browser ─────► GET /layouts/screen-{id}.json ─────► CDN/Browser Cache     │
│                 │                                                          │
│                 │  Response: Static seat positions, types, IDs             │
│                 │  (No status - layout never changes)                      │
│                 │                                                          │
│                 ▼                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                         SEAT MAP RENDERED                            │  │
│  │   ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐                         │  │
│  │   │ A1│ A2│ A3│ A4│   │   │ A7│ A8│ A9│A10│  ← Row A (Recliner)     │  │
│  │   └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘                         │  │
│  │   ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐                         │  │
│  │   │ B1│ B2│ B3│ B4│ B5│ B6│ B7│ B8│ B9│B10│  ← Row B (Premium)      │  │
│  │   └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘                         │  │
│  │   ...                                                                │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  Step 2: Load Status (API, real-time)                                      │
│  ─────────────────────────────────────                                     │
│                                                                            │
│  Browser ─────► GET /api/v1/seats/status/{showId} ─────► Redis Cache       │
│                 │                                                          │
│                 │  Response: Map<showSeatId, status>                       │
│                 │  {"uuid1": "AVAILABLE", "uuid2": "BOOKED", ...}          │
│                 │                                                          │
│                 ▼                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                    SEAT MAP WITH STATUS OVERLAY                      │  │
│  │   ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐                         │  │
│  │   │🟢│🟢│🔴│🟢│   │   │🟡│🟢│🟢│🟢│  ← Real-time status            │  │
│  │   └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘                         │  │
│  │                                                                      │  │
│  │   🟢 AVAILABLE  🟡 LOCKED  🔴 BOOKED                                 │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  Benefits:                                                                 │
│  • Layout cached at CDN edge (never changes)                               │
│  • Status endpoint is lightweight (just IDs + status)                      │
│  • Reduces backend load by 10x for popular shows                           │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 10. Scalability Patterns

### 10.1 Horizontal Scaling Strategy

| Component            | Stateless? | Scaling Strategy | Notes                      |
| -------------------- | ---------- | ---------------- | -------------------------- |
| Frontend             | ✅ Yes     | Scale freely     | SSR pages, no session      |
| API Gateway          | ✅ Yes     | Scale freely     | Stateless routing          |
| Movie Service        | ✅ Yes     | Scale freely     | Read-heavy, cache-backed   |
| Booking Service      | ✅ Yes     | Scale with Redis | State in Redis/DB          |
| Notification Service | ✅ Yes     | Scale consumers  | Kafka partition assignment |
| PostgreSQL           | ❌ No      | Read replicas    | Single primary for writes  |
| Redis                | ❌ No      | Redis Cluster    | Sharding + HA              |
| Kafka                | ❌ No      | Add partitions   | Horizontal throughput      |

### 10.2 Bottleneck Analysis

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           BOTTLENECK ANALYSIS                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Bottleneck              Current Limit     Mitigation                       │
│  ──────────────────      ─────────────     ──────────────────────────       │
│                                                                             │
│  DB Connection Pool      20 per service    Increase pool, add read replicas│
│                                                                             │
│  Redisson Lock           1 thread/show     Short critical section (~5ms),  │
│  Contention              at a time         queue overflow handled by 409    │
│                                                                             │
│  Kafka Throughput        Single partition  Multi-partition topics           │
│                                                                             │
│  Redis Connections       10 pool size      Redis Cluster, larger pool       │
│                                                                             │
│  show_seats Table        ~128 seats/show   Partition by show_date,          │
│  Growth                                    archive old data                 │
│                                                                             │
│  API Gateway             5000 concurrent   Waiting room throttling,         │
│  Throughput              users             horizontal scaling               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 10.3 Read vs Write Path Separation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        READ/WRITE PATH SEPARATION                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  READ PATH (High Volume, Cacheable)                                         │
│  ─────────────────────────────────                                          │
│                                                                             │
│  Browser ──► CDN ──► API Gateway ──► Movie Service ──► Redis Cache          │
│                                            │                                │
│                                            └──► PostgreSQL (cache miss)     │
│                                                                             │
│  • Movie listings, details, shows                                           │
│  • Theater/screen information                                               │
│  • Static seat layouts                                                      │
│  • 90% served from cache                                                    │
│                                                                             │
│  ─────────────────────────────────────────────────────────────────────────  │
│                                                                             │
│  WRITE PATH (Low Volume, Transactional)                                     │
│  ─────────────────────────────────────                                      │
│                                                                             │
│  Browser ──► API Gateway ──► Booking Service ──► Redisson Lock              │
│                                     │                  │                    │
│                                     │                  ▼                    │
│                                     │              Redis (lock token)       │
│                                     │                                       │
│                                     ▼                                       │
│                              PostgreSQL (ACID)                              │
│                                     │                                       │
│                                     ▼                                       │
│                              Redis Cache (write-through)                    │
│                                     │                                       │
│                                     ▼                                       │
│                              Kafka (async events)                           │
│                                                                             │
│  • Seat locking, booking confirmation                                       │
│  • Must be ACID compliant                                                   │
│  • ~1% of total traffic                                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 10.4 Data Volume Projections

| Entity     | Current | Daily Growth | Monthly | Yearly | Strategy                   |
| ---------- | ------- | ------------ | ------- | ------ | -------------------------- |
| show_seats | 188K    | +27K         | +810K   | ~10M   | Partition by date, archive |
| bookings   | 25      | +100         | +3K     | ~36K   | Index optimization         |
| shows      | 1,470   | +210         | +6.3K   | ~75K   | Deactivate old shows       |
| movies     | 30      | +2           | +60     | ~720   | Active/inactive flag       |
| seats      | 2,275   | —            | —       | —      | Static template            |

---

## 11. Infrastructure & Deployment

### 11.1 Docker Compose (Development)

```yaml
version: "3.8"
services:
  postgres:
    image: postgres:15-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: bookmyshow
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres123

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    command: redis-server --requirepass redis123

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    ports: ["2181:2181"]

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports: ["9092:9092"]
    depends_on: [zookeeper]

  api-gateway:
    build: ./backend/api-gateway
    ports: ["8080:8080"]
    depends_on: [redis]

  movie-service:
    build: ./backend/movie-service
    ports: ["8085:8085"]
    depends_on: [postgres, redis]

  booking-service:
    build: ./backend/booking-service
    ports: ["8083:8083"]
    depends_on: [postgres, redis, kafka]

  notification-service:
    build: ./backend/notification-service
    ports: ["8086:8086"]
    depends_on: [postgres, kafka]

  frontend:
    build: ./frontend
    ports: ["3000:3000"]
    depends_on: [api-gateway]

  prometheus:
    image: prom/prometheus:v2.48.0
    ports: ["9090:9090"]

  grafana:
    image: grafana/grafana:10.2.2
    ports: ["3001:3000"]
```

### 11.2 Kubernetes Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         KUBERNETES CLUSTER                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Namespace: bookmyshow                                                      │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                           INGRESS                                    │   │
│  │  nginx-ingress-controller                                            │   │
│  │  ├── host: bookmyshow.example.com                                    │   │
│  │  ├── /api/*  → api-gateway-service                                   │   │
│  │  └── /*      → frontend-service                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        DEPLOYMENTS                                   │   │
│  │                                                                      │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐      │   │
│  │  │ frontend        │  │ api-gateway     │  │ movie-service   │      │   │
│  │  │ replicas: 3     │  │ replicas: 2     │  │ replicas: 2     │      │   │
│  │  │ CPU: 200m       │  │ CPU: 500m       │  │ CPU: 500m       │      │   │
│  │  │ Memory: 256Mi   │  │ Memory: 512Mi   │  │ Memory: 1Gi     │      │   │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘      │   │
│  │                                                                      │   │
│  │  ┌─────────────────┐  ┌─────────────────┐                           │   │
│  │  │ booking-service │  │ notification-svc│                           │   │
│  │  │ replicas: 3     │  │ replicas: 2     │                           │   │
│  │  │ CPU: 1000m      │  │ CPU: 200m       │                           │   │
│  │  │ Memory: 1Gi     │  │ Memory: 512Mi   │                           │   │
│  │  └─────────────────┘  └─────────────────┘                           │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     STATEFULSETS / EXTERNAL                          │   │
│  │                                                                      │   │
│  │  PostgreSQL (AWS RDS / CloudSQL)                                     │   │
│  │  Redis (AWS ElastiCache / MemoryStore)                               │   │
│  │  Kafka (Confluent Cloud / AWS MSK)                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        CONFIGMAPS / SECRETS                          │   │
│  │                                                                      │   │
│  │  ConfigMap: bookmyshow-config                                        │   │
│  │  ├── SPRING_PROFILES_ACTIVE: production                              │   │
│  │  ├── REDIS_HOST: redis.bookmyshow.svc                                │   │
│  │  └── KAFKA_BOOTSTRAP_SERVERS: kafka:9092                             │   │
│  │                                                                      │   │
│  │  Secret: bookmyshow-secrets                                          │   │
│  │  ├── DB_PASSWORD: ****                                               │   │
│  │  └── REDIS_PASSWORD: ****                                            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 12. Monitoring & Observability

### 12.1 Metrics (Prometheus)

All services expose `/actuator/prometheus`:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: "api-gateway"
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets: ["api-gateway:8080"]

  - job_name: "movie-service"
    static_configs:
      - targets: ["movie-service:8085"]

  - job_name: "booking-service"
    static_configs:
      - targets: ["booking-service:8083"]
```

**Key Metrics:**

| Metric                             | Type      | Description             |
| ---------------------------------- | --------- | ----------------------- |
| `http_server_requests_seconds`     | Histogram | Request latency         |
| `hikaricp_connections_active`      | Gauge     | Active DB connections   |
| `redis_commands_duration_seconds`  | Histogram | Redis operation latency |
| `kafka_producer_record_send_total` | Counter   | Kafka messages sent     |
| `jvm_memory_used_bytes`            | Gauge     | JVM heap usage          |

### 12.2 Distributed Tracing (Jaeger)

Request tracing across service boundaries:

```
Frontend → API Gateway → Movie Service (trace-id: abc123)
                      → Booking Service (trace-id: abc123)
                           → Redis
                           → PostgreSQL
                           → Kafka → Notification Service
```

### 12.3 Logging

Structured JSON logging with correlation IDs:

```json
{
  "timestamp": "2026-02-11T22:30:45.123Z",
  "level": "INFO",
  "logger": "c.b.booking.service.BookingService",
  "message": "Booking confirmed",
  "bookingId": "550e8400-...",
  "bookingNumber": "BMS-A1B2C3D4",
  "traceId": "abc123def456"
}
```

---

## 13. Performance Considerations

### 13.1 Database Query Optimization

**Native SQL for Seat Layout (avoids N+1):**

```java
@Query(value = """
    SELECT ss.id, ss.show_id, ss.seat_id, ss.status, ss.price,
           s.row_name, s.seat_number, s.seat_type, s.column_number
    FROM show_seats ss
    JOIN seats s ON ss.seat_id = s.id
    WHERE ss.show_id = :showId
    ORDER BY s.row_name, s.column_number
    """, nativeQuery = true)
List<ShowSeatDTO> findSeatsWithLayoutByShowId(@Param("showId") UUID showId);
```

### 13.2 Connection Pool Settings

| Service         | DB Pool Max | DB Pool Min | Redis Pool Max |
| --------------- | ----------- | ----------- | -------------- |
| Booking Service | 20          | 5           | 10             |
| Movie Service   | 20          | 5           | 10             |
| Notification    | 10          | 3           | —              |

### 13.3 Cache Hit Rates

| Cache        | Expected Hit Rate | Strategy                            |
| ------------ | ----------------- | ----------------------------------- |
| `movies`     | >95%              | Long TTL (1h), evict on update      |
| `cities`     | >99%              | Very long TTL (24h), rarely changes |
| `theaters`   | >90%              | Medium TTL (6h)                     |
| `show_seats` | ~70%              | Short TTL (5m), write-through       |

---

## 14. Security Architecture

### 14.1 Current Model (Guest Booking)

| Layer            | Security Posture                       |
| ---------------- | -------------------------------------- |
| API Gateway      | Pass-through (no auth)                 |
| Backend Services | `permitAll()` on all endpoints         |
| Database         | No users table; guest info per booking |
| Frontend         | No login; city selection only          |
| Booking Identity | Booking number (`BMS-XXXXXXXX`)        |

### 14.2 Security Measures

| Measure          | Implementation               |
| ---------------- | ---------------------------- |
| CSRF             | Disabled (stateless API)     |
| Sessions         | `STATELESS`                  |
| Lock Token       | UUID validated against Redis |
| Input Validation | `@Valid` + Bean Validation   |
| SQL Injection    | JPA parameterized queries    |
| Seat Limit       | Max 10 seats (server-side)   |

---

## 15. Configuration Reference

### 15.1 Seat Lock Timing

| Parameter              | Value  | Config Key                                         |
| ---------------------- | ------ | -------------------------------------------------- |
| Lock timeout           | 8 min  | `booking.seat-lock.timeout-minutes`                |
| Distributed lock wait  | 5 sec  | `booking.seat-lock.distributed-lock-wait-seconds`  |
| Distributed lock lease | 10 sec | `booking.seat-lock.distributed-lock-lease-seconds` |
| Lock expiry job        | 60 sec | `@Scheduled(fixedRate = 60000)`                    |
| Max seats per booking  | 10     | Hardcoded                                          |
| Convenience fee        | 4.5%   | Hardcoded                                          |

### 15.2 Cache TTLs

| Cache             | TTL      | Rationale                      |
| ----------------- | -------- | ------------------------------ |
| `movies`          | 1 hour   | Movie details change rarely    |
| `movies-list`     | 10 min   | New movies added periodically  |
| `movies-by-city`  | 10 min   | Shows change by city           |
| `featured-movies` | 15 min   | Editorial curation             |
| `cities`          | 24 hours | Very static data               |
| `theaters`        | 6 hours  | Theaters rarely change         |
| `shows`           | 5 min    | Show times are time-sensitive  |
| `shows-by-movie`  | 5 min    | Same as above                  |
| `show_seats`      | 5 min    | Seat status changes frequently |

### 15.3 Service Ports

| Service              | Port  | Protocol |
| -------------------- | ----- | -------- |
| Frontend             | 3000  | HTTP     |
| API Gateway          | 8080  | HTTP     |
| Booking Service      | 8083  | HTTP     |
| Movie Service        | 8085  | HTTP     |
| Notification Service | 8086  | HTTP     |
| PostgreSQL           | 5432  | TCP      |
| Redis                | 6379  | TCP      |
| Kafka                | 9092  | TCP      |
| Prometheus           | 9090  | HTTP     |
| Grafana              | 3001  | HTTP     |
| Jaeger UI            | 16686 | HTTP     |

---

## Appendix A: Key Design Decisions

| Decision                        | Rationale                                                    |
| ------------------------------- | ------------------------------------------------------------ |
| Guest booking (no auth)         | Reduces friction; MVP focus on booking flow                  |
| Separate `seats` / `show_seats` | Template pattern: layout fixed, status varies per show       |
| Redisson over SETNX             | Reentrant locks, auto-renewal, fairness                      |
| Kafka over REST                 | Decouples services; async notification                       |
| Optimistic locking              | Better throughput under low contention                       |
| 8-minute lock timeout           | Balance: long enough to fill form, short enough to not block |
| Booking number format           | Human-readable for guest reference                           |
| Snapshot in `booking_seats`     | Preserves booking details if layout changes                  |
| Single DB, multiple services    | Simplicity for MVP; can split later                          |
| Write-through cache             | Ensures cache consistency with DB                            |
| CDN-decoupled layouts           | Reduces backend load for popular shows                       |

---

## Appendix B: API Error Codes

| Error Code           | HTTP Status | Description                   |
| -------------------- | ----------- | ----------------------------- |
| `SEAT_UNAVAILABLE`   | 409         | Requested seats not available |
| `INVALID_LOCK_TOKEN` | 403         | Lock token mismatch           |
| `BOOKING_EXPIRED`    | 410         | Lock timeout exceeded         |
| `VALIDATION_ERROR`   | 400         | Input validation failed       |
| `RESOURCE_NOT_FOUND` | 404         | Entity not found              |
| `CONFLICT`           | 409         | Optimistic lock failure       |
| `INTERNAL_ERROR`     | 500         | Unexpected server error       |

---

_Document Version: 1.0.0_  
_Last Updated: February 11, 2026_
