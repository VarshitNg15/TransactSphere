# TransactSphere Banking Platform

TransactSphere is an enterprise-grade, event-driven microservices banking application designed for scalability, security, and high performance. It features isolated databases per service, global API routing and rate-limiting, secure JWT-based authentication, Redis caching, real-time fraud checking, asynchronous notifications, and dynamic service discovery.

---

## 📖 Detailed Summary & Architecture

TransactSphere is architected around the **Database-per-Service** pattern to guarantee strict service boundary isolation. Services are dynamically registered and discovered using **Netflix Eureka (Discovery Server)**, enabling load balancing and dynamic routing without hardcoded URLs. They communicate either synchronously via **Spring Cloud OpenFeign** (for immediate consistency operations like debit/credit checks) or asynchronously via **Apache Kafka** (for eventual consistency flows like triggering email/SMS alerts upon transaction completion).

### Key Architectural Concepts
1. **Dynamic Service Discovery**: A central **Eureka Discovery Server** maintains a registry of all active microservice instances. The API Gateway and inter-service Feign clients seamlessly route requests to available instances.
2. **API Gateway Security**: All client traffic flows through the `gateway` module, which validates JWT tokens, resolves client IP addresses to apply granular rate-limits (using Redis), and injects verified edge headers (`X-User-Id`, `X-User-Email`, `X-User-Roles`) to downstream services dynamically resolved via Eureka.
3. **Database Isolation**: The infrastructure initializes isolated PostgreSQL databases for each service instance (e.g., `auth_db`, `user_db`, `account_db`) rather than using a single shared database, preventing tight service coupling.
4. **Anti-Stampede Caching Strategy**: The `account-service` employs an advanced caching system utilizing **Probabilistic Early Expiration (XFetch)** and **SingleFlight**. This completely eliminates cache misses for hot keys, ensures zero wait times for end-users during refreshes, and prevents database stampedes.
5. **Asynchronous Notification Routing**: High-latency notification delivery (Email, SMS) is decoupled from the transaction journey. The `transaction-service` publishes events to Kafka, allowing the `notification-service` to process them asynchronously.

---

## 🛠️ Tech Stack & Dependencies

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.5
- **Microservice Integration**: Spring Cloud 2023.0.1 (Netflix Eureka, Spring Cloud Gateway, OpenFeign)
- **Security**: Spring Security & JSON Web Tokens (JJWT 0.11.5)
- **Data Access & Mapping**: Spring Data JPA, MapStruct 1.5.5, Lombok 1.18.32
- **Database Driver**: PostgreSQL JDBC Driver
- **Event Streaming**: Spring Kafka
- **Caching**: Spring Data Redis

### Frontend
- **Framework**: React 18 (Vite-powered SPA)
- **Routing**: React Router DOM 6.18
- **HTTP Client**: Axios (configured with automated JWT interceptors and auto-refresh redirects)
- **Icons**: Lucide React 0.292
- **Styling**: Vanilla CSS custom variables system implementing a modern glassmorphism design with persistent light/dark themes.

### Infrastructure & Operations
- **Containerization**: Docker & Docker Compose
- **Relational Database**: PostgreSQL 15 (Alpine)
- **Key-Value Store / Cache**: Redis 7 (Alpine)
- **Message Broker**: Apache Kafka 3.4.0 (Bitnami KRaft mode, eliminating Zookeeper dependency)
- **Reverse Proxy**: Nginx (handling ports 80 and 443)

---

## 📂 Repository Structure

The project is structured as a Maven multi-module monorepo:

