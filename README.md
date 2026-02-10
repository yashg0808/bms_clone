# 🎬 BookMyShow Clone - Monorepo

A scalable movie ticket booking platform built with microservices architecture, designed for testing concurrent seat booking scenarios.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENTS                                  │
│              (Web / Mobile / Third Party)                         │
└──────────────────────┬───────────────────────────────────────────┘
                       │
                ┌──────▼──────┐
                │  API Gateway │   (Spring Cloud Gateway)
                │   :8080      │
                └──────┬──────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
  ┌─────▼─────┐ ┌─────▼──────┐      │
  │  Movie    │ │  Booking   │      │
  │  Service  │ │  Service   │      │
  │   :8082   │ │   :8083    │      │
  └─────┬─────┘ └─────┬──────┘      │
        │              │              │
        └──────────────┼──────────────┘
                       │
              ┌────────▼──┐    ┌─────────────┐
              │ PostgreSQL │    │    Redis     │
              │   :5432    │    │    :6379     │
              └────────────┘    └─────────────┘
                       │
              ┌────────▼──────┐
              │  Kafka + ZK   │ ──► Notification Service :8085
              │  :9092 :2181  │
              └───────────────┘
                       │
              ┌────────▼──────┐
              │ Elasticsearch │
              │    :9200      │
              └───────────────┘
```

## Tech Stack

| Layer            | Technology                                     |
| ---------------- | ---------------------------------------------- |
| Frontend         | Next.js 14, TypeScript, Tailwind CSS, ShadcnUI |
| Backend          | Java 17, Spring Boot 3.2, Spring Cloud         |
| Database         | PostgreSQL 15, Redis 7                         |
| Search           | Elasticsearch 8                                |
| Messaging        | Apache Kafka 3.6                               |
| Containerization | Docker, Kubernetes, Helm                       |
| CI/CD            | GitHub Actions                                 |
| Monitoring       | Prometheus, Grafana, Jaeger                    |

## Prerequisites

- **Java** 17+ (OpenJDK / Eclipse Temurin)
- **Node.js** 18+ & npm 9+
- **Docker** 24+ & Docker Compose v2
- **Maven** 3.9+ (or use the included Maven Wrapper)
- **Git** 2.40+

## Quick Start

```bash
# 1. Clone the repository
git clone https://github.com/your-org/bookmyshow-clone.git
cd bookmyshow-clone

# 2. Run the setup script
chmod +x scripts/setup.sh
./scripts/setup.sh

# 3. Start infrastructure services
docker-compose up -d

# 4. Start backend services (each in a separate terminal)
cd backend/movie-service && ./mvnw spring-boot:run
cd backend/booking-service && ./mvnw spring-boot:run
cd backend/notification-service && ./mvnw spring-boot:run
cd backend/api-gateway && ./mvnw spring-boot:run

# 5. Start frontend
cd frontend && npm run dev
```

## Services

| Service              | Port | Description                       |
| -------------------- | ---- | --------------------------------- |
| API Gateway          | 8080 | Routes & load balances requests   |
| Movie Service        | 8082 | Movies, theaters, shows           |
| Booking Service      | 8083 | Seat locking & booking management |
| Notification Service | 8085 | Email & SMS notifications         |
| Frontend (Next.js)   | 3000 | Web application                   |

## Key Features

- 🎯 **Distributed Seat Locking** — Redis-based distributed locks via Redisson prevent double-booking
- 🎫 **Guest Booking** — No login required; guests provide name, email, phone at booking time
- 🔍 **Full-Text Search** — Elasticsearch-powered movie & event search
- 📊 **Real-Time Seat Updates** — WebSocket-based live seat availability
- 🔔 **Event-Driven Notifications** — Kafka-powered async email & SMS
- 📈 **Observability** — Prometheus metrics, Grafana dashboards, distributed tracing

## Documentation

- [API Documentation](docs/API.md)
- [Development Guide](docs/DEVELOPMENT.md)
- [Deployment Guide](docs/DEPLOYMENT.md)

## License

MIT License — see [LICENSE](LICENSE) for details.
