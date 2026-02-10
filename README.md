# BookMyShow Clone — System Design Document

## Table of Contents

1. [High-Level Architecture](#1-high-level-architecture)
2. [Service Breakdown](#2-service-breakdown)
3. [Database Design](#3-database-design)
4. [Concurrent Seat Booking — The Core Problem](#4-concurrent-seat-booking--the-core-problem)
5. [Booking Lifecycle — Step by Step](#5-booking-lifecycle--step-by-step)
6. [Distributed Locking Deep Dive](#6-distributed-locking-deep-dive)
7. [Event-Driven Notifications (Kafka)](#7-event-driven-notifications-kafka)
8. [API Gateway & Routing](#8-api-gateway--routing)
9. [Caching Strategy (Redis)](#9-caching-strategy-redis)
10. [Static Asset Decoupling (CDN Layout)](#10-static-asset-decoupling-cdn-layout)
11. [Surge Protection (Virtual Waiting Room)](#11-surge-protection-virtual-waiting-room)
12. [Logical Geo-Sharding](#12-logical-geo-sharding)
13. [Scheduled Jobs & Background Processes](#13-scheduled-jobs--background-processes)
14. [Frontend Architecture](#14-frontend-architecture)
15. [Error Handling & Resilience](#15-error-handling--resilience)
16. [Infrastructure & DevOps](#16-infrastructure--devops)
17. [Monitoring & Observability](#17-monitoring--observability)
18. [Scalability Considerations](#18-scalability-considerations)
19. [Security Model](#19-security-model)
20. [Data Flow Diagrams](#20-data-flow-diagrams)

---

## 1. High-Level Architecture

The system follows a **microservices architecture** with four backend services communicating through REST APIs and Apache Kafka for asynchronous events. It incorporates four high-scale optimizations: **layered caching**, **static asset decoupling**, **surge protection**, and **logical geo-sharding**.

```
                              ┌──────────────────┐
                              │   Next.js Frontend│
                              │   (Port 3000)     │
                              └────────┬─────────┘
                                       │ HTTP + X-City-ID header
                              ┌────────▼─────────┐
                              │   API Gateway     │
                              │ (Spring Cloud GW) │
                              │   (Port 8080)     │
                              │                   │
                              │ ┌───────────────┐ │
                              │ │WaitingRoomFilter│ │ ← Surge protection (Redis counter)
                              │ └───────────────┘ │
                              └──┬──────────────┬──┘
                                 │              │
                ┌────────────────▼──┐    ┌──────▼───────────────┐
                │  Movie Service    │    │  Booking Service     │
                │   (Port 8085)     │    │    (Port 8083)       │
                │                   │    │                      │
                │ ┌───────────────┐ │    │ ┌──────────────────┐ │
                │ │LayoutGenerator│ │    │ │ SeatCacheService │ │ ← Redis Hash cache
                │ └───────────────┘ │    │ └──────────────────┘ │
                │ ┌───────────────┐ │    │ ┌──────────────────┐ │
                │ │ Static /layouts│ │    │ │  SeatLockService │ │ ← Redisson distributed locks
                │ └───────────────┘ │    │ └──────────────────┘ │
                └───────┬───────────┘    └──┬──────┬────┬───────┘
                        │                   │      │    │
                  ┌─────▼──────┐    ┌──────▼──┐ ┌─▼──┐ │
                  │ PostgreSQL │    │  Redis  │ │Kafka│ │
                  │   (5432)   │◄── │ (6379)  │ │9092│ │
                  │            │    └─────────┘ └─┬──┘ │
                  │ ┌────────┐ │                  │    │
                  │ │ north  │ │       ┌──────────▼────▼──┐
                  │ │ south  │ │       │Notification Svc  │
                  │ └────────┘ │       │   (Port 8086)    │
                  └────────────┘       └──────────────────┘
                  (Geo-sharded)
```

### Technology Stack

| Layer                | Technology                                            |
| -------------------- | ----------------------------------------------------- |
| Frontend             | Next.js 14, TypeScript, Tailwind CSS, Zustand         |
| API Gateway          | Spring Cloud Gateway (reactive/Netty)                 |
| Backend Services     | Java 17, Spring Boot 3.2.1                            |
| Database             | PostgreSQL 15 with Flyway migrations                  |
| Cache / Locking      | Redis 7 + Redisson (distributed locks) + Redis Hashes |
| Message Broker       | Apache Kafka + Zookeeper                              |
| Search               | Elasticsearch 8                                       |
| Monitoring           | Prometheus + Grafana + Jaeger (tracing)               |
| Containerization     | Docker + Docker Compose                               |
| Orchestration        | Kubernetes + Helm                                     |

---

## 2. Service Breakdown

### 2.1 API Gateway (`api-gateway` — Port 8080)

The **single entry point** for all client requests. Built on Spring Cloud Gateway (reactive/Netty-based).

**Responsibilities:**

- Routes incoming HTTP requests to downstream microservices
- Cross-cutting concerns (CORS, rate limiting, request logging)
- **Surge protection** via `WaitingRoomFilter` (see [Section 11](#11-surge-protection-virtual-waiting-room))
- Forwards `X-City-ID` header for geo-shard routing
- Currently runs a no-op `AuthenticationFilter` (guest booking model)

**Routing Table:**

| Path Pattern          | Downstream Service     |
| --------------------- | ---------------------- |
| `/api/v1/movies/**`   | `movie-service:8085`   |
| `/api/v1/cities/**`   | `movie-service:8085`   |
| `/api/v1/theaters/**` | `movie-service:8085`   |
| `/api/v1/shows/**`    | `movie-service:8085`   |
| `/layouts/**`         | `movie-service:8085`   |
| `/api/v1/bookings/**` | `booking-service:8083` |
| `/api/v1/seats/**`    | `booking-service:8083` |

### 2.2 Movie Service (`movie-service` — Port 8085)

**Responsibilities:**

- Movie catalog (CRUD, search, featured movies)
- City and theater management
- Show schedule management
- Elasticsearch integration for movie search
- **Static layout generation** — exports screen seat layouts as JSON files (see [Section 10](#10-static-asset-decoupling-cdn-layout))

**Key Endpoints:**

| Method | Endpoint                              | Description                          |
| ------ | ------------------------------------- | ------------------------------------ |
| GET    | `/api/v1/movies`                      | Paginated movie listing              |
| GET    | `/api/v1/movies/{id}`                 | Movie details                        |
| GET    | `/api/v1/movies/city/{cityId}`        | Movies playing in a city             |
| GET    | `/api/v1/movies/search?q=`            | Full-text movie search               |
| GET    | `/api/v1/movies/featured`             | Featured/trending movies             |
| GET    | `/api/v1/movies/{id}/shows`           | Shows for a movie (by date/city)     |
| GET    | `/api/v1/shows/{id}`                  | Show details                         |
| GET    | `/api/v1/cities`                      | All active cities                    |
| GET    | `/api/v1/cities/{cityId}/theaters`    | Theaters in a city                   |
| GET    | `/layouts/screen-{screenId}.json`     | Static seat layout (CDN-served)      |
| POST   | `/api/v1/layouts/generate`            | Trigger layout regeneration          |
| POST   | `/api/v1/layouts/generate/{screenId}` | Regenerate one screen layout         |

### 2.3 Booking Service (`booking-service` — Port 8083)

The **most critical service** — handles seat locking, booking creation, confirmation, and cancellation with full concurrency control and a **layered Redis cache**.

**Responsibilities:**

- Seat availability queries (with read-through Redis Hash cache)
- Lightweight seat status endpoint (for CDN-decoupled flow)
- Distributed seat locking (Redisson + Redis)
- Booking creation with PENDING status
- Booking confirmation with guest details
- Booking cancellation and seat release
- Write-through cache updates on every seat mutation
- Lock expiry management (scheduled job)
- Kafka event publishing

**Key Endpoints:**

| Method | Endpoint                                   | Description                           |
| ------ | ------------------------------------------ | ------------------------------------- |
| GET    | `/api/v1/seats/show/{showId}`              | All seats with layout + cached status |
| GET    | `/api/v1/seats/show/{showId}/availability` | Available seat count                  |
| GET    | `/api/v1/seats/status/{showId}`            | Lightweight status-only (CDN flow)    |
| POST   | `/api/v1/bookings/lock`                    | Lock seats + create booking           |
| POST   | `/api/v1/bookings/confirm`                 | Confirm booking with guest info       |
| POST   | `/api/v1/bookings/{id}/cancel`             | Cancel a booking                      |
| GET    | `/api/v1/bookings/{id}`                    | Get booking by UUID                   |
| GET    | `/api/v1/bookings/number/{bookingNumber}`  | Get booking by human-readable number  |

### 2.4 Notification Service (`notification-service` — Port 8086)

**Responsibilities:**

- Consumes Kafka events from the booking service
- Creates notification records in the database
- Sends confirmation/cancellation emails via SMTP
- SMS support via Twilio (configurable, disabled by default)

**Kafka Topics Consumed:**

| Topic               | Trigger                        |
| -------------------- | ------------------------------ |
| `booking.confirmed`  | Booking successfully confirmed |
| `booking.cancelled`  | Booking cancelled              |

---

## 3. Database Design

### 3.1 Schema Overview

A single PostgreSQL 15 database (`bookmyshow`) shared by all services, managed through **6 Flyway migrations** (V1–V6). When geo-sharding is enabled, a second database (`bookmyshow_south`) provides regional data isolation.

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│    cities     │◄─────│   theaters   │◄─────│   screens    │
│  (25 rows)   │ 1:N  │  (12 rows)   │ 1:N  │  (18 rows)   │
└──────────────┘      └──────────────┘      └──────┬───────┘
                                                   │ 1:N
                                             ┌─────▼──────┐
                                             │    seats    │
                                             │(2,275 rows) │ "Template"
                                             └─────┬──────┘
                                                   │
                      ┌──────────────┐        (referenced by)
                      │    movies    │             │
                      │  (30 rows)  │         ┌───▼──────────┐
                      └──────┬──────┘         │  show_seats   │
                             │ 1:N            │(188,475 rows) │ "Per-show instances"
                      ┌──────▼──────┐         └───┬──────────┘
                      │    shows    │◄────────────┘
                      │ (1,470 rows)│ 1:N
                      └──────┬──────┘
                             │
                             │(show_id on booking)
                      ┌──────▼──────┐      ┌──────────────────┐
                      │  bookings   │──────│  booking_seats    │
                      │  (25 rows)  │ 1:N  │   (45 rows)      │
                      └──────┬──────┘      └──────────────────┘
                             │
                      ┌──────▼──────┐
                      │notifications│
                      │  (18 rows)  │
                      └─────────────┘

                      ┌─────────────┐      ┌──────────────┐
                      │   reviews   │      │   coupons    │
                      │  (81 rows)  │      │  (12 rows)   │
                      └─────────────┘      └──────────────┘

                      ┌─────────────┐
                      │  audit_log  │
                      │  (13 rows)  │
                      └─────────────┘
```

### 3.2 Key Tables — Detailed

#### `seats` — The Template Table

Defines the **physical layout** of a cinema screen. Each row represents a fixed seat in a screen.

| Column          | Type      | Description                          |
| --------------- | --------- | ------------------------------------ |
| `id`            | UUID (PK) | Primary key                          |
| `screen_id`     | UUID (FK) | Which screen this seat belongs to    |
| `row_name`      | VARCHAR   | Row label (A, B, C, …, K)           |
| `seat_number`   | INT       | Seat number within the row (1–25)    |
| `column_number` | INT       | Physical column for layout rendering |
| `seat_type`     | ENUM      | `REGULAR`, `PREMIUM`, or `RECLINER`  |
| `is_active`     | BOOLEAN   | Whether the seat is bookable         |

Seats are organized by screen with a specific layout convention:

- **Rows A–C**: `RECLINER` (front rows, fewest seats, highest price)
- **Rows D–F**: `PREMIUM` (middle rows)
- **Rows G–K**: `REGULAR` (back rows, most seats, lowest price)

#### `show_seats` — Per-Show Seat Instances

Created for **every seat for every show**. This is the high-volume table (~188K rows for 1,470 shows).

| Column      | Type          | Description                            |
| ----------- | ------------- | -------------------------------------- |
| `id`        | UUID (PK)     | Primary key                            |
| `show_id`   | UUID (FK)     | Which show this instance belongs to    |
| `seat_id`   | UUID (FK)     | Reference to the template seat         |
| `status`    | ENUM          | `AVAILABLE`, `LOCKED`, `BOOKED`        |
| `price`     | DECIMAL(10,2) | Price for this seat in this show       |
| `locked_by` | VARCHAR       | Lock token UUID (when status = LOCKED) |
| `locked_at` | TIMESTAMP     | When the lock was acquired             |
| `version`   | BIGINT        | **Optimistic locking version counter** |

> **Design Rationale:** Separating `seats` (template) from `show_seats` (instances) allows the same physical seat to have different prices and statuses across shows. It also enables the CDN layout decoupling — the template (`seats`) data is static and cacheable, while `show_seats` status is dynamic.

#### `bookings` — Guest Booking Records

| Column            | Type          | Description                                    |
| ----------------- | ------------- | ---------------------------------------------- |
| `id`              | UUID (PK)     | Primary key                                    |
| `booking_number`  | VARCHAR       | Human-readable (`BMS-XXXXXXXX`)                |
| `show_id`         | UUID (FK)     | Which show was booked                          |
| `status`          | ENUM          | `PENDING`, `CONFIRMED`, `CANCELLED`, `EXPIRED` |
| `total_amount`    | DECIMAL(10,2) | Base total of all seats                        |
| `convenience_fee` | DECIMAL(10,2) | 4.5% service charge                            |
| `final_amount`    | DECIMAL(10,2) | `total_amount + convenience_fee`               |
| `guest_name`      | VARCHAR       | Guest name (set on confirm)                    |
| `guest_email`     | VARCHAR       | Guest email (set on confirm)                   |
| `guest_phone`     | VARCHAR       | Guest phone (set on confirm)                   |
| `lock_token`      | VARCHAR       | UUID linking to Redis lock                     |
| `expires_at`      | TIMESTAMP     | Lock expiry time                               |
| `version`         | BIGINT        | Optimistic locking version                     |

#### `booking_seats` — Snapshot of Booked Seats

Captures seat details **at the time of booking** so they remain consistent even if the seat template changes later.

| Column         | Type          | Description                     |
| -------------- | ------------- | ------------------------------- |
| `id`           | UUID (PK)     | Primary key                     |
| `booking_id`   | UUID (FK)     | Parent booking                  |
| `show_seat_id` | UUID (FK)     | The show_seat that was booked   |
| `seat_row`     | VARCHAR       | Row name snapshot (e.g., "F")   |
| `seat_number`  | INT           | Seat number snapshot (e.g., 12) |
| `seat_type`    | VARCHAR       | Type snapshot (e.g., "PREMIUM") |
| `price`        | DECIMAL(10,2) | Price snapshot                  |

### 3.3 Indexes & Partitions

**Key Indexes (V2 migration):**

```sql
-- Fast seat lookups for a show
CREATE INDEX idx_show_seats_show_id ON show_seats(show_id);
CREATE INDEX idx_show_seats_status ON show_seats(show_id, status);

-- Fast show lookups by movie and date
CREATE INDEX idx_shows_movie_date ON shows(movie_id, show_date);
CREATE INDEX idx_shows_screen_date ON shows(screen_id, show_date);

-- Booking lookups
CREATE INDEX idx_bookings_number ON bookings(booking_number);
CREATE INDEX idx_bookings_show ON bookings(show_id);
CREATE INDEX idx_bookings_status ON bookings(status);

-- Lock expiry queries (partial index — only LOCKED seats)
CREATE INDEX idx_show_seats_locked ON show_seats(status, locked_at)
    WHERE status = 'LOCKED';
CREATE INDEX idx_bookings_expires ON bookings(expires_at)
    WHERE status = 'PENDING';
```

**Table Partitioning (V3 migration):**

- `shows` table is partitioned by `show_date` (range partitioning) for efficient date-based queries
- Partition per month, automatically pruning old data

---

## 4. Concurrent Seat Booking — The Core Problem

### The Challenge

When a popular movie opens for booking, **hundreds of users may try to book the same seats simultaneously**. The system must guarantee:

1. **No double-booking:** A seat can only be sold to one person
2. **Fair locking:** The first user to request a seat gets it
3. **No deadlocks:** Users cannot permanently block seats
4. **Graceful degradation:** If a user abandons their booking, seats are released automatically

### The Solution: Three-Layer Concurrency Control

The system uses a **defense-in-depth** approach with three independent layers:

```
Layer 1: Redisson Distributed Lock (Mutex)
├── Scope: Per-show (all seat operations for a show are serialized)
├── Duration: 10-second lease, 5-second wait timeout
├── Purpose: Prevents race conditions during seat status check + update
│
Layer 2: PostgreSQL Optimistic Locking (@Version)
├── Scope: Per-row (each show_seat has a version counter)
├── Mechanism: UPDATE ... WHERE version = ? → fails if version changed
├── Purpose: Catches any concurrent modifications that slip past Layer 1
│
Layer 3: Redis TTL (Automatic Expiry)
├── Scope: Per-lock-token (each booking gets a unique token in Redis)
├── Duration: 8 minutes (configurable)
├── Purpose: Guarantees locks are released even if the service crashes
```

### Why Three Layers?

| Scenario                                       | Layer 1 Handles? | Layer 2 Handles? | Layer 3 Handles? |
| ---------------------------------------------- | :--------------: | :--------------: | :--------------: |
| Two users click "Book" at the same millisecond |        ✅        |        —         |        —         |
| Distributed lock fails to acquire (Redis down) |        ❌        |        ✅        |        —         |
| User closes browser without confirming         |        —         |        —         |        ✅        |
| Service crashes while holding lock             |        —         |        —         |        ✅        |
| Network partition between service and Redis    |        —         |        ✅        |        ✅        |
| Two service instances race on same seat        |        ✅        |        ✅        |        —         |

---

## 5. Booking Lifecycle — Step by Step

The booking process has **three distinct phases**, with a clear state machine:

```
  ┌─────────┐     Lock Seats     ┌─────────┐    Confirm     ┌───────────┐
  │AVAILABLE │────────────────────►│ PENDING │───────────────►│ CONFIRMED │
  └─────────┘                    └────┬────┘                └───────────┘
                                      │                           │
                                      │ Timeout / Cancel          │ Cancel
                                      ▼                           ▼
                                 ┌─────────┐                ┌───────────┐
                                 │ EXPIRED │                │ CANCELLED │
                                 └─────────┘                └───────────┘
```

### Phase 1: Seat Locking (`POST /api/v1/bookings/lock`)

**Input:** `{ showId, seatIds[] }` (max 10 seats)

**What Happens:**

```
1. Validate: seatIds.length ≤ 10
2. Call SeatLockService.lockSeats(showId, seatIds)
   ├── 2a. Acquire Redisson distributed lock: "show:lock:{showId}"
   │       (wait up to 5 seconds, lease for 10 seconds)
   ├── 2b. Fetch requested ShowSeat entities from DB
   ├── 2c. Verify ALL seats have status = AVAILABLE
   │       └── If any unavailable → throw SeatUnavailableException (HTTP 409)
   ├── 2d. Generate lockToken = UUID.randomUUID()
   ├── 2e. Set each seat: status=LOCKED, lockedAt=now()
   ├── 2f. Batch save to DB (triggers version increment via @Version)
   ├── 2g. Store in Redis: key="seat:lock:token:{lockToken}"
   │       value="{showId}|{seatId1},{seatId2},..."
   │       TTL = 8 minutes
   ├── 2h. Write-through: update Redis Hash cache (seat status → LOCKED)
   └── 2i. Release Redisson distributed lock (finally block)
3. Fetch seat layout info (row_name, seat_number, seat_type) via JOIN query
4. Calculate pricing:
   ├── totalAmount = sum of all seat prices
   ├── convenienceFee = totalAmount × 0.045 (4.5%)
   └── finalAmount = totalAmount + convenienceFee
5. Create Booking entity:
   ├── status = PENDING
   ├── bookingNumber = "BMS-" + timestamp + "-" + 6-char random
   ├── lockToken, expiresAt = now() + 8 minutes
   └── totalAmount, convenienceFee, finalAmount
6. Create BookingSeat entities (snapshot of each seat's row/number/type/price)
7. Return: { bookingId, lockToken, expiresAt, totalAmount, convenienceFee,
             finalAmount, seatCount, bookingNumber }
```

**Frontend Response:** Shows a countdown timer (8 minutes) and a guest details form.

### Phase 2: Booking Confirmation (`POST /api/v1/bookings/confirm`)

**Input:** `{ bookingId, lockToken, guestName, guestEmail, guestPhone }`

**What Happens:**

```
1. Fetch booking by bookingId
   └── If not found → throw ResourceNotFoundException
2. Verify booking.status == PENDING
   └── If not → throw IllegalStateException
3. Verify request.lockToken matches booking.lockToken
   └── If mismatch → throw InvalidLockTokenException (HTTP 403)
4. Check booking.expiresAt > now()
   └── If expired → throw BookingExpiredException (HTTP 410)
5. Validate lock is still alive in Redis
   └── Call seatLockService.validateLockToken(lockToken)
   └── If Redis key gone → throw BookingExpiredException
6. Update all linked ShowSeats: status = BOOKED
7. Write-through: update Redis Hash cache (seat status → BOOKED)
8. Save guest details on booking:
   ├── guestName, guestEmail, guestPhone
   └── status = CONFIRMED
9. Delete lock token from Redis (no longer needed)
10. Publish Kafka event to "booking.confirmed" topic
11. Return BookingResponse with all booking details
```

### Phase 3a: Cancellation (`POST /api/v1/bookings/{id}/cancel`)

```
1. Fetch booking by ID
2. If status == PENDING:
   ├── Release seat locks via SeatLockService
   │   (sets status=AVAILABLE in DB + deletes Redis key + write-through cache update)
   └── Set booking.status = CANCELLED
3. If status == CONFIRMED:
   ├── Fetch associated ShowSeats
   ├── Set each seat status = AVAILABLE, clear lockedBy/lockedAt
   ├── Write-through: update Redis Hash cache (seat status → AVAILABLE)
   └── Set booking.status = CANCELLED
4. Publish Kafka event to "booking.cancelled" topic
5. Return updated BookingResponse
```

### Phase 3b: Automatic Expiry (Scheduled Job)

```
Every 60 seconds, LockExpiryJob runs:
1. Find show_seats WHERE status='LOCKED' AND locked_at < (now - 8 minutes)
   ├── Collect affected showIds
   ├── Bulk UPDATE: set status='AVAILABLE', clear lockedBy/lockedAt
   └── Evict Redis Hash cache for each affected showId
2. Find bookings WHERE status='PENDING' AND expires_at < now
   └── Set status = EXPIRED
```

---

## 6. Distributed Locking Deep Dive

### 6.1 Redisson Configuration

```java
@Bean
public RedissonClient redissonClient() {
    Config config = new Config();
    config.useSingleServer()
        .setAddress("redis://host:6379")
        .setConnectionMinimumIdleSize(5)
        .setConnectionPoolSize(10)
        .setRetryAttempts(3)
        .setRetryInterval(1500);
    return Redisson.create(config);
}
```

### 6.2 Lock Acquisition Flow

```java
RLock lock = redissonClient.getLock("show:lock:" + showId);

boolean acquired = lock.tryLock(
    5,    // Wait up to 5 seconds for the lock
    10,   // Auto-release after 10 seconds (lease)
    TimeUnit.SECONDS
);

if (!acquired) {
    throw new SeatUnavailableException("High demand. Try again.");
}

try {
    // CRITICAL SECTION — only one thread at a time per show
    // 1. Check seats are AVAILABLE
    // 2. Mark seats as LOCKED
    // 3. Store lock token in Redis
    // 4. Write-through: update seat cache
} finally {
    lock.unlock();
}
```

### 6.3 Redis Data Structures

```
Key:   show:lock:{showId}               → Redisson internal (distributed mutex)
Key:   seat:lock:token:{lockToken}      → "{showId}|{seatId1},{seatId2},...}"
TTL:   480 seconds (8 minutes)

Key:   show_seats:{showId}              → Redis Hash (seat cache, see Section 9)
Field: {showSeatId}                     → "{status}:{price}"
TTL:   30 minutes (configurable)

Key:   waiting_room:active_users        → String counter (see Section 11)
Key:   waiting_room:session:{token}     → "1" with TTL (see Section 11)
```

### 6.4 Race Condition Walkthrough

**Scenario:** Users A and B both try to book Seat F-12 for the same show at the same time.

```
Time    User A                              User B
─────   ──────                              ──────
T+0ms   POST /bookings/lock                 POST /bookings/lock
        {showId: X, seatIds: [F12]}         {showId: X, seatIds: [F12]}

T+1ms   Redisson: tryLock("show:X")         Redisson: tryLock("show:X")
        → ACQUIRED ✅                        → WAITING... (up to 5s)

T+2ms   DB Query: F12.status == AVAILABLE ✅
T+3ms   DB Update: F12.status = LOCKED
        Redis SET: token:abc → "X|F12"
        Redis HSET: show_seats:X F12 "LOCKED:250.00"

T+5ms   lock.unlock()
        → Returns {lockToken: "abc"}

T+6ms                                       → ACQUIRED ✅
T+7ms                                       DB Query: F12.status == LOCKED ❌
T+8ms                                       → SeatUnavailableException!
                                            → HTTP 409 Conflict
```

---

## 7. Event-Driven Notifications (Kafka)

### 7.1 Producer (Booking Service)

The booking service publishes events **after** a booking state change is committed to the database.

```java
// KafkaProducerConfig — Key settings:
ACKS_CONFIG = "all"              // Wait for all replicas to acknowledge
RETRIES_CONFIG = 3               // Retry up to 3 times on failure
ENABLE_IDEMPOTENCE_CONFIG = true // Prevent duplicate messages on retry
```

**Event Payload (booking.confirmed):**

```json
{
  "bookingId": "uuid",
  "bookingNumber": "BMS-20260210143025-A1B2C3",
  "showId": "uuid",
  "guestName": "John Doe",
  "guestEmail": "john@example.com",
  "totalAmount": 1500.00,
  "finalAmount": 1567.50,
  "seatCount": 3
}
```

### 7.2 Consumer (Notification Service)

```java
@KafkaListener(topics = "booking.confirmed", groupId = "notification-service")
public void handleBookingConfirmed(String message) {
    BookingEvent event = objectMapper.readValue(message, BookingEvent.class);
    // 1. Create Notification record in DB (type=BOOKING_CONFIRMATION)
    // 2. Send confirmation email to event.guestEmail
    // 3. (Optional) Send SMS if Twilio is enabled
}
```

### 7.3 Why Kafka?

| Concern          | Solution                                             |
| ---------------- | ---------------------------------------------------- |
| Reliability      | `acks=all` ensures the message is durably stored     |
| Idempotency      | Idempotent producer prevents duplicate events        |
| Decoupling       | Booking service doesn't need to know about email/SMS |
| Async processing | Email sending doesn't block the booking response     |
| Replay-ability   | Failed notifications can be re-consumed from offset  |

---

## 8. API Gateway & Routing

### 8.1 Architecture

The API Gateway is built on **Spring Cloud Gateway**, which uses Project Reactor and Netty for non-blocking, high-throughput request routing.

**Filter Chain (execution order):**

```
Incoming Request
  │
  ▼ Order -2
┌─────────────────────┐
│  WaitingRoomFilter   │ ← Surge protection (see Section 11)
│  Check Redis counter │    Redirects to /waiting-room if over threshold
└──────────┬──────────┘
           ▼ Order -1
┌─────────────────────┐
│ AuthenticationFilter │ ← No-op pass-through (guest model)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│   Route Predicates   │ ← Path-based routing to downstream services
│   + Load Balancing   │    Forwards all headers including X-City-ID
└──────────┬──────────┘
           ▼
     Downstream Service
```

### 8.2 Authentication Filter (No-Op)

Since this system uses a **guest booking model** (no user accounts), the `AuthenticationFilter` is a pass-through:

```java
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange);  // Pass through — guest model
    }

    @Override
    public int getOrder() { return -1; }
}
```

> **Extension Point:** To add authentication later, implement JWT validation in this filter.

---

## 9. Caching Strategy (Redis)

Redis serves **three distinct roles** in this system:

### 9.1 Seat Availability Cache (Redis Hash — `SeatCacheService`)

The highest-impact optimization. A **read-through / write-through** Redis Hash cache that eliminates ~99% of PostgreSQL reads for the seat-map endpoint.

**Data Structure:**

```
Key:   show_seats:{showId}           (Redis Hash)
Field: {showSeatId}                  (UUID string)
Value: {status}:{price}              (e.g., "AVAILABLE:250.00")
TTL:   30 minutes (configurable via booking.cache.seat-ttl-minutes)
```

**Read Path (Read-Through):**

```
Frontend: GET /api/v1/seats/show/{showId}
                    │
     ┌──────────────▼──────────────┐
     │ BookingService.getShowSeats()│
     │                             │
     │ 1. Fetch layout from DB     │ ← Always needed (seat rows, columns, types)
     │    (JOIN seats + show_seats) │
     │                             │
     │ 2. Check Redis Hash         │
     │    HGETALL show_seats:{id}  │
     │         │                   │
     │    ┌────▼────┐              │
     │    │  HIT?   │              │
     │    └──┬───┬──┘              │
     │   Yes │   │ No              │
     │       │   │                 │
     │  Overlay   Populate         │
     │  cached    cache from       │
     │  status    DB result        │
     │  on DB     HSET + TTL       │
     │  layout                     │
     └──────────────┬──────────────┘
                    │
              Return ShowSeatDTO[]
```

**Write Path (Write-Through):**

Every seat mutation updates the cache immediately:

| Operation            | DB Change          | Cache Action                                        |
| -------------------- | ------------------ | --------------------------------------------------- |
| `lockSeats()`        | status → LOCKED    | `HSET show_seats:{id} {seatId} "LOCKED:price"`     |
| `confirmBooking()`   | status → BOOKED    | `HSET show_seats:{id} {seatId} "BOOKED:price"`     |
| `cancelBooking()`    | status → AVAILABLE | `HSET show_seats:{id} {seatId} "AVAILABLE:price"`  |
| `releaseSeats()`     | status → AVAILABLE | `HSET show_seats:{id} {seatId} "AVAILABLE:price"`  |
| `LockExpiryJob`      | Bulk release       | `DEL show_seats:{id}` (full eviction)               |

**Safety guarantees:**

- Write-through only updates if the hash key already exists (no partial cache creation)
- TTL refreshed on every write (prevents stale reads)
- Cache miss triggers full population from DB
- All cache operations are wrapped in try/catch — failures degrade gracefully to DB reads

### 9.2 Distributed Locking (Redisson)

As described in Section 6, Redis is the backbone of the seat locking mechanism:

- **Distributed mutex locks:** `show:lock:{showId}` — ensures only one thread modifies a show's seats at a time
- **Lock tokens with TTL:** `seat:lock:token:{lockToken}` — maps a booking's lock token to the locked seats, auto-expires after 8 minutes

### 9.3 Movie Service Cache (Spring Cache Manager)

The movie-service uses `@Cacheable` annotations with `RedisCacheManager`:

| Cache Name        | TTL    | Data                 |
| ----------------- | ------ | -------------------- |
| `movies`          | 1 hour | Movie details by ID  |
| `featured-movies` | 15 min | Featured movies list |
| `cities`          | 24 hr  | City list            |
| `theaters`        | 6 hr   | Theater list by city |

### 9.4 Redis Configuration

```yaml
# Booking Service
spring.data.redis:
  host: localhost
  port: 6379
  password: redis123
  timeout: 5000

# Redisson pool settings:
#   min idle: 5 connections
#   max pool: 10 connections
#   retry: 3 attempts, 1.5s interval

# Seat cache TTL
booking.cache.seat-ttl-minutes: 30
```

---

## 10. Static Asset Decoupling (CDN Layout)

### 10.1 The Problem

The traditional seat-map endpoint (`GET /seats/show/{showId}`) returns ~128 rows containing both **static layout data** (row name, seat number, seat type, column position) and **dynamic status data** (AVAILABLE/LOCKED/BOOKED, price). The layout never changes — it's defined by the screen's physical seat arrangement — yet it's re-fetched on every page load.

### 10.2 The Solution: Split Static from Dynamic

```
BEFORE (single endpoint):
  GET /seats/show/{showId}  →  128 rows × (layout + status) ≈ 25 KB

AFTER (two endpoints):
  GET /layouts/screen-{screenId}.json   →  Static layout ≈ 10 KB (cached 24hr by browser)
  GET /seats/status/{showId}            →  Dynamic status ≈ 5 KB  (real-time from Redis)
                                           Total per repeat load: 5 KB (60% reduction)
```

### 10.3 Layout Generation (`LayoutGenerator`)

The `LayoutGenerator` service in movie-service exports screen layouts as static JSON files:

**Trigger schedule:**
- Once at startup (warm cache immediately)
- Daily at 2 AM (pick up any screen changes)
- On-demand via `POST /api/v1/layouts/generate`

**Output format** (`/layouts/screen-{screenId}.json`):

```json
{
  "screenId": "uuid",
  "screenName": "Screen 1",
  "screenType": "IMAX",
  "totalSeats": 128,
  "sections": [
    {
      "type": "RECLINER",
      "rows": {
        "A": [
          { "seatId": "uuid", "number": "1", "column": 1 },
          { "seatId": "uuid", "number": "2", "column": 2 }
        ],
        "B": [ "..." ]
      }
    },
    {
      "type": "PREMIUM",
      "rows": { "D": ["..."], "E": ["..."], "F": ["..."] }
    },
    {
      "type": "REGULAR",
      "rows": { "G": ["..."], "H": ["..."], "I": ["..."], "J": ["..."], "K": ["..."] }
    }
  ]
}
```

### 10.4 Serving & Caching

- **Movie-service** serves static files from `/layouts/**` via `WebMvcConfigurer` with `setCachePeriod(86400)` (24-hour `Cache-Control`)
- **API Gateway** routes `/layouts/**` to movie-service
- **Browser** caches layout JSON indefinitely after first fetch — no network request on subsequent visits
- **Production path:** Replace filesystem serving with S3 + CloudFront CDN

### 10.5 Lightweight Status Endpoint (`SeatStatusResponse`)

The new `GET /api/v1/seats/status/{showId}` returns only dynamic data:

```json
{
  "showId": "uuid",
  "seats": [
    {
      "seatId": "uuid",
      "showSeatId": "uuid",
      "status": "AVAILABLE",
      "price": 250.00
    }
  ]
}
```

This uses the same read-through Redis Hash cache as the full endpoint.

### 10.6 Frontend Merge Flow

```
┌─────────────────────────────────────────────────────────────┐
│ seats/page.tsx — fetchSeats()                               │
│                                                             │
│ 1. Try CDN-decoupled flow:                                  │
│    ├── layoutApi.getScreenLayout(screenId) → ScreenLayout   │
│    ├── showApi.getSeatStatuses(showId)     → SeatStatus[]   │
│    └── mergeLayoutAndStatus(layout, status) → ShowSeat[]    │
│                                                             │
│ 2. On failure → Fallback to:                                │
│    └── showApi.getShowSeats(showId) → ShowSeat[] (full)     │
└─────────────────────────────────────────────────────────────┘
```

The `mergeLayoutAndStatus()` function joins static layout data with dynamic status by matching on `seatId` (the template seat UUID that appears in both the layout JSON and the status response).

---

## 11. Surge Protection (Virtual Waiting Room)

### 11.1 The Problem

When a blockbuster movie opens bookings, thousands of concurrent users can overwhelm backend services, causing cascading failures. Traditional rate limiting drops requests — the waiting room queues them gracefully.

### 11.2 Architecture

```
                     ┌──────────────────────────┐
                     │     API Gateway           │
                     │                           │
Incoming Request ───►│  WaitingRoomFilter         │
                     │  (GlobalFilter, Order -2)  │
                     │         │                  │
                     │    ┌────▼──────────┐       │
                     │    │ Check Redis   │       │
                     │    │ active_users  │       │
                     │    │ counter       │       │
                     │    └───┬──────┬────┘       │
                     │   < max   ≥ max            │
                     │        │      │            │
                     │   ┌────▼──┐  ┌▼──────────┐ │
                     │   │ ADMIT │  │302 REDIRECT│ │
                     │   │ + set │  │/waiting-room│ │
                     │   │session│  └────────────┘ │
                     │   │key   │                  │
                     │   └──────┘                  │
                     └────────────────────────────┘
```

### 11.3 `WaitingRoomFilter` Implementation

**Redis Data Model:**

```
Key:   waiting_room:active_users         → String counter (incremented on admit, decremented on session expiry)
Key:   waiting_room:session:{surgeToken} → "1" with TTL = session-ttl (5 min)
```

**Flow:**

1. Request arrives at the gateway
2. Bypass paths: `/waiting-room`, `/actuator`, `/layouts/`, `/favicon.ico`
3. If request has valid `X-Surge-Token` header with existing session key → refresh TTL, allow through
4. New user → check `ACTIVE_COUNTER_KEY`:
   - If `currentCount >= maxThreshold` → `302 Redirect` to `/waiting-room`
   - If below threshold → create session key in Redis (with TTL), increment counter, attach `X-Surge-Token` header, allow through
5. Counter auto-decrements when session keys expire (via delayed `Mono.delay`)

**Configuration:**

```yaml
waiting-room:
  enabled: true                    # Feature flag
  max-threshold: 5000             # Max concurrent active users
  session-ttl-seconds: 300        # 5-minute session window
```

### 11.4 Waiting Room Frontend (`/waiting-room`)

An animated queue page with:

- **Purple gradient** background with pulsing blur effects
- **Countdown ring** (10 seconds between retries)
- **Auto-retry** — hits `/actuator/health` every 10 seconds to check capacity
- **Manual "Try Now"** button for impatient users
- **Retry counter** showing attempt number
- On success → `router.push("/")`

### 11.5 Safety Properties

| Property             | Implementation                                     |
| -------------------- | -------------------------------------------------- |
| No data loss         | 302 redirect (not 503) — browser retries naturally |
| Session affinity     | `X-Surge-Token` header persists user identity      |
| Auto-recovery        | TTL-based expiry auto-drains the queue             |
| Feature flag         | `waiting-room.enabled=false` disables entirely     |
| Static passthrough   | Layout files and health checks bypass the filter   |

---

## 12. Logical Geo-Sharding

### 12.1 The Problem

At scale, a single PostgreSQL database becomes a bottleneck for read-heavy workloads that are geographically distributed. Indian users in Mumbai don't need to query data for theaters in Delhi, and vice versa.

### 12.2 Architecture

```
Frontend (city=Mumbai)
  │
  │ Axios interceptor attaches: X-City-ID: Mumbai
  │
  ▼
API Gateway (forwards all headers)
  │
  ▼
CityRoutingInterceptor (HandlerInterceptor)
  │
  │ Reads X-City-ID header
  │ Looks up city-region-map: Mumbai → "south"
  │ Sets ThreadLocal: GeoShardingContext.setRegion("south")
  │
  ▼
BookingService / MovieService
  │
  │ JPA calls DataSource.getConnection()
  │
  ▼
CityRoutingDataSource (extends AbstractRoutingDataSource)
  │
  │ determineCurrentLookupKey() → GeoShardingContext.getRegion() → "south"
  │
  ├── "north" → HikariDataSource → bookmyshow (north DB)
  └── "south" → HikariDataSource → bookmyshow_south (south DB)
```

### 12.3 Components

**Shared Module (`com.bookmyshow.shared.geosharding`):**

| Class                    | Role                                                  |
| ------------------------ | ----------------------------------------------------- |
| `GeoShardingContext`     | ThreadLocal holding current region ("north"/"south")  |
| `CityRoutingDataSource`  | Extends `AbstractRoutingDataSource`, reads ThreadLocal |
| `CityRoutingInterceptor` | HandlerInterceptor → reads `X-City-ID`, maps to region, sets ThreadLocal |

**Per-Service (`GeoShardingConfig`):**

| Responsibility               | Implementation                                                 |
| ----------------------------- | -------------------------------------------------------------- |
| Two `DataSourceProperties` beans | `@ConfigurationProperties("geo-sharding.datasource.north/south")` |
| Two `HikariDataSource` beans | Built from north/south properties                               |
| `@Primary DataSource`        | `CityRoutingDataSource` wrapping both                           |
| Interceptor registration     | `WebMvcConfigurer.addInterceptors()` for `/api/**` paths        |

### 12.4 City → Region Mapping

Configured in `application.yml` as a SpEL-parsed map:

| Region    | Cities                                                                                                                |
| --------- | --------------------------------------------------------------------------------------------------------------------- |
| **South** | Mumbai, Bengaluru, Hyderabad, Chennai, Pune, Kochi, Visakhapatnam, Goa, Coimbatore, Thiruvananthapuram, Mysuru         |
| **North** | Delhi, Kolkata, Ahmedabad, Jaipur, Lucknow, Chandigarh, Indore, Bhopal, Nagpur, Surat, Vadodara, Patna, Guwahati, Dehradun |

### 12.5 Feature Flag

```yaml
geo-sharding:
  enabled: ${GEO_SHARDING_ENABLED:false}   # Disabled by default
```

The entire `GeoShardingConfig` is annotated with `@ConditionalOnProperty(name = "geo-sharding.enabled", havingValue = "true")`. When disabled, the normal `spring.datasource` config is used — zero impact on existing behavior.

### 12.6 Frontend Integration

An Axios request interceptor reads the selected city from `localStorage` and attaches it on every API call:

```typescript
apiClient.interceptors.request.use((config) => {
  const cityStr = localStorage.getItem("bms_city");
  if (cityStr) {
    const city = JSON.parse(cityStr);
    config.headers["X-City-ID"] = city.name;
  }
  return config;
});
```

### 12.7 ThreadLocal Safety

The `CityRoutingInterceptor` **always clears** the ThreadLocal in `afterCompletion()`, preventing thread-pool contamination in Tomcat's shared thread pool. If no `X-City-ID` header is present, it defaults to `"north"`.

### 12.8 Production Deployment

For local dev, both shards point to the same Postgres container with different database names. In production:

```
geo-sharding.datasource.north.url = jdbc:postgresql://pg-north.ap-north-1.rds.amazonaws.com:5432/bookmyshow
geo-sharding.datasource.south.url = jdbc:postgresql://pg-south.ap-south-1.rds.amazonaws.com:5432/bookmyshow
```

Each regional cluster runs its own Flyway migrations, has its own HikariCP pool, and can be scaled independently.

---

## 13. Scheduled Jobs & Background Processes

### 13.1 Lock Expiry Job

**Schedule:** Every 60 seconds
**Class:** `LockExpiryJob`

```
Purpose: Safety net for abandoned bookings

Step 1: Database sweep
  → Find show_seats WHERE status='LOCKED'
    AND locked_at < (now - timeout_minutes)
  → Collect affected showIds
  → Bulk UPDATE: status='AVAILABLE', lockedBy=null, lockedAt=null
  → Evict Redis Hash cache for each affected show

Step 2: Booking expiry
  → Find bookings WHERE status='PENDING'
    AND expires_at < now
  → Set status = 'EXPIRED'
```

**Why is this needed alongside Redis TTL?**

| Failure Mode                     | Redis TTL Handles? | DB Job Handles? |
| -------------------------------- | :----------------: | :-------------: |
| Normal lock expiry               |         ✅         |       ✅        |
| Redis crashes/loses data         |         ❌         |       ✅        |
| Service crash mid-booking        |         ❌         |       ✅        |
| Redis TTL and DB get out of sync |         —          | ✅ (reconciles) |

### 13.2 Layout Generation Job

**Schedule:** Startup + Daily at 2 AM
**Class:** `LayoutGenerator`

```
Purpose: Export static seat layout JSON files

→ For each active screen:
  → Query seats table (grouped by type, ordered by row/column)
  → Write /layouts/screen-{screenId}.json
  → Log success/failure
```

### 13.3 Show Activation Job

**Schedule:** Daily at midnight
**Class:** `ShowActivationJob`

```
Purpose: Deactivate past shows

→ Find shows WHERE show_date < today AND is_active = true
→ Set is_active = false
```

---

## 14. Frontend Architecture

### 14.1 Technology Stack

- **Framework:** Next.js 14 (App Router, React Server Components)
- **Language:** TypeScript (strict mode)
- **Styling:** Tailwind CSS with PostCSS
- **State Management:** Zustand (lightweight, no boilerplate)
- **HTTP Client:** Axios (with geo-shard interceptor)
- **Notifications:** react-hot-toast

### 14.2 State Management (Zustand Stores)

**City Store:**

```typescript
{
  selectedCity: City | null,    // Persisted in localStorage as "bms_city"
  cities: City[],
  setSelectedCity(city),        // Saves to localStorage (used by X-City-ID interceptor)
  loadFromStorage(),            // Hydrates on mount
}
```

**Booking Store:**

```typescript
{
  selectedSeats: string[],      // Array of seat IDs (max 10)
  lockToken: string | null,
  bookingId: string | null,
  expiresAt: string | null,
  toggleSeat(seatId),           // Add/remove seat from selection
  setLockInfo(token, id, exp),  // After lock API response
  clearBooking(),               // After confirm/cancel/timeout
}
```

### 14.3 Booking UI Flow

```
┌──────────┐    ┌──────────────┐    ┌────────────┐    ┌──────────┐
│ City     │───►│ Movie List   │───►│ Show Times │───►│ Seat Map │
│ Selector │    │ (by city)    │    │ (by date)  │    │ Selection│
└──────────┘    └──────────────┘    └────────────┘    └────┬─────┘
                                                          │ Lock
                                                    ┌─────▼─────┐
                                                    │ Guest Form│
                                                    │ + Timer   │
                                                    └─────┬─────┘
                                                          │ Confirm
                                                    ┌─────▼─────┐
                                                    │ Success   │
                                                    │ Screen    │
                                                    └───────────┘
```

### 14.4 Seat Map Component

The `SeatMap` component renders a cinema-hall-style seat grid:

- Seats are grouped by **section** (RECLINER → PREMIUM → REGULAR) with section headers showing price
- Each row has a **row label** (A, B, C, …) on the left
- Seats are spaced using `columnNumber` for realistic aisle gaps
- Color-coded by type:
  - 🟡 **Recliner** (premium front rows)
  - 🔵 **Premium** (middle rows)
  - ⬜ **Regular** (back rows)
- Seat states: AVAILABLE (clickable), LOCKED (greyed), BOOKED (greyed)
- Selected seats are highlighted with a distinct color
- Maximum 10 seats per booking (enforced by the store)
- A legend shows seat types and their prices
- Sticky bottom bar shows selected seat count and total price

### 14.5 CDN-Decoupled Seat Loading

The seats page uses a **two-phase loading strategy** with graceful fallback:

```
Phase 1: Try CDN-decoupled flow
  ├── Fetch show details → get screenId
  ├── layoutApi.getScreenLayout(screenId) → Static JSON (browser-cached 24hr)
  ├── showApi.getSeatStatuses(showId) → Dynamic status from Redis cache
  └── mergeLayoutAndStatus() → Combined ShowSeat[]

Phase 2: Fallback (if Phase 1 fails)
  └── showApi.getShowSeats(showId) → Full endpoint (layout + status)
```

On repeat visits, Phase 1 completes with zero network requests for the layout (browser cache hit), making the seat map load ~60% faster.

### 14.6 Waiting Room Page

Located at `/waiting-room`, this page is shown when the `WaitingRoomFilter` detects surge conditions:

- Animated purple gradient background with pulsing blur effects
- Countdown ring (10 seconds) with auto-retry
- Checks `/actuator/health` to detect when capacity is available
- Manual "Try Now" button
- Retry attempt counter
- Auto-redirects to `/` when capacity frees up

### 14.7 Countdown Timer

After seats are locked, an 8-minute countdown timer is displayed:

```
┌─ Timer Bar ─────────────────────────────────────────────┐
│ ████████████████████████████░░░░░░  5:32 remaining     │
└────────────────────────────────────────────────────────┘
```

- Calculated from `expiresAt` returned by the lock API
- On reaching 0, automatically shows error toast, clears booking store, refreshes seat map

---

## 15. Error Handling & Resilience

### 15.1 Exception Hierarchy (Booking Service)

```
Exception
├── SeatUnavailableException         → HTTP 409 Conflict
│   └── includes: unavailableSeatIds[]
├── InvalidLockTokenException        → HTTP 403 Forbidden
├── BookingExpiredException          → HTTP 410 Gone
├── ResourceNotFoundException        → HTTP 404 Not Found
├── ObjectOptimisticLockingFailure   → HTTP 409 Conflict (JPA/Hibernate)
├── MethodArgumentNotValidException  → HTTP 400 Bad Request (validation)
├── IllegalArgumentException         → HTTP 400 Bad Request
└── Exception (catch-all)            → HTTP 500 Internal Server Error
```

### 15.2 Standardized API Response

All responses follow a consistent format:

```json
{
  "success": true,
  "message": "Seats locked successfully",
  "data": { "..." },
  "errorCode": null,
  "details": null
}
```

### 15.3 Resilience Patterns

| Pattern                  | Implementation                                            |
| ------------------------ | --------------------------------------------------------- |
| Optimistic locking       | `@Version` on ShowSeat and Booking entities               |
| Distributed locking      | Redisson RLock with lease timeout                         |
| Read-through cache       | Redis Hash with DB fallback on cache miss                 |
| Write-through cache      | Cache updated after every DB mutation                     |
| Graceful cache failure   | All cache operations wrapped in try/catch; fallback to DB |
| Surge protection         | WaitingRoomFilter caps concurrent users via Redis counter |
| Idempotent messaging     | Kafka producer with `enable.idempotence=true`             |
| Automatic cleanup        | LockExpiryJob runs every 60s + cache eviction             |
| Connection pooling       | HikariCP (max 20 for booking, 20 for movie)               |
| Retry logic              | Redisson client retries (3 attempts, 1.5s interval)       |
| Graceful timeout         | Lock wait timeout of 5s prevents indefinite blocking      |
| TTL-based expiry         | Redis keys auto-delete after 8 minutes                    |
| Stateless services       | No server-side sessions; all state in DB/Redis            |
| Feature flags            | Geo-sharding and waiting room can be disabled independently |

---

## 16. Infrastructure & DevOps

### 16.1 Docker Compose Stack

The full development environment is orchestrated via `docker-compose.yml`:

| Service         | Image                         | Port  | Purpose                          |
| --------------- | ----------------------------- | ----- | -------------------------------- |
| `postgres`      | postgres:15-alpine            | 5432  | Primary database (+ south shard) |
| `redis`         | redis:7-alpine                | 6379  | Cache + locks + waiting room     |
| `zookeeper`     | confluentinc/cp-zookeeper:7.5 | 2181  | Kafka coordination               |
| `kafka`         | confluentinc/cp-kafka:7.5     | 9092  | Message broker                   |
| `elasticsearch` | elasticsearch:8.11.0          | 9200  | Movie search                     |
| `kibana`        | kibana:8.11.0                 | 5601  | ES dashboard                     |
| `prometheus`    | prom/prometheus:v2.48.0       | 9090  | Metrics collection               |
| `grafana`       | grafana/grafana:10.2.0        | 3001  | Metrics visualization            |
| `jaeger`        | jaegertracing/all-in-one:1.51 | 16686 | Distributed tracing              |

**Geo-shard initialization:** The `database/init/01-create-shards.sh` script is mounted as `Z99_create-shards.sh` in `docker-entrypoint-initdb.d` — it creates the `bookmyshow_south` database on first container startup, running after all Flyway migration SQL files.

### 16.2 Kubernetes Deployment

The project includes full K8s manifests and Helm charts:

```
k8s/
├── namespace.yaml          # bookmyshow namespace
├── configmap.yaml          # Environment config
├── secrets.yaml            # Credentials (base64)
├── ingress.yaml            # Ingress routing rules
└── deployments/
    ├── api-gateway.yaml
    ├── booking-service.yaml
    ├── movie-service.yaml
    ├── notification-service.yaml
    └── frontend.yaml

helm/bookmyshow/
├── Chart.yaml
├── values.yaml             # Production values
├── values-dev.yaml         # Development overrides
└── templates/              # Helm templates
```

### 16.3 Database Migrations (Flyway)

```
V1__initial_schema.sql          # Core tables (cities, theaters, screens, seats,
                                #   movies, shows, show_seats, bookings, etc.)
V2__add_indexes.sql             # Performance indexes
V3__add_partitions.sql          # Table partitioning for shows
V4__add_payment_columns.sql     # (Legacy — payment fields)
V5__drop_auth_foreign_keys.sql  # Remove user/auth FK constraints
V6__remove_users_payments_add_guest.sql  # Guest model migration
```

---

## 17. Monitoring & Observability

### 17.1 Metrics (Prometheus + Grafana)

All services expose `/actuator/prometheus` endpoints with metrics:

- **JVM metrics:** Heap usage, GC pauses, thread counts
- **HTTP metrics:** Request count, latency histogram, error rates
- **HikariCP metrics:** Connection pool utilization
- **Redis metrics:** Cache hit/miss rates (logged by SeatCacheService)
- **Custom metrics:** Booking counts, seat lock durations
- **Waiting room metrics:** Active user count (observable via Redis key)

**Prometheus scrape config:**

```yaml
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

### 17.2 Distributed Tracing (Jaeger)

Jaeger traces requests across service boundaries:

```
Frontend → API Gateway → Movie Service    (movie/show queries)
Frontend → API Gateway → Booking Service  (seat lock/confirm)
Booking Service → Kafka → Notification Service  (async events)
```

### 17.3 Logging

All services use structured logging with SLF4J/Logback:

```yaml
logging:
  level:
    com.bookmyshow: DEBUG
    org.springframework.kafka: WARN
    org.redisson: WARN
```

Key log lines for debugging:

```
Cache HIT for show_seats:{showId} (128 seats)
Cache MISS for show_seats:{showId}
Cache write-through for show_seats:{showId} (3 seats updated)
Cache evicted for show_seats:{showId}
Waiting room activated: 5001 active users (threshold: 5000)
Geo-shard routed: cityId=Mumbai → region=south
Seats locked successfully - showId: X, seatCount: 3
Booking confirmed - bookingNumber: BMS-20260210-A1B2C3
```

---

## 18. Scalability Considerations

### 18.1 Horizontal Scaling

| Component          | Scaling Strategy                                                     |
| ------------------ | -------------------------------------------------------------------- |
| API Gateway        | Stateless; scale with load balancer; waiting room counter in Redis   |
| Movie Service      | Stateless; read-heavy, scale freely; layouts on shared filesystem/S3 |
| Booking Service    | Stateless; Redis provides shared cache + locks                       |
| Notification Svc   | Scale consumers (Kafka consumer group handles partition assignment)  |
| PostgreSQL         | Geo-sharded by region; read replicas per shard                       |
| Redis              | Redis Cluster for HA + sharding                                     |
| Kafka              | Add partitions for higher throughput                                 |

### 18.2 Bottleneck Analysis & Mitigations

| Bottleneck                 | Current Limit          | Mitigation                               |
| -------------------------- | ---------------------- | ---------------------------------------- |
| DB reads for seat map      | ~128 rows/show         | Redis Hash cache (99% hit rate)          |
| DB connection pool         | 20 per service         | Increase pool, add read replicas         |
| Redisson lock contention   | 1 thread/show at a time | Short critical section (~5ms)           |
| Concurrent users           | Uncapped               | WaitingRoomFilter (5000 threshold)       |
| Seat map payload size      | ~25KB/request          | CDN layout decoupling (5KB dynamic-only) |
| Geographic latency         | Single DB region       | Geo-sharding to north/south clusters     |
| Kafka throughput           | Single partition       | Multi-partition topics                   |
| Redis connections          | 10 pool size           | Redis Cluster                            |

### 18.3 Data Volumes

| Entity     | Current Volume | Growth Rate                    | Storage Strategy                     |
| ---------- | -------------- | ------------------------------ | ------------------------------------ |
| show_seats | 188,475 rows   | ~128 per show per day          | Partition by show_date + Redis cache |
| bookings   | 25 rows        | Proportional to ticket sales   | Index on status, number              |
| shows      | 1,470 rows     | ~210/day (30 movies × 7 shows) | Deactivated by cron job              |
| seats      | 2,275 rows     | Grows only with new screens    | Static template + CDN layout         |

### 18.4 Performance Optimizations Summary

1. **Redis Hash read-through cache** — eliminates ~99% of Postgres reads for seat availability
2. **Write-through cache** — every mutation (lock/confirm/cancel) updates Redis immediately
3. **CDN-decoupled seat loading** — ~60% payload reduction on repeat seat map views
4. **Native SQL queries** for seat layout (avoids N+1 problem)
5. **Batch operations** for seat locking (single `saveAll()` instead of N saves)
6. **Partial indexes** for lock expiry queries
7. **HikariCP connection pooling** with tuned settings
8. **Browser caching** (24-hour `Cache-Control` on layout files)
9. **Surge protection** prevents backend saturation under extreme load
10. **Geo-sharding** reduces per-shard data volume and cross-region latency

### 18.5 Cache Hit Flow Under Load

```
   Concurrent Users Requesting Seat Map for Same Show
                    │
         ┌──────────────────────┐
         │  First Request (T+0) │
         │  Cache MISS          │
         │  → DB query          │
         │  → Populate Redis Hash│
         │  Response: 25ms      │
         └──────────┬───────────┘
                    │
    ┌───────────────┼────────────────┐
    │               │                │
    ▼               ▼                ▼
Request 2      Request 3        Request N
Cache HIT      Cache HIT        Cache HIT
→ Redis only   → Redis only     → Redis only
Response: 2ms  Response: 2ms    Response: 2ms

Zero Postgres load for subsequent requests (30 min TTL)
```

---

## 19. Security Model

### 19.1 Current Model (Guest Booking)

The system operates as a **guest booking platform** — no user accounts, no authentication required.

| Layer            | Security Posture                                  |
| ---------------- | ------------------------------------------------- |
| API Gateway      | WaitingRoomFilter (surge protection) + no-op auth |
| Backend Services | `SecurityConfig`: all endpoints `permitAll()`     |
| Database         | No `users` table; guest info stored per booking   |
| Frontend         | No login/signup flows; city selection only         |
| Booking Identity | Booking number (`BMS-XXXXXXXX`) serves as receipt |

### 19.2 Existing Security Measures

| Measure                  | Implementation                          |
| ------------------------ | --------------------------------------- |
| CSRF protection          | Disabled (stateless API, no cookies)    |
| Session management       | `STATELESS` (no server-side sessions)   |
| Lock token validation    | UUID-based token verified against Redis |
| Surge protection         | WaitingRoomFilter with Redis counter    |
| Input validation         | `@Valid` + Jakarta Bean Validation      |
| SQL injection prevention | JPA parameterized queries               |
| Seat limit enforcement   | Max 10 seats per booking (server-side)  |
| Header-based routing     | X-City-ID for geo-sharding (read-only)  |

### 19.3 Extension Points for Future Authentication

To add user authentication:

1. Implement JWT validation in `AuthenticationFilter` (replace no-op)
2. Add `@PreAuthorize` annotations on booking endpoints
3. Re-create `users` table with a new migration
4. Add login/signup pages to the frontend
5. Store user ID on bookings (alongside guest fields)

---

## 20. Data Flow Diagrams

### 20.1 Complete Booking Flow with Cache (Sequence Diagram)

```
 Browser          API Gateway      Booking Service    SeatCacheService    Redis           PostgreSQL        Kafka
   │                  │                  │                  │                │                 │               │
   │ GET /seats/show/X│                  │                  │                │                 │               │
   │─────────────────►│                  │                  │                │                 │               │
   │  WaitingRoomFilter│                 │                  │                │                 │               │
   │  ✓ Under threshold│                 │                  │                │                 │               │
   │─────────────────►│─────────────────►│                  │                │                 │               │
   │                  │                  │── SELECT show_seats JOIN seats ──────────────────►│               │
   │                  │                  │◄── ShowSeatDTO[] ────────────────────────────────│               │
   │                  │                  │                  │                │                 │               │
   │                  │                  │──getShowSeatStatuses(X)──────────►│                │               │
   │                  │                  │                  │  HGETALL       │                 │               │
   │                  │                  │                  │  show_seats:X  │                 │               │
   │                  │                  │◄── Cache MISS ───│◄──────────────│                 │               │
   │                  │                  │                  │                │                 │               │
   │                  │                  │──populateCache(X, seats)─────────►│                │               │
   │                  │                  │                  │  HSET bulk     │                 │               │
   │                  │                  │                  │  + EXPIRE 30m  │                 │               │
   │◄─────────────────│◄─────────────────│                  │                │                 │               │
   │                  │                  │                  │                │                 │               │
   │ POST /lock       │                  │                  │                │                 │               │
   │ {showId, seatIds}│                  │                  │                │                 │               │
   │─────────────────►│─────────────────►│                  │                │                 │               │
   │                  │                  │── tryLock(show:X)─────────────────►                │               │
   │                  │                  │◄── ACQUIRED ─────────────────────│                 │               │
   │                  │                  │── SELECT + UPDATE status=LOCKED ────────────────►│               │
   │                  │                  │── SET token:abc ──────────────────►│ TTL=8min      │               │
   │                  │                  │                  │                │                 │               │
   │                  │                  │──updateSeatStatuses(LOCKED)──────►│                │               │
   │                  │                  │                  │  HSET fields   │                 │               │
   │                  │                  │── unlock(show:X) ─────────────────►│               │               │
   │                  │                  │── INSERT booking (PENDING) ──────────────────────►│               │
   │◄─────────────────│◄─────────────────│                  │                │                 │               │
   │ {lockToken, bookingId, expiresAt}   │                  │                │                 │               │
   │                  │                  │                  │                │                 │               │
   │ POST /confirm    │                  │                  │                │                 │               │
   │ {guest details}  │                  │                  │                │                 │               │
   │─────────────────►│─────────────────►│                  │                │                 │               │
   │                  │                  │── GET token:abc ──────────────────►│               │               │
   │                  │                  │◄── VALID ────────────────────────│                 │               │
   │                  │                  │── UPDATE seats status=BOOKED ────────────────────►│               │
   │                  │                  │──updateSeatStatuses(BOOKED)──────►│                │               │
   │                  │                  │── UPDATE booking CONFIRMED ──────────────────────►│               │
   │                  │                  │── DEL token:abc ──────────────────►│               │               │
   │                  │                  │── PUBLISH booking.confirmed ─────────────────────────────────────►│
   │◄─────────────────│◄─────────────────│                  │                │                 │               │
   │ {bookingNumber: "BMS-...", CONFIRMED}                  │                │                 │               │
```

### 20.2 CDN-Decoupled Seat Loading Flow

```
 Browser                       API Gateway              Movie Service         Booking Service       Redis
   │                              │                          │                      │                  │
   │ GET /layouts/screen-S1.json  │                          │                      │                  │
   │ (browser cache MISS)         │                          │                      │                  │
   │─────────────────────────────►│─────────────────────────►│                      │                  │
   │                              │                          │── Read file ──►      │                  │
   │◄─────────────────────────────│◄─────────────────────────│  /layouts/...        │                  │
   │                              │   Cache-Control: 86400   │                      │                  │
   │ Stored in browser cache      │                          │                      │                  │
   │                              │                          │                      │                  │
   │ GET /seats/status/{showId}   │                          │                      │                  │
   │─────────────────────────────►│──────────────────────────────────────────────────►│                │
   │                              │                          │                      │── HGETALL ──────►│
   │                              │                          │                      │◄── Cache HIT ───│
   │◄─────────────────────────────│◄─────────────────────────────────────────────────│                 │
   │                              │                          │                      │                  │
   │ mergeLayoutAndStatus()       │                          │                      │                  │
   │ (client-side join)           │                          │                      │                  │
   │                              │                          │                      │                  │
   │ ── Next page load ──         │                          │                      │                  │
   │                              │                          │                      │                  │
   │ GET /layouts/screen-S1.json  │                          │                      │                  │
   │ (browser cache HIT!)         │  ← No network request!  │                      │                  │
   │                              │                          │                      │                  │
   │ GET /seats/status/{showId}   │                          │                      │                  │
   │─────────────────────────────►│─────────────────────────────────────────────────►│── HGETALL ─────►│
   │◄─────────────────────────────│◄────────────────────────────────────────────────│◄── 2ms ────────│
   │                              │                          │                      │                  │
   │ Total: 1 request, ~5KB      (vs 1 request, ~25KB before)                      │                  │
```

### 20.3 Waiting Room Surge Protection Flow

```
 Browser                    API Gateway                    Redis
   │                            │                            │
   │ GET /api/v1/movies         │                            │
   │───────────────────────────►│                            │
   │                            │── GET active_users ────────►│
   │                            │◄── "5001" ─────────────────│
   │                            │                            │
   │                            │ 5001 ≥ 5000 threshold     │
   │◄── 302 /waiting-room ─────│                            │
   │                            │                            │
   │ Renders waiting room UI    │                            │
   │ (countdown, queue position)│                            │
   │                            │                            │
   │ ─── 10 seconds later ───   │                            │
   │                            │                            │
   │ GET /actuator/health       │                            │
   │───────────────────────────►│                            │
   │                            │── GET active_users ────────►│
   │                            │◄── "4998" ─────────────────│
   │                            │                            │
   │                            │ 4998 < 5000 ✅             │
   │                            │── SET session:{token} ─────►│ TTL=5min
   │                            │── INCR active_users ───────►│
   │◄── 200 OK ────────────────│                            │
   │                            │                            │
   │ router.push("/")           │                            │
```

### 20.4 Geo-Sharding Data Flow

```
 Browser                    API Gateway              Booking Service                    PostgreSQL
   │                            │                         │                               │
   │ Selected city: Mumbai      │                         │                               │
   │ Axios interceptor adds:    │                         │                               │
   │ X-City-ID: Mumbai          │                         │                               │
   │                            │                         │                               │
   │ POST /bookings/lock        │                         │                               │
   │ Header: X-City-ID: Mumbai  │                         │                               │
   │───────────────────────────►│── Forward all headers ─►│                               │
   │                            │                         │                               │
   │                            │                         │ CityRoutingInterceptor:       │
   │                            │                         │ cityId="Mumbai"               │
   │                            │                         │ region=map.get("Mumbai")      │
   │                            │                         │       = "south"               │
   │                            │                         │ GeoShardingContext            │
   │                            │                         │   .setRegion("south")         │
   │                            │                         │                               │
   │                            │                         │ DataSource.getConnection()    │
   │                            │                         │ CityRoutingDataSource         │
   │                            │                         │   .determineCurrentLookupKey()│
   │                            │                         │   → "south"                   │
   │                            │                         │                               │
   │                            │                         │── Query ─────────────────────►│ bookmyshow_south
   │                            │                         │◄── Results ──────────────────│
   │                            │                         │                               │
   │                            │                         │ afterCompletion():            │
   │                            │                         │ GeoShardingContext.clear()    │
   │◄──────────────────────────│◄─────────────────────────│                               │
```

### 20.5 Infrastructure Dependency Graph

```
                         ┌─────────────┐
                         │  Frontend   │
                         │ (Next.js)   │
                         │             │
                         │ ┌─────────┐ │
                         │ │X-City-ID│ │ ← Geo-shard header
                         │ │intercept│ │
                         │ └─────────┘ │
                         └──────┬──────┘
                                │
                         ┌──────▼──────┐
                         │ API Gateway │
                         │             │
                         │ ┌─────────┐ │
                         │ │Waiting  │ │ ← Surge protection
                         │ │Room     │ │
                         │ │Filter   │─────────────►Redis
                         │ └─────────┘ │              │
                         └──┬───────┬──┘              │
                            │       │                 │
          ┌─────────────────▼──┐  ┌─▼──────────────┐  │
          │  Movie Service     │  │Booking Service │  │
          │                    │  │                │  │
          │ ┌────────────────┐ │  │┌──────────────┐│  │
          │ │LayoutGenerator │ │  ││SeatCacheSvc  ││──►Redis (Hash)
          │ └────────────────┘ │  │└──────────────┘│  │
          │ ┌────────────────┐ │  │┌──────────────┐│  │
          │ │GeoShardConfig  │ │  ││SeatLockSvc   ││──►Redis (Redisson)
          │ └────────────────┘ │  │└──────────────┘│  │
          └──┬────────┬────────┘  └─┬──┬───────────┘  │
             │        │             │  │              │
    ┌────────▼──┐  ┌──▼────┐        │  │              │
    │PostgreSQL │  │Elastic│        │  │              │
    │ north ◄───────────────────────┘  │              │
    │ south │   │Search │              │              │
    └───────┘   └───────┘           ┌──▼───┐      ┌───▼──────────────┐
                                    │Kafka │─────►│Notification Svc  │
                                    └──────┘      └──────────────────┘

    Monitoring:  Prometheus ──► Grafana
                 Jaeger (tracing)
```

---

## Appendix A: Configuration Reference

### Seat Lock Timing

| Parameter                | Value  | Config Key                                        |
| ------------------------ | ------ | ------------------------------------------------- |
| Lock timeout             | 8 min  | `booking.seat-lock.timeout-minutes`               |
| Distributed lock wait    | 5 sec  | `booking.seat-lock.distributed-lock-wait-seconds` |
| Distributed lock lease   | 10 sec | `booking.seat-lock.distributed-lock-lease-seconds` |
| Lock expiry job interval | 60 sec | `@Scheduled(fixedRate = 60000)`                   |
| Max seats per booking    | 10     | Hardcoded in `BookingService`                     |
| Convenience fee          | 4.5%   | `booking.convenience-fee-percent`                 |

### Cache Configuration

| Parameter            | Value  | Config Key                       |
| -------------------- | ------ | -------------------------------- |
| Seat cache TTL       | 30 min | `booking.cache.seat-ttl-minutes` |
| Layout browser cache | 24 hr  | `WebConfig.setCachePeriod(86400)` |
| Movie cache TTL      | 1 hr   | CacheConfig `movies`             |
| Featured movies TTL  | 15 min | CacheConfig `featured-movies`    |
| Cities cache TTL     | 24 hr  | CacheConfig `cities`             |
| Theaters cache TTL   | 6 hr   | CacheConfig `theaters`           |

### Waiting Room

| Parameter           | Value | Config Key                         |
| ------------------- | ----- | ---------------------------------- |
| Max threshold       | 5000  | `waiting-room.max-threshold`       |
| Session TTL         | 5 min | `waiting-room.session-ttl-seconds` |
| Enabled             | true  | `waiting-room.enabled`             |
| Retry interval (FE) | 10 sec | Hardcoded in waiting-room page    |

### Geo-Sharding

| Parameter       | Value     | Config Key                           |
| --------------- | --------- | ------------------------------------ |
| Enabled         | false     | `geo-sharding.enabled`               |
| Default region  | north     | `GeoShardingContext.DEFAULT_REGION`  |
| City-region map | 25 cities | `geo-sharding.city-region-map`       |
| North DB URL    | env-based | `geo-sharding.datasource.north.url`  |
| South DB URL    | env-based | `geo-sharding.datasource.south.url`  |

### Connection Pool Settings

| Service          | DB Pool Max | DB Pool Min Idle | Redis Pool Max | Redis Min Idle |
| ---------------- | :---------: | :--------------: | :------------: | :------------: |
| Booking Service  |     20      |        5         |       10       |       5        |
| Movie Service    |     20      |        5         |       —        |       —        |
| Notification Svc |     10      |        3         |       —        |       —        |

### Service Ports

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
| Zookeeper            | 2181  | TCP      |
| Elasticsearch        | 9200  | HTTP     |
| Kibana               | 5601  | HTTP     |
| Prometheus           | 9090  | HTTP     |
| Grafana              | 3001  | HTTP     |
| Jaeger UI            | 16686 | HTTP     |

---

## Appendix B: Redis Key Reference

| Key Pattern                         | Type     | TTL    | Purpose                                |
| ----------------------------------- | -------- | ------ | -------------------------------------- |
| `show:lock:{showId}`                | Redisson | 10 sec | Distributed mutex for seat operations  |
| `seat:lock:token:{lockToken}`       | String   | 8 min  | Maps lock token → showId + seatIds     |
| `show_seats:{showId}`               | Hash     | 30 min | Seat availability cache (status:price) |
| `waiting_room:active_users`         | String   | ~6 min | Counter of concurrent active users     |
| `waiting_room:session:{surgeToken}` | String   | 5 min  | Per-user session key for surge counting |
| `movies::{movieId}`                 | String   | 1 hr   | Cached movie details (Spring Cache)    |
| `featured-movies::*`               | String   | 15 min | Cached featured movie list             |
| `cities::*`                        | String   | 24 hr  | Cached city list                       |
| `theaters::*`                      | String   | 6 hr   | Cached theater list                    |

---

## Appendix C: Key Design Decisions

| Decision                                   | Rationale                                                                        |
| ------------------------------------------ | -------------------------------------------------------------------------------- |
| Guest booking (no auth)                    | Reduces friction; MVP focus on booking flow, not identity management             |
| Separate `seats` and `show_seats`          | Template pattern: seat layout is fixed, but price/status varies per show         |
| Redis Hash for seat cache                  | O(1) field-level updates; no need to deserialize entire cache on every mutation  |
| Read-through + write-through               | Consistent cache: reads fill on miss, writes update immediately                  |
| CDN layout decoupling                      | 60% payload reduction; layout is immutable, status is dynamic                    |
| Waiting room over rate limiting            | Queue users instead of dropping them; better UX for ticket sales                 |
| Geo-sharding via AbstractRoutingDataSource | Spring-native, transparent to JPA/Hibernate; no code changes in services         |
| Feature-flagged geo-sharding               | Zero-risk deployment; disabled by default, enable per environment                |
| ThreadLocal for region context             | Thread-safe, zero-allocation for hot path; cleared in afterCompletion            |
| City name (not UUID) as routing key        | Human-readable; UUIDs are auto-generated and unpredictable                       |
| Redisson over Redis SETNX                  | Redisson provides reentrant locks, automatic lease renewal, and fairness         |
| Kafka over REST for notifications          | Decouples services; email failures don't block booking confirmation              |
| PostgreSQL @Version over SELECT FOR UPDATE | Optimistic locking has better throughput under low contention                    |
| 8-minute lock timeout                      | Long enough to fill in details; short enough to not block popular seats          |
| Booking number (`BMS-XXXXXXXX`)            | Human-readable identifier for guest reference                                    |
| Snapshot seat info in `booking_seats`      | Preserves booking details even if screen layout changes later                    |
| Single DB, multiple services (default)     | Simplicity for MVP; geo-sharding activates per environment when needed           |
| Native SQL for seat layout JOIN            | Avoids N+1 problem; single query fetches seat layout + availability              |
| Cache failures are non-fatal               | try/catch on all Redis ops; degrade gracefully to DB reads                       |
