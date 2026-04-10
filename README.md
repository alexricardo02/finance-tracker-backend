# 📊 Personal Finance Tracker API (Spring Boot)

A robust, high-performance RESTful API built with Spring Boot for tracking personal expenses and incomes. This backend system is designed with a strong focus on **scalability, security, and performance optimization**, featuring JWT-based authentication, an isolated multi-tenant architecture (user data privacy), and high-speed data retrieval using Redis caching.

---

## 🚀 Key Features

* **Secure Authentication:** JWT (JSON Web Token) implementation for stateless, secure user sessions and route protection.
* **Complete Financial CRUD:** Create, Read, Update, and Delete operations for both Incomes and Expenses.
* **Advanced Reporting & Analytics:** Native SQL queries optimized to fetch total aggregations by specific days, months, years, date ranges, and transaction types.
* **High-Performance Caching:** Redis integration to cache heavy database aggregations, reducing database load and dropping response times to milliseconds.
* **Strict Data Isolation:** Every query and cache key is scoped by `user_id` to ensure absolute data privacy between users.
* **Optimized Database Schema:** B-Tree indexing on highly queried columns (Dates, Types, and User IDs) to maintain fast lookup speeds as the database grows.

---

## 🛠️ Tech Stack & Architecture

* **Framework:** Java, Spring Boot, Spring Web
* **Persistence:** Spring Data JPA, Hibernate
* **Database:** PostgreSQL (Relational DB)
* **Caching Layer:** Redis (In-memory DB via Docker)
* **Security:** Spring Security, JWT (io.jsonwebtoken)
* **Build Tool:** Maven

### 🏗️ System Design Highlights

1. **Caching Strategy (Read-Heavy Optimization):**
   Heavy aggregations (e.g., "Total Income for the last 6 months") are cached in Redis. Cache keys are uniquely identified by `#username + '_' + #parameter` to prevent cross-user data leakage.
2. **Cache Eviction (Data Consistency):**
   A strict `@CacheEvict` policy is applied to all mutating methods (`POST`, `PUT`, `DELETE`). Any change in a user's financial records immediately invalidates the stale cache, ensuring the dashboard always displays real-time data.
3. **Native SQL & COALESCE:**
   Instead of loading thousands of Java objects into memory to calculate totals, the application leverages Native SQL queries with `COALESCE(SUM(amount), 0)` to perform calculations directly at the database layer.

---

## 🗄️ Database Schema Overview

The system operates on three main entities:

* **Users:** Stores authentication credentials, hashed passwords, and creation dates.
* **Incomes:** Records money coming in. Belongs to a User. Contains amount, currency, date, type, and description.
* **Expenses:** Records money going out. Belongs to a User. Contains amount, currency, date, type, and description.

*(A One-to-Many relationship exists between User -> Incomes and User -> Expenses, utilizing Lazy Fetching for memory efficiency).*

---

## 📡 API Endpoints Reference

### 🔐 Authentication (`/api/users`)
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/users/register` | Register a new user | No |
| `POST` | `/api/users/login` | Authenticate and receive JWT | No |

### 💸 Incomes (`/api/incomes`)
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/incomes` | Create a new income record | Yes (JWT) |
| `GET` | `/api/incomes` | Get all incomes for current user | Yes (JWT) |
| `GET` | `/api/incomes/{id}` | Get specific income by ID | Yes (JWT) |
| `PUT` | `/api/incomes/{id}` | Update an existing income | Yes (JWT) |
| `DELETE`| `/api/incomes/{id} | Delete an income | Yes (JWT) |
| `GET` | `/api/incomes/by-type` | Get incomes filtered by type | Yes (JWT) |
| `GET` | `/api/incomes/total-month` | Get aggregated total by month | Yes (JWT) |

### 🛒 Expenses (`/api/expenses`)
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/expenses` | Create a new expense record | Yes (JWT) |
| `GET` | `/api/expenses` | Get all expenses for current user | Yes (JWT) |
| `GET` | `/api/expenses/{id}` | Get specific expense by ID | Yes (JWT) |
| `PUT` | `/api/expenses/{id}` | Update an existing expense | Yes (JWT) |
| `DELETE`| `/api/expenses/{id}` | Delete an expense | Yes (JWT) |

*(Note: The API also includes multiple endpoints for fetching totals by Year, Last 7 Days, Last 3 Months, etc.)*

---

## ⚙️ Setup & Installation

### Prerequisites
* Java 17 or higher
* Maven 3.6+
* PostgreSQL installed and running
* Docker Desktop (for Redis)

### 1. Start Redis Server
To run the caching layer, spin up a Redis container using Docker:
```bash
docker run --name finance-redis -p 6379:6379 -d redis
```


### 2. Configure Database and Application Properties
Create an application.properties (or .yml) file in src/main/resources and configure your environment variables:
```bash
# Server configuration
server.port=8080

# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/finance_db
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.type=redis

# JWT Secret Key (Replace with a strong secret in production)
jwt.secret=your_super_secret_key_here
```

### 3. Build and Run
Clone the repository and build the project using Maven:
```bash
git clone [https://github.com/yourusername/finance-tracker-api.git](https://github.com/yourusername/finance-tracker-api.git)
cd finance-tracker-api
mvn clean install
mvn spring-boot:run
```

---

## ⚙️ Core Optimizations

3. **Native SQL & COALESCE**
Instead of loading thousands of Java objects into memory to calculate totals, the application leverages Native SQL queries with COALESCE(SUM(amount), 0) to perform calculations directly at the database layer.

---

## 🔒 Security Workflow
* **1.** The client sends a POST request to /api/users/login with credentials.
* **2.** The server verifies the credentials and returns a signed JWT.
* **3.** For subsequent requests, the client must include this token in the HTTP Header:
```HTTP
Authorization: Bearer <your_jwt_token>
```
* **4.** The JwtRequestFilter intercepts incoming requests, validates the token signature/expiration, and extracts the username to establish the SecurityContext.

---

## 🗺️ Roadmap / Future Improvements
* **[ ]** Rate Limiting (Bucket4j): Implement Token Bucket algorithm to protect the /login and POST endpoints against Brute Force and DDoS attacks.
* **[ ]** Pagination & Sorting: Implement Spring Data Pageable for /api/expenses and /api/incomes to handle massive datasets gracefully.
* **[ ]** Global Exception Handler: Implement @ControllerAdvice to standardize error response payloads (e.g., 404 Not Found, 400 Bad Request) across the entire application.
