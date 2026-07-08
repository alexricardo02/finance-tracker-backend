# 📊 Personal Finance Tracker API (Spring Boot)

A robust, high-performance RESTful API built with Spring Boot for tracking personal expenses and incomes. This backend system is designed with a strong focus on **scalability, security, and performance optimization**, featuring JWT-based authentication, an isolated multi-tenant architecture (user data privacy), and high-speed data retrieval using Redis caching.

🌍 **Live Frontend (Next.js):** [https://expenses-incomes-frontend.vercel.app/]

---

## Table of Contents
 
- [Features](#features)
- [System Design](#system-design)
- [Tech Stack](#tech-stack)
- [Setup & Installation]
- [Roadmap](#roadmap)

--- 

## Features
 
- **JWT authentication** with short-lived access tokens and rotating refresh tokens, delivered as httpOnly cookies
- **Per-user data isolation** — every query and cache key is scoped by user
- **Categories** — user-defined and default income/expense categories, replacing free-text types
- **Multi-currency support** — every transaction stores its original currency and is converted to the user's primary currency at the exchange rate of its own date; changing the primary currency triggers an asynchronous recalculation of full history via RabbitMQ
- **Redis caching** — heavy aggregations cached and evicted on mutation, isolated per user
- **Rate limiting** — token bucket (Bucket4j) on login, registration, refresh, password reset, and all write endpoints
- **Idempotency keys** — safe retries on income/expense creation without duplicating records
- **Soft deletes** with audit fields (created/updated/deleted timestamps and actor)
- **Transaction exports** to CSV, Excel, and PDF with filtering by date range, category, and payment method
- **Password reset** via signed, time-limited tokens, delivered through the Resend email API
- **Global exception handling** — consistent JSON error responses, no stack traces leaked to clients
- **Circuit breakers and retries** (Resilience4j) around external exchange-rate providers, with safe fallbacks
- **Native SQL aggregations** for totals by day/month/year/date range, computed at the database layer

## System Design
 
**Caching.** Heavy read operations (monthly totals, date-range aggregations, category breakdowns) are cached in Redis with per-user key isolation. Any mutation for that user evicts only that user's cache entries via a non-blocking `SCAN`, never a global flush.
 
**Currency conversion.** Each income/expense stores both its original amount/currency and its converted amount in the user's primary currency, using the FX rate of its own transaction date. When a user changes their primary currency, the update publishes an event to RabbitMQ; a background listener recalculates the full transaction history using each transaction's original date, so past totals stay historically accurate instead of being skewed by today's rate.
 
**Idempotency.** Write endpoints accept an `Idempotency-Key` header. A unique DB constraint reserves the key atomically, so concurrent duplicate requests (e.g. a retried network call) return the original response instead of creating a duplicate record.
 
**Exception handling.** A single `@ControllerAdvice` converts every exception into a standardized error response with status, message, and timestamp.
 
## Tech Stack
 
| Layer | Technology |
|---|---|
| Framework | Java 17, Spring Boot 3 |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| Cache | Redis |
| Messaging | RabbitMQ |
| Auth | JWT (jjwt), Spring Security |
| Rate limiting | Bucket4j |
| Resilience | Resilience4j (circuit breaker, retry) |
| Email | Resend (HTTP API) |
| Exports | Apache POI (Excel), Apache PDFBox (PDF) |
| Metrics | Micrometer + Prometheus |
| Validation | Jakarta Bean Validation |
| DevOps | Docker, Docker Compose |
| CI/CD | GitHub Actions (build, tests, license check, dependency vulnerability scan, secret scan, CodeQL) |
| Deployment | Render |
 
--- 
 
## Setup & Installation
 
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
This starts PostgreSQL, Redis, and RabbitMQ locally.
 
### 3. Configure the application
Copy `src/main/resources/application.properties.example` (or create `application-local.properties`) and set your database, Redis, RabbitMQ, JWT secret, and Resend API key. See `docker-compose.yml` for the local defaults each service expects.
 
### 4. Run
```bash
mvn spring-boot:run
```
The API is available at `http://localhost:8080`.
 
### 5. Run tests
```bash
mvn clean verify
```
 
## Roadmap
 
- [x] JWT authentication with refresh tokens
- [x] Redis caching with per-user isolation
- [x] Rate limiting (Bucket4j)
- [x] Idempotency keys
- [x] Multi-currency support with async recalculation
- [x] Transaction exports (CSV, Excel, PDF)
- [x] Transactional email via HTTP API (Resend)
- [x] CI/CD with license, vulnerability, and secret scanning
- [ ] Role-Based Access Control (RBAC)
- [ ] Swagger / OpenAPI documentation
- [ ] Structured logging with correlation IDs

## Author
 
**Alex Brinckmann**