```
TransactSphere/
├── pom.xml                           # Parent Maven configuration
├── .env.example                      # Shell environment config template
├── README.md                         # Project documentation (this file)
├── docker/
│   ├── docker-compose.infra.yml      # Infrastructure service declarations (Postgres, Redis, Kafka, Nginx)
│   └── postgres/
│       └── init-db.sql               # PostgreSQL initial DB creations
├── discovery-server/                 # Netflix Eureka Service Registry - Port 8761
├── gateway/                          # API Gateway (Spring Cloud Gateway) - Port 8080
├── auth-service/                     # Identity Management & JWT Generation - Port 8081
├── user-service/                     # User Profiles & KYC status - Port 8082
├── account-service/                  # Accounts & Caching - Port 8083
├── transaction-service/              # Transaction processing & Fraud checks - Port 8084
├── notification-service/             # Multi-channel notification consumers - Port 8085
├── fraud-service/                    # Fraud Detection & Resolution - Port 8086
├── analytics-service/                # Real-time User & Global Stats - Port 8087
├── audit-service/                    # Global Audit Logging - Port 8088
├── statement-service/                # Account Statement Generation - Port 8089
├── admin-service/                    # Admin Dashboard & Approvals - Port 8090
└── frontend/                         # React / Vite Web Application - Port 5173
```

---

## ⚙️ Ports and Database Mapping

| Service Name | Port | Database Name | Internal/External Resources Used |
| :--- | :---: | :--- | :--- |
| **Discovery Server** | `8761` | *None* | Netflix Eureka Service Registry |
| **API Gateway** | `8080` | *None* | Redis (IP-based Request Rate Limiting), Eureka |
| **Auth Service** | `8081` | `auth_db` | PostgreSQL, Redis (JWT Blacklisting), Eureka |
| **User Service** | `8082` | `user_db` | PostgreSQL, Kafka, Eureka |
| **Account Service** | `8083` | `account_db` | PostgreSQL, Redis (Balance Caching), Kafka, Eureka |
| **Transaction Service** | `8084` | `transaction_db` | PostgreSQL, OpenFeign, Kafka, Eureka |
| **Notification Service** | `8085` | `notification_db` | PostgreSQL, Kafka (Consumer), SMTP MailHog, Eureka |
| **Fraud Service** | `8086` | `fraud_db` | PostgreSQL, Kafka (Consumer), Eureka |
| **Analytics Service** | `8087` | `analytics_db` | PostgreSQL, Kafka (Consumer), Eureka |
| **Audit Service** | `8088` | `audit_db` | PostgreSQL, Kafka (Consumer), Eureka |
| **Statement Service** | `8089` | `statement_db` | PostgreSQL, OpenFeign, Eureka |
| **Admin Service** | `8090` | *None* | OpenFeign, Eureka |
| **React Frontend** | `5173` | *None (Local Storage)*| Browser Client connecting to Gateway |
| **Nginx Proxy** | `80` / `443` | *N/A* | Edge proxy mapping traffic to Gateway |
| **PostgreSQL** | `5432` | *N/A (Multi-DB)* | Stores data for all backend services |
| **Redis Cache** | `6379` | *N/A* | In-memory cache & session store |
| **Kafka Broker** | `9092` / `29092` | *N/A* | Asynchronous communication pipeline (KRaft) |

---

## 🛡️ Core Features

### 1. Dynamic Service Discovery
- Centralized registry where microservices announce their presence.
- API Gateway acts as the edge proxy mapping routes to Eureka instances.
- Internal communication via `@FeignClient` is resolved automatically via Eureka, removing the need for static IPs.

### 2. Secure Authentication & Session Store
- Token-based stateless authentication utilizing JSON Web Tokens.
- Automated API Gateway security filter mapping client headers.
- **Refresh Token Rotation**: Endpoint POST `/api/v1/auth/refresh` retrieves updated short-lived access tokens.
- **Logout Blacklisting**: Invalidates access tokens immediately on POST `/api/v1/auth/logout` by storing signatures in Redis with a TTL matching the token's lifetime.

### 3. User & Profile Management
- Auto-initialization of user profiles upon first API call using gateway propagated details.
- Handles user identity records, addresses, contact information, and KYC verification levels.
- **KYC Document Upload**: Secure Base64 document upload via the frontend profile page.
- **Admin Dashboard**: Dedicated dashboard (`/admin`) for administrators to view users, inspect KYC documents, and approve/reject KYC verifications.
- **Account Limits**: Strictly enforces one Savings and one Current account per user.

