# TransactSphere - Exhaustive Interview Questions & Answers

This document provides a highly detailed, service-by-service breakdown of potential interview questions for the TransactSphere project.

---

## 1. Core Infrastructure & Routing Services

### Discovery Server (`discovery-server`)
**Q1: What is the exact role of the Discovery Server (Eureka) in TransactSphere?**
- Acts as a central registry where all other microservices (auth, user, account, etc.) register their instances and IP addresses upon startup.
- Enables client-side load balancing and dynamic routing, allowing services to find each other via logical application names rather than hardcoded URLs.

**Q2: How does the system handle a scenario where a service instance crashes abruptly?**
- Services send periodic heartbeats to the Discovery Server. 
- If the server stops receiving heartbeats from a specific instance, it eventually evicts that instance from the registry so traffic is no longer routed to it.

### API Gateway (`gateway`)
**Q3: Why is the API Gateway necessary, and what are its core responsibilities?**
- It provides a single point of entry for all external traffic (like the frontend), hiding the internal microservice architecture from the public internet.
- It handles cross-cutting concerns natively: routing requests to the correct service, validating authentication tokens, and implementing global CORS policies.

**Q4: The `gateway` depends on `redis`. What is Redis used for at the Gateway level?**
- Implements distributed rate limiting (e.g., using a Token Bucket algorithm) to prevent DDoS attacks and API abuse.
- Maintains a distributed cache to quickly serve frequent, non-dynamic requests without hitting backend services.

---

## 2. Security & Identity

### Auth Service (`auth-service`)
**Q5: Describe the authentication flow implemented by the `auth-service`.**
- Validates user credentials against stored, hashed passwords in the Postgres database.
- Upon success, generates and cryptographically signs a JSON Web Token (JWT) containing user identity and roles, returning it to the client.

**Q6: How does TransactSphere handle token invalidation or logout since JWTs are stateless?**
- The `auth-service` uses Redis as a "blacklist" or "deny-list" cache. 
- When a user logs out, their current active token is placed in Redis until its natural expiration time; the API gateway checks this list to reject blacklisted tokens.

### User Service (`user-service`)
**Q7: What is the distinct responsibility of the `user-service` versus the `auth-service`?**
- `auth-service` is strictly for identity verification (login/passwords) and token issuance.
- `user-service` handles user profiles, KYC (Know Your Customer) information, addresses, and account preferences.

**Q8: How does the `user-service` handle data synchronization when a new user registers?**
- After storing user details in Postgres, it publishes a `UserCreatedEvent` to Kafka.
- Other services (like `account-service` or `notification-service`) consume this event to initialize their own required data structures (like sending a welcome email or creating a default checking account).

---

## 3. Core Banking Domains

### Account Service (`account-service`)
**Q9: What data and operations does the `account-service` manage?**
- Manages the lifecycle of bank accounts (creation, freezing, closing) and holds the current authoritative balance.
- Validates that sufficient funds exist before allowing a debit operation.

**Q10: Why does `account-service` connect to both Postgres and Redis?**
- Postgres acts as the source of truth for account state and ledger entries ensuring ACID compliance.
- Redis is used to cache highly-read data, such as real-time balance inquiries, to offload heavy read traffic from the database.

### Transaction Service (`transaction-service`)
**Q11: How does the `transaction-service` guarantee that a transaction is only processed once (Idempotency)?**
- Requires clients to pass a unique `Idempotency-Key` header with every financial request.
- Checks this key against the database; if it exists, it returns the previous response instead of processing the transaction again.

**Q12: Describe the distributed transaction flow between `transaction-service` and `account-service`.**
- Uses a Saga pattern orchestrated via Kafka rather than distributed 2PC (Two-Phase Commit).
- `transaction-service` logs a pending transaction, publishes an event to Kafka; `account-service` consumes it, updates balances, and publishes a success/failure event back to Kafka.

---

## 4. Asynchronous & Support Services

