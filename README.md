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
10. [Scheduled Jobs & Background Processes](#10-scheduled-jobs--background-processes)
11. [Frontend Architecture](#11-frontend-architecture)
12. [Error Handling & Resilience](#12-error-handling--resilience)
13. [Infrastructure & DevOps](#13-infrastructure--devops)
14. [Monitoring & Observability](#14-monitoring--observability)
15. [Scalability Considerations](#15-scalability-considerations)
16. [Security Model](#16-security-model)
17. [Data Flow Diagrams](#17-data-flow-diagrams)

---

## 1. High-Level Architecture

The system follows a **microservices architecture** with four backend services communicating through REST APIs and Apache Kafka for asynchronous events.

```
                        ┌──────────────────┐
                        │   Next.js Frontend│
                        │   (Port 3000)     │
                        └────────┬─────────┘
                                 │ HTTP
                        ┌────────▼─────────┐
                        │   API Gateway     │
                        │ (Spring Cloud GW) │
                        │   (Port 8080)     │
                        └──┬─────────────┬──┘
                           │             │
              ┌────────────▼──┐    ┌─────▼────────────┐
              │ Movie Service │    │ Booking Service   │
              │  (Port 8085)  │    │   (Port 8083)     │
              └───────┬───────┘    └──┬──────┬────┬────┘
                      │               │      │    │
                 ┌────▼────┐    ┌─────▼──┐ ┌─▼──┐ │
                 │PostgreSQL│    │  Redis │ │Kafka│ │
                 │  (5432)  │◄───(6379)  │ │9092│ │
                 └──────────┘    └────────┘ └─┬──┘ │
                                              │    │
                                    ┌─────────▼────▼──┐
                                    │Notification Svc  │
                                    │   (Port 8085)    │
                                    └──────────────────┘
```

### Technology Stack

| Layer             | Technology                                    |
|-------------------|-----------------------------------------------|
| Frontend          | Next.js 14, TypeScript, Tailwind CSS, Zustand |
| API Gateway       | Spring Cloud Gateway (reactive)               |
| Backend Services  | Java 17, Spring Boot 3.2.1                    |
| Database          | PostgreSQL 15 with Flyway migrations          |
| Cache / Locking   | Redis 7 + Redisson (distributed locks)        |
| Message Broker    | Apache Kafka + Zookeeper                      |
| Search            | Elasticsearch 8                               |
| Monitoring        | Prometheus + Grafana + Jaeger (tracing)       |
| Containerization  | Docker + Docker Compose                       |
| Orchestration     | Kubernetes + Helm                             |

---

## 2. Service Breakdown

### 2.1 API Gateway (`api-gateway` — Port 8080)

The **single entry point** for all client requests. Built on Spring Cloud Gateway (reactive/Netty-based).

**Responsibilities:**
- Routes incoming HTTP requests to downstream microservices
- Cross-cutting concerns (CORS, rate limiting, request logging)
- Currently runs a no-op `AuthenticationFilter` (guest booking model)

**Routing Table:**

| Path Pattern              | Downstream Service   |
|---------------------------|----------------------|
| `/api/v1/movies/**`      | `movie-service:8085` |
| `/api/v1/cities/**`      | `movie-service:8085` |
| `/api/v1/theaters/**`    | `movie-service:8085` |
| `/api/v1/shows/**`       | `movie-service:8085` |
| `/api/v1/bookings/**`    | `booking-service:8083` |
| `/api/v1/seats/**`       | `booking-service:8083` |

### 2.2 Movie Service (`movie-service` — Port 8085)

**Responsibilities:**
- Movie catalog (CRUD, search, featured movies)
- City and theater management
- Show schedule management
- Elasticsearch integration for movie search

**Key Endpoints:**

| Method | Endpoint                            | Description                     |
|--------|-------------------------------------|---------------------------------|
| GET    | `/api/v1/movies`                    | Paginated movie listing         |
| GET    | `/api/v1/movies/{id}`               | Movie details                   |
| GET    | `/api/v1/movies/city/{cityId}`      | Movies playing in a city        |
| GET    | `/api/v1/movies/search?q=`          | Full-text movie search          |
| GET    | `/api/v1/movies/featured`           | Featured/trending movies        |
| GET    | `/api/v1/movies/{id}/shows`         | Shows for a movie (by date/city)|
| GET    | `/api/v1/shows/{id}`                | Show details                    |
| GET    | `/api/v1/cities`                    | All active cities               |
| GET    | `/api/v1/cities/{cityId}/theaters`  | Theaters in a city              |

### 2.3 Booking Service (`booking-service` — Port 8083)

The **most critical service** — handles seat locking, booking creation, confirmation, and cancellation with full concurrency control.

**Responsibilities:**
- Seat availability queries (with layout data)
- Distributed seat locking (Redisson + Redis)
- Booking creation with PENDING status
- Booking confirmation with guest details
- Booking cancellation and seat release
- Lock expiry management (scheduled job)
- Kafka event publishing

**Key Endpoints:**

| Method | Endpoint                                | Description                     |
|--------|-----------------------------------------|---------------------------------|
| GET    | `/api/v1/seats/show/{showId}`           | All seats with layout info      |
| GET    | `/api/v1/seats/show/{showId}/availability` | Available seat count         |
| POST   | `/api/v1/bookings/lock`                 | Lock seats + create booking     |
| POST   | `/api/v1/bookings/confirm`              | Confirm booking with guest info |
| POST   | `/api/v1/bookings/{id}/cancel`          | Cancel a booking                |
| GET    | `/api/v1/bookings/{id}`                 | Get booking by UUID             |
| GET    | `/api/v1/bookings/number/{bookingNumber}` | Get booking by human-readable number |

### 2.4 Notification Service (`notification-service` — Port 8085)

**Responsibilities:**
- Consumes Kafka events from the booking service
- Creates notification records in the database
- Sends confirmation/cancellation emails via SMTP
- SMS support via Twilio (configurable, disabled by default)

**Kafka Topics Consumed:**

| Topic               | Trigger                        |
|----------------------|--------------------------------|
| `booking.confirmed`  | Booking successfully confirmed |
| `booking.cancelled`  | Booking cancelled              |

---

## 3. Database Design

### 3.1 Schema Overview

A single PostgreSQL 15 database (`bookmyshow`) shared by all services, managed through **6 Flyway migrations** (V1–V6).

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

| Column        | Type     | Description                              |
|---------------|----------|------------------------------------------|
| `id`          | UUID (PK)| Primary key                              |
| `screen_id`   | UUID (FK)| Which screen this seat belongs to         |
| `row_name`    | VARCHAR  | Row label (A, B, C, …, K)               |
| `seat_number` | INT      | Seat number within the row (1–25)        |
| `column_number`| INT     | Physical column for layout rendering     |
| `seat_type`   | ENUM     | `REGULAR`, `PREMIUM`, or `RECLINER`      |
| `is_active`   | BOOLEAN  | Whether the seat is bookable             |

Seats are organized by screen with a specific layout convention:
- **Rows A–C**: `RECLINER` (front rows, fewest seats, highest price)
- **Rows D–F**: `PREMIUM` (middle rows)
- **Rows G–K**: `REGULAR` (back rows, most seats, lowest price)

#### `show_seats` — Per-Show Seat Instances

Created for **every seat for every show**. This is the high-volume table (~188K rows for 1,470 shows).

| Column      | Type         | Description                              |
|-------------|--------------|------------------------------------------|
| `id`        | UUID (PK)    | Primary key                              |
| `show_id`   | UUID (FK)    | Which show this instance belongs to      |
| `seat_id`   | UUID (FK)    | Reference to the template seat           |
| `status`    | ENUM         | `AVAILABLE`, `LOCKED`, `BOOKED`          |
| `price`     | DECIMAL(10,2)| Price for this seat in this show         |
| `locked_by` | VARCHAR      | Lock token UUID (when status = LOCKED)   |
| `locked_at` | TIMESTAMP    | When the lock was acquired               |
| `version`   | BIGINT       | **Optimistic locking version counter**   |

> **Design Rationale:** Separating `seats` (template) from `show_seats` (instances) allows the same physical seat to have different prices and statuses across shows.

#### `bookings` — Guest Booking Records

| Column           | Type         | Description                           |
|------------------|--------------|---------------------------------------|
| `id`             | UUID (PK)    | Primary key                           |
| `booking_number` | VARCHAR      | Human-readable (`BMS-XXXXXXXX`)       |
| `show_id`        | UUID (FK)    | Which show was booked                 |
| `status`         | ENUM         | `PENDING`, `CONFIRMED`, `CANCELLED`, `EXPIRED` |
| `total_amount`   | DECIMAL(10,2)| Base total of all seats               |
| `convenience_fee`| DECIMAL(10,2)| 4.5% service charge                   |
| `final_amount`   | DECIMAL(10,2)| `total_amount + convenience_fee`      |
| `guest_name`     | VARCHAR      | Guest name (set on confirm)           |
| `guest_email`    | VARCHAR      | Guest email (set on confirm)          |
| `guest_phone`    | VARCHAR      | Guest phone (set on confirm)          |
| `lock_token`     | VARCHAR      | UUID linking to Redis lock            |
| `expires_at`     | TIMESTAMP    | Lock expiry time                      |
| `version`        | BIGINT       | Optimistic locking version            |

#### `booking_seats` — Snapshot of Booked Seats

Captures seat details **at the time of booking** so they remain consistent even if the seat template changes later.

| Column       | Type         | Description                       |
|--------------|--------------|-----------------------------------|
| `id`         | UUID (PK)    | Primary key                       |
| `booking_id` | UUID (FK)    | Parent booking                    |
| `show_seat_id`| UUID (FK)   | The show_seat that was booked     |
| `seat_row`   | VARCHAR      | Row name snapshot (e.g., "F")     |
| `seat_number`| INT          | Seat number snapshot (e.g., 12)   |
| `seat_type`  | VARCHAR      | Type snapshot (e.g., "PREMIUM")   |
| `price`      | DECIMAL(10,2)| Price snapshot                    |

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

-- Lock expiry queries
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
|------------------------------------------------|:-:|:-:|:-:|
| Two users click "Book" at the same millisecond | ✅ | — | — |
| Distributed lock fails to acquire (Redis down)  | ❌ | ✅ | — |
| User closes browser without confirming          | — | — | ✅ |
| Service crashes while holding lock             | — | — | ✅ |
| Network partition between service and Redis    | — | ✅ | ✅ |
| Two service instances race on same seat        | ✅ | ✅ | — |

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
   ├── 2a. Acquire Redisson distributed lock: "seat:lock:show:{showId}"
   │       (wait up to 5 seconds, lease for 10 seconds)
   ├── 2b. Fetch requested ShowSeat entities from DB
   ├── 2c. Verify ALL seats have status = AVAILABLE
   │       └── If any unavailable → throw SeatUnavailableException (HTTP 409)
   ├── 2d. Generate lockToken = UUID.randomUUID()
   ├── 2e. Set each seat: status=LOCKED, lockedBy=lockToken, lockedAt=now()
   ├── 2f. Batch save to DB (triggers version increment via @Version)
   ├── 2g. Store in Redis: key="seat:lock:token:{lockToken}"
   │       value="{showId}|{seatId1},{seatId2},..."
   │       TTL = 8 minutes
   └── 2h. Release Redisson distributed lock (finally block)
3. Fetch seat layout info (row_name, seat_number, seat_type) via JOIN query
4. Calculate pricing:
   ├── totalAmount = sum of all seat prices
   ├── convenienceFee = totalAmount × 0.045 (4.5%)
   └── finalAmount = totalAmount + convenienceFee
5. Create Booking entity:
   ├── status = PENDING
   ├── bookingNumber = "BMS-" + 8-char uppercase UUID prefix
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
   └── If not found → throw IllegalArgumentException
2. Verify booking.status == PENDING
   └── If not → throw IllegalArgumentException
3. Verify request.lockToken matches booking.lockToken
   └── If mismatch → throw InvalidLockTokenException (HTTP 403)
4. Check booking.expiresAt > now()
   └── If expired → throw BookingExpiredException (HTTP 410)
5. Validate lock is still alive in Redis
   └── Call seatLockService.validateLockToken(lockToken)
   └── If Redis key gone → throw BookingExpiredException
6. Update all linked ShowSeats: status = BOOKED
7. Save guest details on booking:
   ├── guestName, guestEmail, guestPhone
   └── status = CONFIRMED
8. Delete lock token from Redis (no longer needed)
9. Publish Kafka event to "booking.confirmed" topic:
   {
     bookingId, bookingNumber, showId,
     guestName, guestEmail, totalAmount,
     finalAmount, seatCount
   }
10. Return BookingResponse with all booking details
```

### Phase 3a: Cancellation (`POST /api/v1/bookings/{id}/cancel`)

```
1. Fetch booking by ID
2. If status == PENDING:
   ├── Release seat locks via SeatLockService (sets status=AVAILABLE in DB + deletes Redis key)
   └── Set booking.status = CANCELLED
3. If status == CONFIRMED:
   ├── Fetch associated ShowSeats
   ├── Set each seat status = AVAILABLE, clear lockedBy/lockedAt
   └── Set booking.status = CANCELLED
4. Publish Kafka event to "booking.cancelled" topic
5. Return updated BookingResponse
```

### Phase 3b: Automatic Expiry (Scheduled Job)

```
Every 60 seconds, LockExpiryJob runs:
1. Find show_seats WHERE status='LOCKED' AND locked_at < (now - 8 minutes)
   └── Bulk UPDATE: set status='AVAILABLE', clear lockedBy/lockedAt
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
        .setConnectionMinimumIdleSize(5)    // Keep 5 idle connections
        .setConnectionPoolSize(10)          // Max 10 connections
        .setRetryAttempts(3)                // Retry on failure
        .setRetryInterval(1500);            // 1.5s between retries
    return Redisson.create(config);
}
```

### 6.2 Lock Acquisition Flow

```java
// Pseudocode from SeatLockService.lockSeats()
RLock lock = redissonClient.getLock("seat:lock:show:" + showId);

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
} finally {
    lock.unlock();  // Always release, even on exception
}
```

### 6.3 Redis Data Structures

```
Key:   seat:lock:show:{showId}          → Redisson internal (distributed mutex)
Key:   seat:lock:token:{lockToken}      → "{showId}|{seatId1},{seatId2},...}"
TTL:   480 seconds (8 minutes)
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

T+5ms   lock.unlock()
        → Returns {lockToken: "abc"}

T+6ms                                       → ACQUIRED ✅
T+7ms                                       DB Query: F12.status == LOCKED ❌
T+8ms                                       → SeatUnavailableException!
                                            → HTTP 409 Conflict
                                            → "Seats are no longer available"
```

**User B sees:** `"Selected seats are no longer available. Please choose different seats."`  
**User A sees:** A countdown timer and guest details form to complete the booking.

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
  "bookingNumber": "BMS-A1B2C3D4",
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

@KafkaListener(topics = "booking.cancelled", groupId = "notification-service")
public void handleBookingCancelled(String message) {
    BookingEvent event = objectMapper.readValue(message, BookingEvent.class);
    // 1. Create Notification record in DB (type=BOOKING_CANCELLATION)
    // 2. Send cancellation email
}
```

### 7.3 Why Kafka?

| Concern               | Solution                                              |
|------------------------|------------------------------------------------------|
| Reliability            | `acks=all` ensures the message is durably stored     |
| Idempotency            | Idempotent producer prevents duplicate events        |
| Decoupling             | Booking service doesn't need to know about email/SMS |
| Async processing       | Email sending doesn't block the booking response     |
| Replay-ability         | Failed notifications can be re-consumed from offset  |

---

## 8. API Gateway & Routing

### 8.1 Architecture

The API Gateway is built on **Spring Cloud Gateway**, which uses Project Reactor and Netty for non-blocking, high-throughput request routing.

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: movie-service
          uri: http://movie-service:8085
          predicates:
            - Path=/api/v1/movies/**, /api/v1/cities/**, /api/v1/theaters/**, /api/v1/shows/**

        - id: booking-service
          uri: http://booking-service:8083
          predicates:
            - Path=/api/v1/bookings/**, /api/v1/seats/**
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
    public int getOrder() { return -1; }  // Runs first
}
```

> **Extension Point:** To add authentication later, implement JWT validation in this filter. All downstream services already accept all requests via their `SecurityConfig`.

---

## 9. Caching Strategy (Redis)

Redis serves **two distinct roles** in this system:

### 9.1 Distributed Locking (Redisson)

As described in [Section 6](#6-distributed-locking-deep-dive), Redis is the backbone of the seat locking mechanism:

- **Distributed mutex locks:** `seat:lock:show:{showId}` — ensures only one thread modifies a show's seats at a time
- **Lock tokens with TTL:** `seat:lock:token:{lockToken}` — maps a booking's lock token to the locked seats, auto-expires after 8 minutes

### 9.2 Data Caching (RedisTemplate)

The `RedisTemplate` is configured with `StringRedisSerializer` for both keys and values, providing a general-purpose cache layer for:

- Seat availability counts
- Show details (frequently accessed during booking flow)

### 9.3 Redis Configuration

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: redis123

# Redisson pool settings (via RedissonConfig):
#   min idle: 5 connections
#   max pool: 10 connections
#   retry: 3 attempts, 1.5s interval
```

---

## 10. Scheduled Jobs & Background Processes

### 10.1 Lock Expiry Job

**Schedule:** Every 60 seconds  
**Class:** `LockExpiryJob`

```
Purpose: Safety net for abandoned bookings

Step 1: Database sweep
  → Find show_seats WHERE status='LOCKED'
    AND locked_at < (now - timeout_minutes)
  → Bulk UPDATE: status='AVAILABLE', lockedBy=null, lockedAt=null

Step 2: Booking expiry
  → Find bookings WHERE status='PENDING'
    AND expires_at < now
  → Set status = 'EXPIRED'
```

**Why is this needed alongside Redis TTL?**

| Failure Mode               | Redis TTL Handles? | DB Job Handles? |
|---------------------------|:--:|:--:|
| Normal lock expiry         | ✅ | ✅ |
| Redis crashes/loses data  | ❌ | ✅ |
| Service crash mid-booking | ❌ | ✅ |
| Redis TTL and DB get out of sync | — | ✅ (reconciles) |

### 10.2 Show Activation Job

**Schedule:** Daily at midnight (`@Scheduled(cron = "0 0 0 * * *")`)  
**Class:** `ShowActivationJob`

```
Purpose: Deactivate past shows

→ Find shows WHERE show_date < today AND is_active = true
→ Set is_active = false
→ (Prevents booking seats for shows that have already happened)
```

---

## 11. Frontend Architecture

### 11.1 Technology Stack

- **Framework:** Next.js 14 (App Router, React Server Components)
- **Language:** TypeScript (strict mode)
- **Styling:** Tailwind CSS with PostCSS
- **State Management:** Zustand (lightweight, no boilerplate)
- **HTTP Client:** Axios
- **Notifications:** react-hot-toast

### 11.2 State Management (Zustand Stores)

**City Store:**
```typescript
{
  selectedCity: City | null,    // Persisted in localStorage
  cities: City[],
  setSelectedCity(city),        // Saves to localStorage
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

### 11.3 Booking UI Flow

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

### 11.4 Seat Map Component

The `SeatMap` component renders a cinema-hall-style seat grid:

- Seats are grouped by **row** (A, B, C, …) with row labels
- Each seat shows its number and is color-coded by type:
  - 🟡 **Recliner** (premium front rows)
  - 🔵 **Premium** (middle rows)
  - ⬜ **Regular** (back rows)
- Seat states: `AVAILABLE` (clickable), `LOCKED` (greyed, someone else's), `BOOKED` (greyed, sold)
- Selected seats are highlighted
- Maximum 10 seats per booking (enforced by the store)
- A legend shows seat types and their prices
- The component uses data from `ShowSeatDTO` (backend JOIN of `show_seats` + `seats` tables)

### 11.5 Countdown Timer

After seats are locked, an 8-minute countdown timer is displayed:

```
┌─ Timer Bar ─────────────────────────────────────────────┐
│ ████████████████████████████░░░░░░  5:32 remaining     │
└────────────────────────────────────────────────────────┘
```

- Calculated from `expiresAt` returned by the lock API
- On reaching 0, automatically:
  1. Shows error toast: "Booking expired! Seats have been released."
  2. Clears the booking store
  3. Refreshes the seat map

---

## 12. Error Handling & Resilience

### 12.1 Exception Hierarchy (Booking Service)

```
Exception
├── SeatUnavailableException         → HTTP 409 Conflict
│   └── includes: unavailableSeatIds[]
├── InvalidLockTokenException        → HTTP 403 Forbidden
├── BookingExpiredException          → HTTP 410 Gone
├── ObjectOptimisticLockingFailure   → HTTP 409 Conflict (JPA/Hibernate)
├── MethodArgumentNotValidException  → HTTP 400 Bad Request (validation)
├── IllegalArgumentException         → HTTP 400 Bad Request
└── Exception (catch-all)            → HTTP 500 Internal Server Error
```

### 12.2 Standardized API Response

All responses follow a consistent format:

```json
{
  "success": true,
  "message": "Seats locked successfully",
  "data": { ... },
  "errorCode": null,
  "details": null
}
```

Error response:
```json
{
  "success": false,
  "message": "Selected seats are no longer available",
  "data": null,
  "errorCode": "SEAT_UNAVAILABLE",
  "details": {
    "unavailableSeatIds": ["uuid1", "uuid2"]
  }
}
```

### 12.3 Resilience Patterns

| Pattern                 | Implementation                                             |
|-------------------------|------------------------------------------------------------|
| Optimistic locking      | `@Version` on `ShowSeat` and `Booking` entities           |
| Distributed locking     | Redisson `RLock` with lease timeout                       |
| Idempotent messaging    | Kafka producer with `enable.idempotence=true`             |
| Automatic cleanup       | `LockExpiryJob` runs every 60s                            |
| Connection pooling      | HikariCP (max 20 for booking, 20 for movie)               |
| Retry logic             | Redisson client retries (3 attempts, 1.5s interval)       |
| Graceful timeout        | Lock wait timeout of 5s prevents indefinite blocking      |
| TTL-based expiry        | Redis keys auto-delete after 8 minutes                    |
| Stateless services      | No server-side sessions; all state in DB/Redis            |

---

## 13. Infrastructure & DevOps

### 13.1 Docker Compose Stack

The full development environment is orchestrated via `docker-compose.yml`:

| Service         | Image                        | Port  | Purpose                    |
|-----------------|------------------------------|-------|----------------------------|
| `postgres`      | postgres:15-alpine           | 5432  | Primary database           |
| `redis`         | redis:7-alpine               | 6379  | Cache + distributed locks  |
| `zookeeper`     | confluentinc/cp-zookeeper:7.5| 2181  | Kafka coordination         |
| `kafka`         | confluentinc/cp-kafka:7.5    | 9092  | Message broker             |
| `elasticsearch` | elasticsearch:8.11.0         | 9200  | Movie search               |
| `kibana`        | kibana:8.11.0                | 5601  | ES dashboard               |
| `prometheus`    | prom/prometheus:v2.48.0      | 9090  | Metrics collection         |
| `grafana`       | grafana/grafana:10.2.2       | 3001  | Metrics visualization      |
| `jaeger`        | jaegertracing/all-in-one:1.52| 16686 | Distributed tracing        |
| `api-gateway`   | Built from Dockerfile        | 8080  | API routing                |
| `movie-service` | Built from Dockerfile        | 8085  | Movie/show/theater APIs    |
| `booking-service`| Built from Dockerfile       | 8083  | Booking/seat APIs          |
| `notification-service` | Built from Dockerfile | 8086  | Kafka consumer + email     |
| `frontend`      | Built from Dockerfile        | 3000  | Next.js web app            |

### 13.2 Kubernetes Deployment

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
    ├── payment-service.yaml
    └── frontend.yaml

helm/bookmyshow/
├── Chart.yaml
├── values.yaml             # Production values
├── values-dev.yaml         # Development overrides
└── templates/              # Helm templates
```

### 13.3 Database Migrations (Flyway)

```
V1__initial_schema.sql          # Core tables (cities, theaters, screens, seats,
                                #   movies, shows, show_seats, bookings, etc.)
V2__add_indexes.sql             # Performance indexes
V3__add_partitions.sql          # Table partitioning for shows
V4__add_payment_columns.sql     # (Legacy — payment fields)
V5__drop_auth_foreign_keys.sql  # Remove user/auth FK constraints
V6__remove_users_payments_add_guest.sql  # Guest model migration:
                                # - Drop users, payments tables
                                # - Add guest_name, guest_email, guest_phone
                                # - Add PENDING to booking_status enum
                                # - Drop user-related indexes
```

---

## 14. Monitoring & Observability

### 14.1 Metrics (Prometheus + Grafana)

All services expose `/actuator/prometheus` endpoints with metrics:

- **JVM metrics:** Heap usage, GC pauses, thread counts
- **HTTP metrics:** Request count, latency histogram, error rates
- **HikariCP metrics:** Connection pool utilization
- **Custom metrics:** Booking counts, seat lock durations

**Prometheus scrape config:**
```yaml
scrape_configs:
  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api-gateway:8080']

  - job_name: 'movie-service'
    static_configs:
      - targets: ['movie-service:8085']

  - job_name: 'booking-service'
    static_configs:
      - targets: ['booking-service:8083']
```

### 14.2 Distributed Tracing (Jaeger)

Jaeger traces requests across service boundaries:

```
Frontend → API Gateway → Movie Service    (movie/show queries)
Frontend → API Gateway → Booking Service  (seat lock/confirm)
Booking Service → Kafka → Notification Service  (async events)
```

### 14.3 Logging

All services use structured logging with SLF4J/Logback:

```yaml
logging:
  level:
    com.bookmyshow: DEBUG    # Application code
    org.springframework.kafka: WARN  # Reduce Kafka noise
```

### 14.4 Grafana Dashboards

Pre-configured dashboards for:
- Service health overview
- Booking throughput and latency
- Database connection pool utilization
- Kafka consumer lag

---

## 15. Scalability Considerations

### 15.1 Horizontal Scaling

| Component          | Scaling Strategy                                         |
|--------------------|----------------------------------------------------------|
| API Gateway        | Stateless; scale with load balancer                      |
| Movie Service      | Stateless; read-heavy, scale freely                      |
| Booking Service    | Stateless; Redis provides shared state                   |
| Notification Svc   | Scale consumers (Kafka consumer group handles partition assignment) |
| PostgreSQL         | Read replicas for movie queries; single primary for bookings |
| Redis              | Redis Cluster for HA + sharding                          |
| Kafka              | Add partitions for higher throughput                     |

### 15.2 Bottleneck Analysis

```
Bottleneck                    Current Limit          Mitigation
─────────────────────         ─────────────          ──────────
DB connection pool            20 per service         Increase pool, add replicas
Redisson lock contention      1 thread/show at a time  Short critical section (~5ms)
Kafka throughput              Single partition        Multi-partition topics
Redis connections             10 pool size            Redis Cluster
Show_seats table size         ~128 seats/show         Partition by show_date
```

### 15.3 Data Volumes

| Entity       | Current Volume | Growth Rate                   | Storage Strategy        |
|--------------|----------------|-------------------------------|-------------------------|
| show_seats   | 188,475 rows   | ~128 per show per day         | Partition by show_date  |
| bookings     | 25 rows        | Proportional to ticket sales  | Index on status, number |
| shows        | 1,470 rows     | ~210/day (30 movies × 7 shows)| Deactivated by cron job |
| seats        | 2,275 rows     | Grows only with new screens   | Static template table   |

### 15.4 Performance Optimizations

1. **Native SQL queries** for seat layout (avoids N+1 problem):
   ```sql
   SELECT ss.id, ss.show_id, ss.seat_id, ss.status, ss.price,
          s.row_name, s.seat_number, s.seat_type, s.column_number
   FROM show_seats ss
   JOIN seats s ON ss.seat_id = s.id
   WHERE ss.show_id = ?
   ```

2. **Batch operations** for seat locking (single `saveAll()` instead of N saves)

3. **Partial indexes** for lock expiry queries:
   ```sql
   CREATE INDEX idx_show_seats_locked ON show_seats(status, locked_at)
       WHERE status = 'LOCKED';
   ```

4. **HikariCP connection pooling** with tuned min-idle and max-pool settings

---

## 16. Security Model

### 16.1 Current Model (Guest Booking)

The system operates as a **guest booking platform** — no user accounts, no authentication required.

| Layer             | Security Posture                                |
|-------------------|-------------------------------------------------|
| API Gateway       | No-op `AuthenticationFilter` (pass-through)     |
| Backend Services  | `SecurityConfig`: all endpoints `permitAll()`   |
| Database          | No `users` table; guest info stored per booking |
| Frontend          | No login/signup flows; city selection only       |
| Booking Identity  | Booking number (`BMS-XXXXXXXX`) serves as receipt|

### 16.2 Existing Security Measures

| Measure                    | Implementation                              |
|----------------------------|---------------------------------------------|
| CSRF protection            | Disabled (stateless API, no cookies)        |
| Session management         | `STATELESS` (no server-side sessions)       |
| Lock token validation      | UUID-based token verified against Redis     |
| Input validation           | `@Valid` + Jakarta Bean Validation          |
| SQL injection prevention   | JPA parameterized queries                   |
| Seat limit enforcement     | Max 10 seats per booking (server-side)      |

### 16.3 Extension Points for Future Authentication

To add user authentication:
1. Implement JWT validation in `AuthenticationFilter`
2. Add `@PreAuthorize` annotations on booking endpoints
3. Re-create `users` table with a new migration
4. Add login/signup pages to the frontend
5. Store user ID on bookings (alongside guest fields)

---

## 17. Data Flow Diagrams

### 17.1 Complete Booking Flow (Sequence Diagram)

```
 Browser          API Gateway      Booking Service       Redis           PostgreSQL        Kafka          Notification Svc
   │                  │                  │                  │                 │               │                  │
   │ GET /seats/show/X│                  │                  │                 │               │                  │
   │─────────────────►│─────────────────►│                  │                 │               │                  │
   │                  │                  │──── SELECT show_seats JOIN seats ─►│               │                  │
   │                  │                  │◄──── ShowSeatDTO[] ───────────────│               │                  │
   │◄─────────────────│◄─────────────────│                  │                 │               │                  │
   │                  │                  │                  │                 │               │                  │
   │ POST /lock       │                  │                  │                 │               │                  │
   │ {showId, seatIds}│                  │                  │                 │               │                  │
   │─────────────────►│─────────────────►│                  │                 │               │                  │
   │                  │                  │── tryLock(show:X)►│                 │               │                  │
   │                  │                  │◄── ACQUIRED ──────│                 │               │                  │
   │                  │                  │                  │                 │               │                  │
   │                  │                  │── SELECT seats WHERE status=AVAIL ►│               │                  │
   │                  │                  │◄── rows ──────────────────────────│               │                  │
   │                  │                  │                  │                 │               │                  │
   │                  │                  │── UPDATE status=LOCKED ───────────►│               │                  │
   │                  │                  │── SET token:abc ─►│ (TTL=8min)     │               │                  │
   │                  │                  │── unlock(show:X) ►│                 │               │                  │
   │                  │                  │                  │                 │               │                  │
   │                  │                  │── INSERT booking (PENDING) ───────►│               │                  │
   │                  │                  │── INSERT booking_seats ───────────►│               │                  │
   │◄─────────────────│◄─────────────────│                  │                 │               │                  │
   │ {lockToken, bookingId, expiresAt}   │                  │                 │               │                  │
   │                  │                  │                  │                 │               │                  │
   │ ══ 8-min timer ═══════════════════════════════════════════════════════════              │                  │
   │                  │                  │                  │                 │               │                  │
   │ POST /confirm    │                  │                  │                 │               │                  │
   │ {bookingId,      │                  │                  │                 │               │                  │
   │  lockToken,      │                  │                  │                 │               │                  │
   │  guestName,      │                  │                  │                 │               │                  │
   │  guestEmail,     │                  │                  │                 │               │                  │
   │  guestPhone}     │                  │                  │                 │               │                  │
   │─────────────────►│─────────────────►│                  │                 │               │                  │
   │                  │                  │── GET token:abc ─►│                 │               │                  │
   │                  │                  │◄── VALID ────────│                 │               │                  │
   │                  │                  │                  │                 │               │                  │
   │                  │                  │── UPDATE seats status=BOOKED ─────►│               │                  │
   │                  │                  │── UPDATE booking status=CONFIRMED ►│               │                  │
   │                  │                  │── DEL token:abc ─►│                 │               │                  │
   │                  │                  │                  │                 │               │                  │
   │                  │                  │── PUBLISH booking.confirmed ──────────────────────►│                  │
   │                  │                  │                  │                 │               │──── CONSUME ─────►│
   │                  │                  │                  │                 │               │                  │
   │                  │                  │                  │                 │               │    INSERT notif ─►│(DB)
   │                  │                  │                  │                 │               │    Send email     │
   │◄─────────────────│◄─────────────────│                  │                 │               │                  │
   │ {bookingNumber: "BMS-A1B2C3D4", status: CONFIRMED}    │                 │               │                  │
```

### 17.2 Lock Expiry Safety Net

```
                    LockExpiryJob (every 60s)
                           │
              ┌────────────┼────────────┐
              ▼                         ▼
     DB: show_seats                DB: bookings
     WHERE status='LOCKED'         WHERE status='PENDING'
     AND locked_at < (now-8min)    AND expires_at < now
              │                         │
              ▼                         ▼
     UPDATE status='AVAILABLE'     UPDATE status='EXPIRED'
     SET lockedBy=null
     SET lockedAt=null
```

### 17.3 Infrastructure Dependency Graph

```
                    ┌─────────────┐
                    │  Frontend   │
                    │ (Next.js)   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ API Gateway │
                    └──┬───────┬──┘
                       │       │
          ┌────────────▼──┐  ┌─▼────────────┐
          │ Movie Service │  │Booking Service│
          └──┬─────────┬──┘  └─┬──┬──┬──────┘
             │         │       │  │  │
    ┌────────▼──┐  ┌───▼────┐  │  │  │
    │PostgreSQL │  │Elastic │  │  │  │
    │           │◄─────────────┘  │  │
    └───────────┘  │Search  │     │  │
                   └────────┘  ┌──▼──▼──┐
                               │ Redis  │
                               └────────┘
                                  │
                               ┌──▼───┐      ┌──────────────────┐
                               │Kafka │─────►│Notification Svc  │
                               └──────┘      └──────────────────┘

    Monitoring:  Prometheus ──► Grafana
                 Jaeger (tracing)
```

---

## Appendix A: Configuration Reference

### Seat Lock Timing

| Parameter                  | Value    | Config Key                              |
|----------------------------|----------|-----------------------------------------|
| Lock timeout               | 8 min    | `booking.seat-lock.timeout-minutes`     |
| Distributed lock wait      | 5 sec    | `booking.seat-lock.distributed-lock-wait-seconds` |
| Distributed lock lease     | 10 sec   | `booking.seat-lock.distributed-lock-lease-seconds`|
| Lock expiry job interval   | 60 sec   | `@Scheduled(fixedRate = 60000)`         |
| Max seats per booking      | 10       | Hardcoded in `BookingService`           |
| Convenience fee            | 4.5%     | Hardcoded in `BookingService`           |

### Connection Pool Settings

| Service          | DB Pool Max | DB Pool Min Idle | Redis Pool Max | Redis Min Idle |
|------------------|:-----------:|:----------------:|:--------------:|:--------------:|
| Booking Service  | 20          | 5                | 10             | 5              |
| Movie Service    | 20          | 5                | —              | —              |
| Notification Svc | 10          | 3                | —              | —              |

### Service Ports

| Service              | Port  | Protocol |
|----------------------|-------|----------|
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

## Appendix B: Key Design Decisions

| Decision                              | Rationale                                                                 |
|---------------------------------------|---------------------------------------------------------------------------|
| Guest booking (no auth)               | Reduces friction; MVP focus on booking flow, not identity management     |
| Separate `seats` and `show_seats`     | Template pattern: seat layout is fixed, but price/status varies per show |
| Redisson over Redis SETNX             | Redisson provides reentrant locks, automatic lease renewal, and fairness |
| Kafka over REST for notifications     | Decouples services; email failures don't block booking confirmation      |
| PostgreSQL @Version over SELECT FOR UPDATE | Optimistic locking has better throughput under low contention          |
| 8-minute lock timeout                 | Long enough to fill in details; short enough to not block popular seats  |
| Booking number (`BMS-XXXXXXXX`)       | Human-readable identifier for guest reference (no account to look up)   |
| Snapshot seat info in `booking_seats` | Preserves booking details even if screen layout changes later            |
| Single DB, multiple services          | Simplicity for MVP; can split per-service DBs later if needed           |
| Native SQL for seat layout JOIN       | Avoids N+1 problem; single query fetches seat layout + availability     |