### 4. Core Account Management
- Savings and Current account profiles associated with client owners.
- Automated generation of unique 12-digit account numbers (starts with branch prefix `1000`).
- Strict balance checks, credit/debit processing, and thread-safe lock mechanisms.

### 5. Real-time Fraud Detection & Verification Rules
Before transactions are persisted or executed, the `transaction-service` evaluates three core risk conditions:
- **Rolling 24-Hour Transaction Limits**: Restricts the maximum cumulative transactional amount across all transactions to **₹100,000** in a rolling 24-hour window.
- **High-Frequency Transactions**: Rejects transactions if the client attempts to make more than **5 transactions** within a rolling **10-minute window**.
- **KYC Status Validation**: For transfers and deposits, the transaction service connects to the `user-service` via a Feign client and checks the receiver's KYC status. If the recipient's KYC status is missing or not `APPROVED`, the transaction is immediately blocked and logs a `fraudReason`.

### 6. Multi-channel Asynchronous Notifications
When a transaction executes successfully or gets blocked due to a fraud check:
- **Emails** are dispatched via the SMTP template engine to the recipient and sender using a configured SMTP server (e.g., Gmail).
- **SMS alerts** are generated and logged directly into the notification service console output (free of cost).
- **In-App Notifications** are written to the postgres `notification_db` and can be retrieved dynamically by calling `/api/v1/notifications` from the frontend dashboard.

### 7. Transactional Outbox Pattern
To solve the dual-write problem (e.g. saving to the database and publishing to Kafka atomically), the `transaction-service` utilizes the **Transactional Outbox Pattern**.
- Transactions and their corresponding events (`OutboxEvent`) are saved atomically to PostgreSQL.
- A background `@Scheduled` publisher polls the `outbox_events` table and guarantees at-least-once delivery to Kafka, ensuring zero event loss even during broker outages.

### 8. Analytics & Global Auditing
- **Analytics Service**: Consumes completed transactions to generate real-time metrics, user-level statistics, and global dashboards.
- **Audit Service**: Automatically listens to all critical Kafka topics (`transaction.completed`, `transaction.fraudulent`) to maintain a tamper-proof, centralized audit log of all financial activities across the platform.

### 9. Automated Account Statements
- The **Statement Service** enables users to generate on-demand JSON summaries and downloadable reports (CSV/HTML) for their accounts by dynamically fetching cross-service data over OpenFeign.

---

## 🚀 Running the Project Locally

### Prerequisites
- **Java Development Kit (JDK)**: version 21
- **Apache Maven**: version 3.8+
- **Docker & Docker Compose**: installed and running

### Step 1: Clone and Configure Environment
Copy `.env.example` to `.env` in the root directory:
```bash
cp .env.example .env
```
Ensure your configuration reflects local requirements. By default, the application is pre-configured to locate resources on `localhost`.

### Step 2: Spin Up Full Stack using Docker Compose
You can run the entire infrastructure and microservices ecosystem using the provided `docker-compose.yml`:
```bash
docker compose up -d --build
```
This will start:
- Postgres (`5432`)
- Redis (`6379`)
- Kafka (`9092`/`29092`)
- Nginx (`80`/`443`)
- Discovery Server (`8761`)
- API Gateway (`8080`)
- Auth, User, Account, Transaction, and Notification Services

### Alternative: Running Microservices Manually (Backend)
If you prefer to run services manually via Maven instead of Docker, first start the infrastructure components:
```bash
docker compose -f docker/docker-compose.infra.yml up -d
```
Then, start the services in individual terminals (ensure **Discovery Server** starts first):

