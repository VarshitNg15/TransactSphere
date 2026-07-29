# TransactSphere - Exhaustive Interview Questions & Answers

This document provides a highly detailed, service-by-service breakdown of potential interview questions for the TransactSphere project, aligned with the current Kubernetes-native architecture with Nginx reverse proxy, CoreDNS service discovery, and a full suite of 10 microservices.

---

## 1. Core Infrastructure & Routing Services

### Nginx Reverse Proxy (`k8s/nginx.yaml`)
**Q1: What is the role of Nginx in TransactSphere, and where does it sit in the request flow?**
- Nginx is deployed as a dedicated **Kubernetes pod** (`nginx-proxy`) and is the sole public-facing entrypoint for all external traffic — including the React frontend and any API client.
- The request flow is: **Client → Nginx (ports 80/443) → API Gateway (port 8080) → Downstream microservices**.
- This means the API Gateway itself is never directly exposed to the internet; it only ever receives traffic from Nginx, keeping it shielded inside the cluster.

**Q2: How does Nginx handle HTTP vs. HTTPS traffic?**
- **Port 80 (HTTP):** The Nginx configuration issues a `301 Moved Permanently` redirect to the HTTPS equivalent URL (`return 301 https://$host$request_uri`). No traffic is served over plain HTTP.
- **Port 443 (HTTPS):** Nginx terminates TLS here. It decrypts the incoming HTTPS traffic and forwards it as plain HTTP to `http://gateway:8080` inside the cluster. Downstream services therefore never need to handle TLS themselves — a pattern called **TLS termination at the edge**.

