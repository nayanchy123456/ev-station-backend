# EV Charging Station — Backend

A Spring Boot REST API powering a community EV charging platform. It handles multi-role authentication, charger management, bookings, payments, real-time chat via WebSocket, analytics, notifications, and more.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.x |
| Language | Java 17+ |
| Database | MySQL 8+ |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT (BCrypt) |
| Real-time | Spring WebSocket + STOMP + SockJS |
| Caching | Spring Cache (Caffeine / Simple) |
| Email | Spring Mail (SMTP / Gmail) |
| Scheduling | Spring Task Scheduler |
| Build | Maven |

---

## Project Structure

```
src/main/java/com/evstation/ev_charging_backend/
├── config/           # Security, CORS, WebSocket, Cache, Scheduler configs
├── controller/       # REST controllers + analytics sub-package
├── dto/              # Request/response DTOs + analytics DTOs
├── entity/           # JPA entities
├── enums/            # Role, BookingStatus, PaymentStatus, etc.
├── exception/        # Custom exceptions + GlobalExceptionHandler
├── repository/       # Spring Data JPA repositories
├── scheduler/        # Booking & reservation expiry schedulers
├── security/         # JWT filter, JwtUtil, CustomUserDetails
├── service/          # Service interfaces
├── serviceImpl/      # Service implementations
├── specification/    # JPA Specifications (charger filtering)
└── util/             # GeoCoding, MockPayment, PhoneValidator
```

---

## Features

### Authentication & Roles
- Register and login with JWT tokens (1-hour expiry, BCrypt passwords)
- Three roles: `USER`, `HOST`, `ADMIN` plus `PENDING_HOST` (awaiting admin approval)
- PENDING_HOST accounts are blocked at login until approved by an admin
- Phone number validation enforces Nepal format (`8xxxxxxxxx` / `9xxxxxxxxx` or `+977` prefix)

### Charger Management
- Hosts can create, update, and delete EV chargers with images, location, brand, and price per kWh
- Geocoding utility resolves address strings to latitude/longitude
- JPA Specification-based filtering for charger search (location, price, brand, etc.)
- File upload support for charger images (up to 10 MB per file)

### Booking & Reservations
- Users book chargers for time slots; conflict detection prevents double-booking
- Reservation hold system: a slot is temporarily locked for 10 minutes during payment
- Scheduled task auto-expires unpaid reservations
- Booking status lifecycle: `PENDING → ACTIVE → COMPLETED / CANCELLED`

### Payments
- Mock payment processor with configurable failure rate (10% by default)
- Payment initiation creates a reservation hold; processing confirms or fails it
- Refund support with cancellation deadline enforcement (1-hour cutoff)
- Receipts auto-generated on successful payment (stored under `/receipts`)

### Real-time Chat (WebSocket)
- STOMP over SockJS at `/ws`
- JWT validated in `WebSocketAuthInterceptor` on STOMP CONNECT
- User-to-Host, User-to-Admin, and Host-to-Admin conversation types
- Typing indicators, read receipts, unread counts
- User presence tracking (online/offline/away)
- Message search, pagination, and conversation caching (5-minute TTL)

### Notifications
- In-app notifications pushed via WebSocket
- Notification types: booking confirmations, payment updates, cancellations, etc.

### Analytics
Three analytics layers, each with role-scoped endpoints:

| Role | Scope |
|---|---|
| `USER` | Overview, booking history, spending, charging behaviour, ratings |
| `HOST` | Revenue, charger utilisation, bookings, user stats |
| `ADMIN` | Platform-wide overview, all users/hosts, revenue, time analytics, performance |

Analytics results are cached and refreshed on a schedule.

### Admin Panel
- Approve or reject PENDING_HOST registrations
- Manage all users, hosts, and chargers
- Platform-wide booking and revenue reports

### Email Notifications
- Gmail SMTP integration (configurable via environment variables)
- Sends booking confirmations, payment receipts, and status updates

