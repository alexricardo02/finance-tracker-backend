# 📊 Finance Tracker — Backend API

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![CI](https://img.shields.io/github/actions/workflow/status/alexricardo02/finance-tracker-backend/maven.yml?style=for-the-badge&label=CI&logo=githubactions&logoColor=white)

A production-grade RESTful API for personal finance tracking, built with a focus on **scalability, security, and performance**. Goes beyond a simple CRUD — implements real-world backend engineering concepts including JWT authentication, Redis caching with per-user isolation, rate limiting, pagination, and a global exception handling strategy.

🌍 **Live Frontend:** [expenses-incomes-frontend.vercel.app](https://expenses-incomes-frontend.vercel.app)

---

## 📌 Table of Contents

- [Features](#-features)
- [System Design](#-system-design)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [API Reference](#-api-reference)
- [Setup & Installation](#-setup--installation)
- [Environment Variables](#-environment-variables)
- [Roadmap](#-roadmap)

---

## ✅ Features

- **JWT Authentication** — Stateless session management with signed tokens
- **Per-user data isolation** — Every query and cache key is scoped by `user_id`
- **Redis Caching** — Heavy aggregations cached with strict eviction on mutations
- **Rate Limiting** — Token Bucket algorithm (Bucket4j) on sensitive endpoints
- **Pagination** — Spring Data `Pageable` on all list endpoints
- **Global Exception Handling** — Consistent error responses via `@ControllerAdvice`
- **Native SQL Aggregations** — `COALESCE(SUM(...))` computed at DB layer, not in memory
- **B-Tree Indexing** — On `user_id`, `date`, and `type` columns for fast lookups
- **CI/CD** — GitHub Actions pipeline with PostgreSQL and Redis service containers
- **Dockerized** — Full Docker + Docker Compose setup for local development

---

## 🏗 System Design

### Caching Strategy
All heavy read operations (monthly totals, date-range aggregations, type breakdowns) are cached in Redis. Cache keys follow the pattern `username_parameter` to guarantee strict data isolation between users.

```
GET /api/incomes/total-month?month=JANUARY
  → Cache MISS  → Query DB → Store in Redis → Return result
  → Cache HIT   → Return from Redis (no DB query)

POST /api/incomes (create new income)
  → @CacheEvict invalidates ALL related cache entries for that user
  → Next read will repopulate from DB
```

### Rate Limiting
The Token Bucket algorithm (via Bucket4j) protects the `/login` endpoint and all `POST` mutation endpoints against brute force and abuse. Each IP gets a fixed number of tokens that refill over time.

### Exception Handling
All exceptions are caught by a single `@ControllerAdvice` class and converted into a standardized JSON response — no stacktraces ever reach the client.

```json
{
  "status": 404,
  "message": "Income not found with ID: 7",
  "timestamp": "2026-05-03T14:30:00"
}
```

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Framework | Java 17, Spring Boot 3 |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL (Neon.tech serverless) |
| Cache | Redis (Upstash serverless) |
| Auth | JWT (jjwt), Spring Security |
| Rate Limiting | Bucket4j |
| Validation | Jakarta Bean Validation |
| DevOps | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Deployment | Render |

---

## 📁 Project Structure

```
src/main/java/com/example/
├── config/
│   ├── SecurityConfig.java        # Spring Security filter chain
│   └── WebConfig.java             # CORS configuration
├── controllers/
│   ├── IncomeController.java
│   ├── ExpenseController.java
│   └── UserController.java
├── service/
│   ├── IncomeService.java         # Business logic + caching
│   ├── ExpenseService.java
│   └── UserService.java
├── repository/
│   ├── IncomeRepository.java      # Native SQL aggregation queries
│   ├── ExpenseRepository.java
│   └── UserRepository.java
├── models/
│   ├── Income.java                # JPA entity with B-Tree indexes
│   ├── Expense.java
│   └── User.java
├── dataTransferObjects/
│   ├── IncomeRequestDTO.java
│   ├── IncomeResponseDTO.java
│   └── ...
├── exceptions/
│   ├── GlobalExceptionHandler.java  # @ControllerAdvice
│   └── ErrorResponse.java           # Standardized error DTO
└── security/
    ├── JwtUtil.java               # Token generation & validation
    └── JwtRequestFilter.java      # Per-request JWT filter
```

---

## 📡 API Reference

### Authentication — `/api/users`

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/users/register` | Register a new user | No |
| `POST` | `/api/users/login` | Login and receive JWT | No |

### Incomes — `/api/incomes`

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/incomes` | Create income | JWT |
| `GET` | `/api/incomes` | Get all incomes (paginated) | JWT |
| `GET` | `/api/incomes/{id}` | Get income by ID | JWT |
| `PUT` | `/api/incomes/{id}` | Update income | JWT |
| `DELETE` | `/api/incomes/{id}` | Delete income | JWT |
| `GET` | `/api/incomes/by-type` | Filter by type | JWT |
| `GET` | `/api/incomes/total-month` | Aggregated total by month | JWT |

### Expenses — `/api/expenses`

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/expenses` | Create expense | JWT |
| `GET` | `/api/expenses` | Get all expenses (paginated) | JWT |
| `GET` | `/api/expenses/{id}` | Get expense by ID | JWT |
| `PUT` | `/api/expenses/{id}` | Update expense | JWT |
| `DELETE` | `/api/expenses/{id}` | Delete expense | JWT |

All protected endpoints require the following HTTP header:
```
Authorization: Bearer <your_jwt_token>
```

---

## ⚙️ Setup & Installation

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker Desktop

### 1. Clone the repository
```bash
git clone https://github.com/alexricardo02/finance-tracker-backend.git
cd finance-tracker-backend
```

### 2. Start infrastructure
```bash
docker-compose up -d
```
This spins up PostgreSQL on port `5433` and Redis on port `6379`.

### 3. Configure environment variables
Create `src/main/resources/application.properties`:
```properties
# Server
server.port=8080

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5433/finance_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.type=redis

# JWT
jwt.secret=your_secret_key_here
```

### 4. Run
```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

---

## 🔐 Environment Variables

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Secret key for signing JWT tokens |
| `REDIS_HOST` | Redis hostname |
| `REDIS_PORT` | Redis port |
| `REDIS_PASSWORD` | Redis password (empty for local) |

---

## 🗺 Roadmap

- [x] JWT Authentication
- [x] Redis Caching with per-user isolation
- [x] Rate Limiting (Bucket4j)
- [x] Pagination (Spring Data Pageable)
- [x] Global Exception Handler
- [x] CI/CD (GitHub Actions)
- [x] Docker + Docker Compose
- [ ] Refresh Tokens
- [ ] Role-Based Access Control (RBAC)
- [ ] Idempotency Keys
- [ ] Swagger / OpenAPI documentation
- [ ] Structured Logging (SLF4J)
- [ ] Spring Boot Actuator + Metrics

---

## 👤 Author

**Alex Brinckmann**

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
