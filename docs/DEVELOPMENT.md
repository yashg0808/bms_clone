# BookMyShow Clone - Development Guide

## Prerequisites

- **Java 17** (Temurin recommended)
- **Maven 3.9+**
- **Node.js 18+** & npm
- **Docker** & Docker Compose
- **Git**

## Quick Start

### 1. Clone & Setup

```bash
git clone https://github.com/your-org/bookmyshow-clone.git
cd bookmyshow-clone
cp .env.example .env
```

### 2. Start Infrastructure

```bash
docker compose up -d postgres redis kafka zookeeper elasticsearch
```

Wait for all services to be healthy:
```bash
docker compose ps
```

### 3. Build Backend

```bash
# Build shared module first
cd backend/shared
mvn clean install

# Build all services
cd ../user-service && mvn clean package -DskipTests
cd ../movie-service && mvn clean package -DskipTests
cd ../booking-service && mvn clean package -DskipTests
cd ../payment-service && mvn clean package -DskipTests
cd ../notification-service && mvn clean package -DskipTests
cd ../api-gateway && mvn clean package -DskipTests
```

### 4. Run Database Migrations

Flyway runs automatically on service startup. Or manually:
```bash
cd database
# Migrations are in migrations/ folder
# Seeds are in seeds/ folder
```

### 5. Start Backend Services

Run each service in a separate terminal:
```bash
cd backend/user-service && mvn spring-boot:run
cd backend/movie-service && mvn spring-boot:run
cd backend/booking-service && mvn spring-boot:run
cd backend/payment-service && mvn spring-boot:run
cd backend/notification-service && mvn spring-boot:run
cd backend/api-gateway && mvn spring-boot:run
```

Or use Docker Compose:
```bash
docker compose up -d
```

### 6. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:3000

## Service Ports

| Service              | Port |
|----------------------|------|
| API Gateway          | 8080 |
| User Service         | 8081 |
| Movie Service        | 8082 |
| Booking Service      | 8083 |
| Payment Service      | 8084 |
| Notification Service | 8085 |
| Frontend             | 3000 |
| PostgreSQL           | 5432 |
| Redis                | 6379 |
| Kafka                | 9092 |
| Elasticsearch        | 9200 |
| Prometheus           | 9090 |
| Grafana              | 3001 |
| Jaeger               | 16686 |

## Project Structure

```
bms_clone/
├── backend/
│   ├── shared/              # Common DTOs, exceptions, utilities
│   ├── user-service/        # Authentication & user management
│   ├── movie-service/       # Movies, theaters, shows
│   ├── booking-service/     # Seat locking & booking (critical path)
│   ├── payment-service/     # Payment processing (Razorpay/Stripe)
│   ├── notification-service/ # Email/SMS via Kafka consumers
│   └── api-gateway/         # Spring Cloud Gateway + JWT validation
├── frontend/                # Next.js 14 + TypeScript + Tailwind
├── database/                # Flyway migrations & seed data
├── k8s/                     # Kubernetes manifests
├── helm/                    # Helm charts
├── monitoring/              # Prometheus, Grafana configs
├── .github/workflows/       # CI/CD pipelines
└── docs/                    # Documentation
```

## Running Tests

```bash
# Backend unit tests
cd backend/user-service && mvn test
cd backend/booking-service && mvn test

# Frontend
cd frontend && npm run lint && npx tsc --noEmit
```

## Key Architecture Decisions

### Seat Locking (Booking Service)
- **Redis distributed locks** via Redisson with 8-minute TTL
- **Optimistic locking** with `@Version` on ShowSeat entities
- Background job cleans expired locks every 60 seconds
- Maximum 10 seats per booking

### Payment Idempotency
- Unique `idempotencyKey` per payment request
- Redis-backed deduplication with 24-hour TTL
- Strategy pattern for multiple gateways (Razorpay, Stripe)

### Event-Driven Notifications
- Kafka topics: `booking.confirmed`, `booking.cancelled`, `payment.success`, `payment.failed`
- Notification service consumes events and sends email/SMS asynchronously

## Environment Variables

See `.env.example` for all required environment variables. Key ones:

| Variable | Description |
|----------|-------------|
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | Base64-encoded JWT signing key (min 256 bits) |
| `RAZORPAY_KEY_ID` | Razorpay API key |
| `RAZORPAY_KEY_SECRET` | Razorpay secret |
| `STRIPE_SECRET_KEY` | Stripe API key |