---

## API Endpoints (Summary)

| Prefix | Description |
|---|---|
| `POST /api/auth/register` | Register user/host |
| `POST /api/auth/login` | Login, returns JWT |
| `GET /api/auth/profile` | Get current user profile |
| `GET/POST /api/chargers/**` | Charger CRUD + search |
| `GET/POST /api/bookings/**` | Booking management |
| `POST /api/payments/initiate` | Start payment / reservation hold |
| `POST /api/payments/process` | Confirm payment |
| `POST /api/payments/refund/{id}` | Refund with cancellation check |
| `GET/POST /api/reservations/**` | Reservation management |
| `GET/POST /api/ratings/**` | Submit and view charger ratings |
| `GET /api/receipts/**` | Receipt download |
| `GET/POST /api/chat/**` | Conversation and message REST API |
| `WS /ws` | WebSocket endpoint (STOMP) |
| `GET /api/notifications/**` | Notification list and mark-read |
| `GET /api/dashboard/**` | Role-specific dashboard summaries |
| `GET /api/admin/**` | Admin management endpoints |
| `GET /api/analytics/admin/**` | Admin analytics |
| `GET /api/analytics/host/**` | Host analytics |
| `GET /api/analytics/user/**` | User analytics |
| `GET /uploads/**` | Static file access (charger images) |

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8+

### 1. Database Setup

```sql
CREATE DATABASE ev_charger CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configure `application.properties`

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/ev_charger?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_DB_PASSWORD

# JWT
jwt.secret=YOUR_SECRET_KEY_MIN_32_CHARS
jwt.expiration=3600000

# Email (Gmail)
spring.mail.username=YOUR_GMAIL_ADDRESS
spring.mail.password=YOUR_APP_PASSWORD
```

> **Production tip:** Override sensitive values with environment variables: `spring.datasource.password`, `jwt.secret`, `spring.mail.username`, `spring.mail.password`.

### 3. Build & Run

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

The server starts on **http://localhost:8080**.

Hibernate's `ddl-auto=update` will create/update all tables on first run.

---

## Configuration Reference

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `jwt.expiration` | `3600000` | Token validity in ms (1 hour) |
| `payment.reservation.timeout.minutes` | `10` | Hold duration before expiry |
| `payment.cancellation.deadline.hours` | `1` | Refund cutoff before booking start |
| `payment.mock.failure.rate` | `0.1` | Simulated payment failure probability |
| `spring.websocket.allowed-origins` | `localhost:5173,3000,4200` | CORS origins for WebSocket |
| `cors.allowed.origins` | same as above | CORS origins for REST API |
| `chat.message.max.length` | `5000` | Max characters per chat message |
| `spring.servlet.multipart.max-file-size` | `10MB` | Max image upload size |

---

## Security

- All endpoints except `/api/auth/**`, `/ws/**`, and `/uploads/**` require a valid Bearer JWT
- Passwords are hashed with BCrypt
- CORS is enforced via a custom `CorsFilter` (runs before Spring Security)
- WebSocket connections authenticate via JWT on STOMP CONNECT in `WebSocketAuthInterceptor`
- Sessions are stateless (`SessionCreationPolicy.STATELESS`)

---

## Scheduled Jobs

| Job | Schedule | Purpose |
|---|---|---|
| `ReservationExpiryScheduler` | Every minute | Expires unpaid reservation holds after timeout |
| `BookingStatusScheduler` | Every minute | Auto-completes bookings whose end time has passed |
| `AnalyticsCacheRefreshService` | Configurable | Pre-warms analytics caches |

---

## Logging

Logs are written to `logs/ev-charging-backend.log` (rotated at 10 MB, 30-day history).

Console pattern: `yyyy-MM-dd HH:mm:ss - message`

For production, set `logging.level.root=WARN` to reduce verbosity.