### Fraud Service (`fraud-service`)
**Q13: How is the `fraud-service` integrated so it doesn't slow down legitimate transactions?**
- It operates completely asynchronously by consuming transaction streams from Kafka.
- It analyzes patterns (e.g., rapid consecutive transactions, unusual locations) in the background without blocking the synchronous user request.

**Q14: What action does `fraud-service` take if it detects anomalies?**
- Publishes a high-priority `FraudDetectedEvent` to Kafka.
- `transaction-service` and `account-service` consume this to immediately reverse the pending transaction and temporarily freeze the user's account.

### Notification Service (`notification-service`)
**Q15: How does the `notification-service` know when to send an email or SMS?**
- It is a pure consumer in the Kafka ecosystem. It listens to multiple topics (e.g., `user-registered`, `transaction-completed`, `fraud-alert`).
- Upon receiving an event, it maps the data to an email template and dispatches it via an SMTP server.

**Q16: How does the system test notifications locally without spamming real emails?**
- The docker-compose uses `mailhog`, a local SMTP testing server.
- The `notification-service` points its SMTP settings to `mailhog`, which catches the emails and displays them in a local web interface for developer verification.

### Statement Service (`statement-service`)
**Q17: What is the architectural purpose of the `statement-service`?**
- Offloads heavy, complex read queries required for generating monthly PDF/CSV statements away from the core transactional databases.
- Aggregates historical transaction data and provides it in a user-downloadable format.

**Q18: How does the `statement-service` stay up to date with transactions?**
- It can either consume Kafka events to build a read-optimized projection of transactions (CQRS pattern).
- Or it runs scheduled batch jobs against a read-replica database to compile statements at the end of the month.

### Analytics Service (`analytics-service`)
**Q19: How does `analytics-service` differ from `audit-service`?**
- `analytics-service` aggregates metrics for business intelligence (e.g., total volume processed, active users per day) often using time-series data.
- `audit-service` maintains a strict, immutable log of *who did what and when* for security, compliance, and legal tracking.

### Admin Service (`admin-service`)
**Q20: How are admin privileges enforced at the API and service level?**
- The Gateway and `admin-service` validate that the incoming JWT contains specific roles (e.g., `ROLE_ADMIN`).
- It allows staff to manually unfreeze accounts, refund transactions, or view system-wide health metrics.

---

## 5. Technology Stack & Infrastructure

### Frontend (`frontend`)
**Q21: How does the frontend handle authentication state securely?**
- Stores the JWT token. Best practice is to store it in a secure, `HttpOnly` cookie to prevent XSS (Cross-Site Scripting) attacks, rather than in LocalStorage.
- Intercepts outgoing HTTP requests to attach the token in the `Authorization: Bearer <token>` header if needed.

### Kafka (Message Broker)
**Q22: The `docker-compose.yml` configures Kafka with `KAFKA_CFG_PROCESS_ROLES=broker,controller`. What does this mean?**
- This indicates Kafka is running in KRaft mode, meaning it acts as both the message broker and its own metadata controller.
- It completely eliminates the need to run a separate Zookeeper container, simplifying the infrastructure.

### Nginx (Reverse Proxy)
**Q23: What is the purpose of the Nginx container situated in front of the Gateway?**
- It handles SSL/TLS termination, exposing port 443 (HTTPS) and decrypting traffic before passing it as HTTP to the Spring Cloud Gateway.
- The `docker-compose.yml` includes a script to automatically generate self-signed certificates for local HTTPS development.

### Docker Compose & Kubernetes
**Q24: What is the role of `depends_on` in the `docker-compose.yml`?**
- It dictates startup order. For example, `gateway` will not attempt to start until the `redis` and `discovery-server` containers are up.
- Note: It only waits for the container to start, not necessarily for the application inside (like Spring Boot) to be fully ready to accept traffic.

**Q25: The project has a `k8s` directory. What are the key differences between running this in Docker Compose versus Kubernetes?**
- Compose is strictly for local, single-host orchestration.
- Kubernetes is for production: it provides self-healing (restarting failed pods), horizontal auto-scaling based on CPU/RAM usage, rolling zero-downtime deployments, and distributed secret management.
