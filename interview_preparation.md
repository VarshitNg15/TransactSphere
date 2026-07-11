# TransactSphere - Interview Preparation Guide

This is a comprehensive, interview-focused breakdown of **TransactSphere**, designed to help you understand every technical concept, architectural decision, and feature within the project. 

When you explain this project in an interview, you want to demonstrate that you understand not just *how* to write code, but *why* you chose specific tools and patterns to solve complex enterprise problems.

---

## 1. The "Elevator Pitch" (High-Level Overview)

> [!TIP]
> **What it is:** TransactSphere is an enterprise-grade, event-driven banking platform built using a microservices architecture.
> **What it does:** It handles core banking operations like user onboarding, KYC verification, secure authentication, account management, and real-time transaction processing with built-in fraud detection.
> **The Goal:** To build a system that is highly scalable, secure, and resilient, ensuring strict data isolation while providing high-performance transaction processing and asynchronous notifications.

---

## 2. Core Architectural Patterns

In an interview, architecture is usually the most important topic for senior or mid-level roles.

*   **Microservices Architecture:** The application is split into distinct, independently deployable services (Auth, User, Account, Transaction, Notification). This ensures that if one service fails (e.g., Notifications), the core banking functions (like depositing money) remain unaffected.
*   **Database-per-Service Pattern:** Instead of a giant monolithic database, each microservice has its own isolated PostgreSQL database (e.g., `auth_db`, `user_db`). 
    *   *Interview talking point:* This prevents tight coupling. Services cannot directly query each other's tables; they *must* use APIs. This makes scaling and migrating data much safer.
*   **Synchronous vs. Asynchronous Communication:**
    *   **Synchronous (Spring Cloud OpenFeign):** Used when immediate consistency is required. For example, before a transaction is approved, the Transaction Service uses Feign to ask the Account Service, *"Does this account exist, and what is its balance?"*
    *   **Asynchronous (Apache Kafka):** Used for eventual consistency and decoupling. Once a transaction is successful, the Transaction Service publishes an event to a Kafka topic. The Notification Service consumes this event at its own pace to send emails/SMS, ensuring the user doesn't have to wait for an email server to respond before their transaction is marked complete.

---

## 3. The Tech Stack Explained

*   **Backend:** Java 21 and Spring Boot 3.2.5. Java 21 introduces virtual threads, which makes handling thousands of concurrent requests much more memory-efficient.
*   **Frontend:** React 18 with Vite for a fast, modern Single Page Application (SPA).
*   **Message Broker:** Apache Kafka (using KRaft mode, which removes the old Zookeeper dependency, making the infrastructure lighter).
*   **Caching:** Redis (used for two main things: caching account balances to reduce database load, and rate-limiting incoming API requests).
*   **Database:** PostgreSQL (A robust, ACID-compliant relational database, essential for financial data).
*   **Infrastructure:** Docker & Docker Compose to containerize everything, meaning the app runs exactly the same on a developer's laptop as it would in production.

---

## 4. Service-by-Service Breakdown (The "Micro" in Microservices)

### A. API Gateway (Port 8080)
*   **Role:** The single entry point for all frontend traffic. 
*   **Key Concept:** It acts as a reverse proxy and security checkpoint. It validates JSON Web Tokens (JWTs) before routing requests to downstream services. It also uses **Redis for IP-based Rate Limiting** to prevent DDoS attacks.
*   **Header Injection:** Once it validates a token, it injects the user's ID and roles into the request headers (`X-User-Id`), so backend services don't have to re-verify the token.

### B. Auth Service (Port 8081)
*   **Role:** Identity management and JWT generation.
*   **Key Concept (Stateless Auth):** It issues JWTs. When a user logs out, the token is "blacklisted" by storing its signature in Redis with a Time-To-Live (TTL) matching the token's expiration. This solves the classic JWT problem of not being able to invalidate tokens before they expire.

### C. User Service (Port 8082)
*   **Role:** Manages user profiles, addresses, and KYC (Know Your Customer) statuses.
*   **Key Concept:** It acts as the source of truth for user identity. The Transaction service queries this service to ensure a user is legally allowed to receive money.

### D. Account Service (Port 8083)
*   **Role:** Manages Savings and Current accounts, generates unique 12-digit account numbers, and handles balance checks.
*   **Key Concept (Write-Through Caching):** Because users check their balance frequently (high read-load), balances are cached in Redis. When money is deposited/withdrawn, the system writes the new balance to PostgreSQL *and* updates Redis simultaneously.

### E. Transaction Service (Port 8084)
*   **Role:** The heart of the platform. Processes deposits, withdrawals, and transfers.
*   **Key Concept (Real-Time Fraud Detection):** Before executing a transaction, it evaluates rules:
    1.  *Velocity Check:* Has the user made > 5 transactions in a rolling 10-minute window?
    2.  *Volume Check:* Has the user transferred > ₹100,000 in a rolling 24-hour window?
    3.  *Compliance Check:* Is the receiver's KYC status `APPROVED`? (Checked via Feign client).
*   **Key Concept (Concurrency/Locking):** In banking, if two requests try to withdraw money simultaneously, you could get a race condition resulting in negative balances. This service must implement strict thread-safe mechanisms (like database row locks or optimistic locking) during credit/debit processing.

### F. Notification Service (Port 8085)
*   **Role:** Dispatches emails, SMS, and in-app alerts.
*   **Key Concept (Event-Driven Consumer):** It listens to Kafka topics. If the email server (simulated by MailHog) goes down, Kafka retains the messages. Once the Notification service is back online, it picks up exactly where it left off, guaranteeing no dropped notifications.

---

## 5. Common Interview Questions You Can Answer With This Project

> [!NOTE]
> **Q: "How do you handle distributed transactions across microservices?"**
> 
> *Your Answer:* "In TransactSphere, I rely heavily on event-driven architecture with Kafka for non-critical flows (like notifications) to maintain high availability. For critical flows that span services (like verifying KYC before a transfer), I use synchronous Feign clients. Because I use isolated databases, I ensure strict transaction boundaries within individual services using Spring's `@Transactional` to guarantee ACID properties locally."

> [!NOTE]
> **Q: "How did you optimize application performance?"**
> 
> *Your Answer:* "Two main ways. First, I implemented a Redis cache in the Account Service for balance retrievals, which drastically reduced the read load on the PostgreSQL database. Second, I offloaded high-latency tasks, like sending emails via SMTP, to an asynchronous Kafka pipeline so the user's API request completes in milliseconds."

> [!NOTE]
> **Q: "How did you secure the microservices?"**
> 
> *Your Answer:* "Security starts at the edge. The API Gateway intercepts all traffic and validates stateless JWTs. It also utilizes Redis to enforce rate limits per IP address to mitigate abuse. Furthermore, backend services trust the Gateway; the Gateway extracts the user's identity from the JWT and passes it down via secure HTTP headers (`X-User-Id`), preventing users from spoofing actions."