```bash
# 1. Start Discovery Server (Port 8761)
cd discovery-server && mvn spring-boot:run

# 2. Start Gateway Service (Port 8080)
cd ../gateway && mvn spring-boot:run

# 3. Start Auth Service (Port 8081)
cd ../auth-service && mvn spring-boot:run

# 4. Start User Service (Port 8082)
cd ../user-service && mvn spring-boot:run

# 5. Start Account Service (Port 8083)
cd ../account-service && mvn spring-boot:run

# 6. Start Transaction Service (Port 8084)
cd ../transaction-service && mvn spring-boot:run

# 7. Start Notification Service (Port 8085)
cd ../notification-service && mvn spring-boot:run

# 8. Start Fraud Service (Port 8086)
cd ../fraud-service && mvn spring-boot:run

# 9. Start Analytics Service (Port 8087)
cd ../analytics-service && mvn spring-boot:run

# 10. Start Audit Service (Port 8088)
cd ../audit-service && mvn spring-boot:run

# 11. Start Statement Service (Port 8089)
cd ../statement-service && mvn spring-boot:run

# 12. Start Admin Service (Port 8090)
cd ../admin-service && mvn spring-boot:run
```

*(Note: Wait for the Discovery Server to fully initialize before starting the other microservices so they can successfully register upon startup).*

### Step 3: Start the Frontend Application
```bash
# Navigate to the frontend directory
cd frontend

# Install node dependencies
npm install

# Start Vite React server
npm run dev
```
Open your browser and navigate to `http://localhost:5173` to interact with the web dashboard.

---

## 🧪 Verification & API Flows (PowerShell Examples)

Below is a complete script sequence to test authentication, user creation, account setup, and event routing.

### 1. Register a customer
```powershell
$registerBody = @{
    username = "jane_doe"
    email = "jane.doe@example.com"
    password = "SecurePassword123"
    role = "CUSTOMER"
} | ConvertTo-Json

$user = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/register" `
    -Method Post `
    -Headers @{"Content-Type"="application/json"} `
    -Body $registerBody
```

### 2. Log in and capture access token
```powershell
$loginBody = @{
    username = "jane_doe"
    password = "SecurePassword123"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
    -Method Post `
    -Headers @{"Content-Type"="application/json"} `
    -Body $loginBody

$token = $loginResponse.accessToken
```

### 3. Check and Update Profile
```powershell
# Retrieve profile (triggers automatic profile creation)
$profile = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/users/profile" `
    -Method Get `
    -Headers @{"Authorization"="Bearer $token"}

# Update profile information
$updateProfileBody = @{
    firstName = "Jane"
    lastName = "Doe"
    phoneNumber = "+15550199"
    email = "jane.doe@example.com"
    address = "456 Wall Street, New York"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/users/profile" `
    -Method Put `
    -Headers @{"Authorization"="Bearer $token"; "Content-Type"="application/json"} `
    -Body $updateProfileBody
```

### 4. Open a Savings Account
```powershell
$accountBody = @{
    accountType = "SAVINGS"
} | ConvertTo-Json

$account = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts" `
    -Method Post `
    -Headers @{"Authorization"="Bearer $token"; "Content-Type"="application/json"} `
    -Body $accountBody

$acn = $account.accountNumber
Write-Host "Created account: $acn"
```

### 5. Execute Deposit (Core banking flow)
```powershell
$depositBody = @{
    targetAccountNumber = $acn
    amount = 25000.00
    description = "Monthly savings deposit"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/transactions/deposit" `
    -Method Post `
    -Headers @{"Authorization"="Bearer $token"; "Content-Type"="application/json"} `
    -Body $depositBody
```

### 6. Verify Events & Notification Delivery
- **Email**: Check your configured SMTP email inbox to verify the transaction email alerts.
- **Eureka Dashboard**: Go to [http://localhost:8761](http://localhost:8761) to view all registered microservices.
- **SMS Logs**: Check the console log of the running `notification-service` to inspect mock SMS prints.
- **In-App Notifications**:
  ```powershell
  Invoke-RestMethod -Uri "http://localhost:8080/api/v1/notifications" `
      -Method Get `
      -Headers @{"Authorization"="Bearer $token"}
  ```