**Q3: How are SSL certificates provisioned for Nginx in the Kubernetes deployment?**
- The Nginx container runs a startup script that checks for the existence of `/etc/nginx/certs/nginx.crt`. If it does not exist, it runs an `openssl req -x509` command to **auto-generate a self-signed certificate** (RSA 2048-bit, valid for 365 days) on first boot.
- The certificates are stored in a Kubernetes `emptyDir` volume, meaning they are ephemeral and regenerated each time the pod restarts. This is appropriate for local/dev environments.
- For production, this `emptyDir` volume would be replaced with a **Kubernetes Secret** holding a CA-signed certificate (e.g., provisioned by cert-manager with Let's Encrypt).

**Q4: How is the Nginx configuration managed in Kubernetes?**
- The `nginx.conf` content is stored in a **Kubernetes ConfigMap** (`nginx-config`) declared in `k8s/nginx.yaml`.
- The ConfigMap is mounted into the pod at `/etc/nginx/nginx.conf` using a `volumeMount` with `subPath: nginx.conf`.
- This approach decouples configuration from the Docker image — updating the Nginx config is done by editing the ConfigMap and rolling the deployment, with no image rebuild required.

**Q5: The Nginx config sets WebSocket-related headers. Why?**
- `proxy_set_header Upgrade $http_upgrade` and `proxy_set_header Connection "upgrade"` are required to support **HTTP protocol upgrades** (e.g., WebSocket connections).
- Even if the current application does not use WebSockets, including these headers future-proofs the proxy so that any WebSocket-based feature (e.g., real-time notification push) added to the Gateway will work transparently without Nginx config changes.

**Q6: The Nginx Kubernetes Service is of type `LoadBalancer`. What does this mean?**
- A `LoadBalancer` service type instructs Kubernetes to provision an external load balancer (on a cloud provider) or, for Minikube, to expose the service via `minikube tunnel`.
- This gives Nginx a stable, externally-reachable IP on ports **80** and **443**, making it the cluster's single internet-facing entry point without requiring manual `NodePort` configuration.

---

### Service Discovery (Kubernetes CoreDNS)
**Q7: TransactSphere does NOT use a service registry like Eureka. How do services discover each other?**
- The project runs on Kubernetes, which provides **native service discovery via CoreDNS**. Every Kubernetes Service object gets a stable DNS entry (e.g., `auth-service:8081`) that all pods in the cluster can resolve automatically.
- This removes the need for a separate registry process, meaning there is no single point of failure associated with an Eureka server — CoreDNS is a core Kubernetes component and highly available by default.
- Feign clients are configured to target these static Kubernetes DNS names directly, eliminating heartbeat overhead and the startup race-condition that Eureka-based systems require.

**Q8: What are the practical differences between Kubernetes CoreDNS discovery and Eureka-based discovery?**
- **Eureka (client-side):** Services poll a registry, cache the result locally, and perform their own load balancing. If Eureka restarts, services use a stale local cache for a period.
- **CoreDNS (server-side):** DNS resolution is handled by the cluster control plane. Kubernetes automatically updates DNS when a pod goes down or scales. The application code has zero awareness of service discovery logic.
- **Operational advantage:** In TransactSphere, there is no `discovery-server` module to build, deploy, or monitor. Kubernetes handles liveness, pod replacement, and routing natively.

### API Gateway (`gateway` — Port 8080)
**Q9: Why is the API Gateway necessary, and what are its core responsibilities in TransactSphere?**
- It provides a single entry point for all external traffic (the React frontend), hiding the internal microservice topology from the public internet.
- It handles cross-cutting concerns centrally: routing requests to the correct Kubernetes service, validating and extracting JWT claims, enforcing IP-based rate limits, and injecting verified identity headers (`X-User-Id`, `X-User-Email`, `X-User-Roles`) to downstream services.

**Q10: The `gateway` depends on Redis. What is Redis used for at the Gateway level?**
- **Distributed Rate Limiting:** Implements IP-based rate limiting so that even if multiple gateway replicas are running, the rate limit state is shared. A client hammering the API from one IP is throttled consistently regardless of which gateway pod receives the request.
- **JWT Blacklist Check:** On every request, the gateway queries Redis to ensure the presented token has not been invalidated (e.g., after a logout). This is the first line of defence before any request reaches a downstream service.

---

## 2. Security & Identity

### Auth Service (`auth-service` — Port 8081, DB: `auth_db`)
**Q11: Describe the full authentication flow implemented by the `auth-service`.**
- A user POSTs credentials to `/api/v1/auth/login`. The auth service validates the password against the BCrypt-hashed value stored in `auth_db`.
- Upon success, it signs and returns two JWTs: a **short-lived access token** and a **longer-lived refresh token**.
- The API Gateway validates the access token on subsequent requests and injects identity headers, so downstream services never need to re-verify the token themselves.

**Q12: How does TransactSphere handle JWT invalidation on logout, given that JWTs are stateless?**
- The `auth-service` exposes `POST /api/v1/auth/logout`. When called, the current token's signature (or JTI — JWT ID) is stored in Redis with a TTL exactly matching the token's remaining lifetime.
- The API Gateway checks this Redis "deny-list" on every incoming request. A blacklisted token is rejected with 401 before it ever reaches a downstream service.
- This achieves stateless-style scalability (no session store for active tokens) while still providing immediate, hard invalidation on logout.

**Q13: What is the purpose of the Refresh Token Rotation endpoint?**
- Access tokens are short-lived to minimise the window of exposure if one is leaked. However, short-lived tokens would force users to log in repeatedly.
- `POST /api/v1/auth/refresh` accepts the long-lived refresh token and issues a fresh access token. This balances security (short-lived access tokens) with usability (seamless re-authentication without a login prompt).

### User Service (`user-service` — Port 8082, DB: `user_db`)
**Q14: What is the distinct responsibility of the `user-service` versus the `auth-service`?**
- `auth-service` is strictly about *identity verification* (passwords, token issuance). It knows credentials, not people.
- `user-service` handles everything about the person: profile data (first/last name, phone, address), KYC (Know Your Customer) documents, and KYC verification levels. It is the source of truth for *who* the authenticated user is.

**Q15: How does the `user-service` initialise a user profile?**
- User profiles are **lazily auto-initialised** on the first API call. When the Gateway validates a token and injects `X-User-Id` / `X-User-Email`, the `user-service` checks if a profile exists in `user_db`. If not, it creates a default record on the fly.
- This avoids a synchronous "create profile" step at registration and decouples the auth and user domains cleanly.

**Q16: How is KYC document upload implemented?**
- Users submit KYC documents (e.g., ID scans) as **Base64-encoded strings** via the frontend profile page.
- The `user-service` stores the document reference in `user_db` and sets the KYC status to `PENDING`.
- An administrator reviews the document via the **Admin Dashboard** (`/admin`) and calls the `admin-service` to trigger approval or rejection, which updates the user's KYC status in `user_db` via an inter-service OpenFeign call.

---

## 3. Core Banking Domains

### Account Service (`account-service` — Port 8083, DB: `account_db`)
**Q17: What data and operations does the `account-service` manage?**
- Manages the full lifecycle of bank accounts: creation (Savings/Current), freezing, and closing. Enforces a business rule of strictly **one Savings and one Current account per user**.
- Generates unique 12-digit account numbers with a branch prefix (`1000`).
- Holds the authoritative account balance and validates sufficient funds before approving any debit operation.

**Q18: The `account-service` uses a sophisticated caching strategy. Explain it.**
- The service implements two advanced mechanisms on top of Redis:
  1. **Probabilistic Early Expiration (XFetch):** Instead of waiting for a cache entry to expire before refreshing it, the algorithm probabilistically triggers a background refresh *before* the TTL hits. This means the cache is always warm, and end-users never experience a cache miss delay.
  2. **SingleFlight:** If a cache miss does occur for a hot key, multiple concurrent goroutines/threads that simultaneously request the same key are deduplicated — only **one** database call is made. All waiting callers receive the same result from that single call.
- Together, these eliminate the "cache stampede" (thundering herd) problem where a mass cache expiry could flood the PostgreSQL database.

**Q19: Why does `account-service` connect to both Postgres and Redis?**
- **PostgreSQL** (`account_db`) is the **source of truth** for all financial state, providing ACID-compliant persistence and durable balance records.
- **Redis** is the **read-optimized hot cache**. Balance lookups (a very high-frequency read) are served from Redis in sub-millisecond time, preventing the database from being overwhelmed by read traffic.

### Transaction Service (`transaction-service` — Port 8084, DB: `transaction_db`)
**Q20: Describe the complete lifecycle of a transaction request.**
1. The request arrives at the Gateway, which validates the JWT and injects `X-User-Id`.
2. `transaction-service` runs three **inline, synchronous fraud checks** before touching any balance:
   - **Velocity Check (High-Frequency):** Rejects if > 5 transactions in a rolling 10-minute window.
   - **Volume Check (24-Hour Limit):** Rejects if cumulative amount > ₹100,000 in 24 hours.
   - **KYC Compliance Check:** For transfers/deposits, calls `user-service` via **OpenFeign** to verify the receiver's KYC status is `APPROVED`.
3. If all checks pass, it calls `account-service` via Feign to verify and update balances (debit/credit).
4. The transaction record is persisted to `transaction_db`.
5. An `OutboxEvent` is atomically saved to the `outbox_events` table in the same database transaction.
6. A background `@Scheduled` publisher polls `outbox_events` and publishes the event to Kafka, triggering the notification and analytics pipelines.

**Q21: How does the `transaction-service` guarantee that a transaction is only processed once (Idempotency)?**
- Requires clients to pass a unique `Idempotency-Key` header with every financial POST request.
- Before processing, the service checks this key against `transaction_db`. If it already exists, it returns the previously stored response without re-executing the transaction.
- This is critical for handling network retries — a mobile client that times out and retries the same deposit will not result in a double credit.

**Q22: Explain the Transactional Outbox Pattern used in `transaction-service`.**
- **The Problem (Dual-Write):** When a transaction succeeds, the service must save the record to PostgreSQL AND publish an event to Kafka. If it saves to PostgreSQL and then crashes before publishing to Kafka, the notification/analytics services will never know the transaction happened.
- **The Solution:** Both the `Transaction` entity and an `OutboxEvent` entity are written in the **same local ACID transaction** to PostgreSQL. If the Kafka publish fails, no data is lost — the event remains in `outbox_events`.
- A `@Scheduled` background job periodically polls `outbox_events` for unpublished entries, publishes them to Kafka, and marks them as sent. This guarantees **at-least-once delivery** to Kafka with zero event loss, even during broker outages.

---

## 4. Asynchronous & Support Services

### Fraud Service (`fraud-service` — Port 8086, DB: `fraud_db`)
**Q23: How is fraud detection structured in TransactSphere? Is it synchronous or asynchronous?**
- **Inline (Synchronous) Fraud Rules** run inside the `transaction-service` *before* any transaction is committed. This is a fast, rule-based system (velocity, volume, KYC checks) that can immediately block a bad transaction in the same request lifecycle.
- The `fraud-service` itself is an **asynchronous consumer** that reads from Kafka topics (`transaction.completed`, `transaction.fraudulent`) for post-hoc pattern analysis, recording fraud events, and maintaining a dedicated `fraud_db` for audit history and investigation workflows.
- This two-layer design provides both real-time blocking and deep historical analysis without slowing down the user-facing request.

**Q24: What action does `fraud-service` take when it detects an anomaly in the Kafka stream?**
- Persists a `FraudEvent` record to `fraud_db` with the detected `fraudReason`, timestamp, and associated transaction/account details.
- This data feeds into the `admin-service` and `analytics-service`, allowing staff and dashboards to monitor fraud trends and flag accounts for manual review.

### Notification Service (`notification-service` — Port 8085, DB: `notification_db`)
**Q25: How does the `notification-service` know when to send an alert?**
- It is a pure Kafka consumer. It subscribes to topics published by `transaction-service` (e.g., transaction completed, fraud blocked events).
- Upon receiving an event, it dispatches:
  - **Email** via a configured SMTP server (e.g., Gmail) with a formatted HTML template.
  - **SMS** — logged to the service console (a cost-free simulated dispatch).
  - **In-App Notification** — persisted to `notification_db` so the frontend can fetch unread alerts via `GET /api/v1/notifications`.

**Q26: How does Kafka guarantee no notification is lost if the notification service restarts?**
- Kafka retains messages on disk until the consumer **commits its offset**. The notification service only commits the offset after the notification has been successfully dispatched.
- If the service crashes mid-processing, it restarts and re-reads the uncommitted message, ensuring at-least-once delivery. Idempotency of email delivery (avoiding duplicates) requires a per-event deduplication key stored in `notification_db`.

### Statement Service (`statement-service` — Port 8089, DB: `statement_db`)
**Q27: What is the architectural purpose of the `statement-service`?**
- Offloads computationally heavy, cross-service read queries away from the core transactional databases.
- It aggregates historical transaction data dynamically at request time by calling `account-service` and `transaction-service` via **OpenFeign** and presents it in multiple formats: JSON summaries, downloadable CSV, and HTML reports.
- This separation means a user generating a 12-month statement never impacts the performance of users processing live transactions.

### Analytics Service (`analytics-service` — Port 8087, DB: `analytics_db`)
**Q28: What does the `analytics-service` track, and how does it receive data?**
- It is a Kafka consumer that listens to completed transaction events and builds aggregated metrics: total transaction volume by time window, active users per day, transaction type breakdowns.
- These aggregates are stored in `analytics_db` and exposed to the Admin Dashboard for real-time business intelligence without burdening the `transaction_db` with analytical queries.

### Audit Service (`audit-service` — Port 8088, DB: `audit_db`)
**Q29: How does `audit-service` differ from `analytics-service`?**
- `analytics-service` aggregates **business metrics** (e.g., how much money moved today) — it answers *business* questions.
- `audit-service` maintains a strict, **immutable chronological log** of *who did what and when* — it answers *compliance and legal* questions.
- It listens to all critical Kafka topics (`transaction.completed`, `transaction.fraudulent`) and writes tamper-proof records to `audit_db`. This log is essential for regulatory reporting, security investigations, and forensic auditing.

### Admin Service (`admin-service` — Port 8090)
**Q30: How are admin privileges enforced at the API level?**
- The API Gateway validates that the incoming JWT contains the `ROLE_ADMIN` claim before routing requests to the `admin-service`. Regular user tokens without this role are rejected at the Gateway.
- The `admin-service` uses **OpenFeign** to talk to `user-service`, `account-service`, and other services on behalf of admins, performing actions regular users cannot: approving/rejecting KYC documents, viewing all user profiles, and inspecting system-wide financial summaries.

---

## 5. Technology Stack & Infrastructure

### Frontend (`frontend` — Port 5173)
**Q31: How does the frontend handle authentication state and token lifecycle?**
- The React SPA stores the JWT access token in memory (or a secure `HttpOnly` cookie) to protect against XSS attacks.
- **Axios interceptors** are configured to automatically attach the `Authorization: Bearer <token>` header to every outbound API request.
- When the API returns a `401 Unauthorized` (access token expired), the interceptor transparently calls `POST /api/v1/auth/refresh` to obtain a new access token and retries the original request — providing a seamless experience without forcing the user to log in again.

**Q32: Describe the frontend's design system.**
- Built with **React 18** and **Vite** for a fast Single Page Application with hot-module replacement.
- Styled with **Vanilla CSS custom properties**, implementing a **glassmorphism design** with persistent light/dark theme toggling — no CSS framework overhead.
- Uses **Lucide React** for consistent iconography and **React Router DOM 6** for client-side navigation.

### Kafka (Message Broker)
**Q33: Kafka is configured in KRaft mode. What does this mean and why does it matter?**
- **KRaft mode** (`KAFKA_CFG_PROCESS_ROLES=broker,controller`) means Kafka acts as both the message broker and its own distributed metadata controller.
- It **completely eliminates the need for Apache ZooKeeper**, which was historically required as a separate, complex cluster for Kafka coordination. Removing ZooKeeper simplifies the infrastructure, reduces operational overhead, and removes a potential single point of failure.

**Q34: How does TransactSphere ensure messages are not consumed out of order for a given user?**
- Kafka guarantees ordering **within a single partition**. Producers are configured to use the **user ID or account ID as the partition key** when publishing transaction events.
- Because all events for the same user always go to the same partition, and a single consumer instance owns each partition, events for a given user are always processed in strict FIFO order.
- Different users (on different partitions) are processed in parallel, allowing the system to scale horizontally without sacrificing per-user ordering.

### Kubernetes Deployment
**Q35: Why does TransactSphere use Kubernetes over Docker Compose for production?**
- Docker Compose is a single-host tool used only for legacy local fallback (`docker/docker-compose.infra.yml`). The production and primary local target is Kubernetes.
- Kubernetes provides: **self-healing** (auto-restarts crashed pods), **horizontal auto-scaling** (scales replicas based on CPU/memory), **rolling zero-downtime deployments**, **native service discovery via CoreDNS**, and **Persistent Volume Claims (PVCs)** to ensure stateful data (PostgreSQL, Redis, Kafka) survives pod restarts.

**Q36: What is the purpose of Persistent Volume Claims (PVCs) in the Kubernetes manifests?**
- PostgreSQL, Redis, and Kafka are stateful services. Without PVCs, their data would be lost the moment their pods restart.
- PVCs provision a piece of durable, cluster-managed storage that is **independent of the pod's lifecycle**. When a pod restarts or is rescheduled to a different node, it re-mounts the same PVC and resumes with all data intact — critical for a financial platform where data loss is unacceptable.

**Q37: How does the Kubernetes infrastructure YAML organise dependencies?**
- The manifests are split into two files: `infrastructure.yaml` (PostgreSQL, Redis, Kafka deployments, services, and PVCs) and `services.yaml` (all application microservices).
- Operators apply `infrastructure.yaml` first to bring up the data layer, then apply `services.yaml` for the application layer. Services use Kubernetes readiness probes so the orchestrator only sends traffic to pods that are fully started and healthy.

---

## 6. Cross-Cutting Concerns

**Q38: How does TransactSphere prevent a race condition (negative balance) when two concurrent withdrawal requests hit the same account?**
- The `account-service` uses **database-level pessimistic locking** (`SELECT ... FOR UPDATE`) when reading an account balance before a debit. This places a row-level lock in PostgreSQL, ensuring only one transaction can read-modify-write the balance at a time.
- Concurrent requests queue up at the database layer rather than in application memory, providing strong consistency guarantees that are safe for financial operations.

**Q39: How are inter-service secrets (database passwords, Kafka credentials) managed?**
- Sensitive configuration values are externalised from application code into a `.env` file (with `.env.example` provided as a template).
- In the Kubernetes deployment model, these values are injected as **Kubernetes Secrets** and surfaced to pods as environment variables, keeping credentials out of Docker images and version control.

**Q40: What happens to unread in-app notifications if the user is offline when a transaction occurs?**
- The `notification-service` persists every in-app notification to `notification_db` at the time of processing the Kafka event — regardless of whether the user is currently online.
- When the user next opens the frontend, `GET /api/v1/notifications` fetches all unread notifications from `notification_db`. The notifications are durable and will never be lost, even if the user is offline for days.
