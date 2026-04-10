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
| `DELETE`| `/api/incomes/{id} |
| `GET` | `/api/incomes/by-type` | Get incomes filtered by type | Yes (JWT) |
| `GET` | `/api/incomes/total-month` | Get aggregated total by month | Yes (JWT) |
