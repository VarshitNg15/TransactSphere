# TransactSphere Banking Platform

TransactSphere is an enterprise-grade, event-driven microservices banking application designed for scalability, security, and high performance. It features isolated databases per service, global API routing and rate-limiting, secure JWT-based authentication, Redis caching, real-time fraud checking, and asynchronous notifications.

---

## 📖 Detailed Summary & Architecture

TransactSphere is architected around the **Database-per-Service** pattern to guarantee strict service boundary isolation. Services communicate either synchronously via **Spring Cloud OpenFeign** (for immediate consistency operations like debit/credit checks) or asynchronously via **Apache Kafka** (for eventual consistency flows like triggering email/SMS alerts upon transaction completion).

### Key Architectural Concepts
1. **API Gateway Security**: All client traffic flows through the [gateway](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/gateway) module, which validates JWT tokens, resolves client IP addresses to apply granular rate-limits (using Redis), and injects verified edge headers (`X-User-Id`, `X-User-Email`, `X-User-Roles`) to downstream services.
2. **Database Isolation**: The infrastructure initializes isolated PostgreSQL databases for each service instance (e.g., `auth_db`, `user_db`, `account_db`) rather than using a single shared database, preventing tight service coupling.
3. **Write-Through Caching**: The [account-service](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/account-service) employs Redis to cache account balances, optimizing query speed for dashboard reads and mitigating high read loads on PostgreSQL.
4. **Asynchronous Notification Routing**: High-latency notification delivery (Email, SMS) is decoupled from the transaction journey. The [transaction-service](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/transaction-service) publishes events to Kafka, allowing the [notification-service](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/notification-service) to process them asynchronously.

---

## 🛠️ Tech Stack & Dependencies

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.5
- **Microservice Integration**: Spring Cloud 2023.0.1 (Spring Cloud Gateway, OpenFeign)
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
- **Mock Mail Server**: MailHog (SMTP server for testing e-mails)

---

## 📂 Repository Structure

The project is structured as a Maven multi-module monorepo:

```
TransactSphere/
├── pom.xml                           # Parent Maven configuration
├── .env.example                      # Shell environment config template
├── README.md                         # Project documentation (this file)
├── docker/
│   ├── docker-compose.infra.yml      # Infrastructure service declarations (Postgres, Redis, Kafka, MailHog)
│   └── postgres/
│       └── init-db.sql               # PostgreSQL initial DB creations
├── gateway/                          # API Gateway (Spring Cloud Gateway) - Port 8080
├── auth-service/                     # Identity Management & JWT Generation - Port 8081
├── user-service/                     # User Profiles & KYC status - Port 8082
├── account-service/                  # Accounts & Caching - Port 8083
├── transaction-service/              # Transaction processing & Fraud checks - Port 8084
├── notification-service/             # Multi-channel notification consumers - Port 8085
└── frontend/                         # React / Vite Web Application - Port 5173
```

---

## ⚙️ Ports and Database Mapping

| Service Name | Port | Database Name | Internal/External Resources Used |
| :--- | :---: | :--- | :--- |
| **API Gateway** | `8080` | *None* | Redis (IP-based Request Rate Limiting) |
| **Auth Service** | `8081` | `auth_db` | PostgreSQL, Redis (JWT Blacklisting on logout) |
| **User Service** | `8082` | `user_db` | PostgreSQL |
| **Account Service** | `8083` | `account_db` | PostgreSQL, Redis (Balance Caching) |
| **Transaction Service** | `8084` | `transaction_db` | PostgreSQL, OpenFeign (Account Client), Kafka (Event Producer) |
| **Notification Service** | `8085` | `notification_db` | PostgreSQL, Kafka (Consumer), SMTP MailHog |
| **React Frontend** | `5173` | *None (Local Storage)*| Browser Client connecting to Gateway |
| **PostgreSQL** | `5432` | *N/A (Multi-DB)* | Stores data for all backend services |
| **Redis Cache** | `6379` | *N/A* | In-memory cache & session store |
| **Kafka Broker** | `9092` / `29092` | *N/A* | Asynchronous communication pipeline (KRaft) |
| **MailHog SMTP** | `1025` | *N/A* | Outgoing SMTP listener |
| **MailHog UI** | `8025` | *N/A* | Web dashboard to inspect outgoing emails |

---

## 🛡️ Core Features

### 1. Secure Authentication & Session Store
- Token-based stateless authentication utilizing JSON Web Tokens.
- Automated API Gateway security filter mapping client headers.
- **Refresh Token Rotation**: Endpoint POST `/api/v1/auth/refresh` retrieves updated short-lived access tokens.
- **Logout Blacklisting**: Invalidates access tokens immediately on POST `/api/v1/auth/logout` by storing signatures in Redis with a TTL matching the token's lifetime.

### 2. User & Profile Management
- Auto-initialization of user profiles upon first API call using gateway propagated details.
- Handles user identity records, addresses, contact information, and KYC verification levels.

### 3. Core Account Management
- Savings and Current account profiles associated with client owners.
- Automated generation of unique 12-digit account numbers (starts with branch prefix `1000`).
- Strict balance checks, credit/debit processing, and thread-safe lock mechanisms.

### 4. Real-time Fraud Detection & Verification Rules
Before transactions are persisted or executed, the `transaction-service` evaluates three core risk conditions:
- **Rolling 24-Hour Transaction Limits**: Restricts the maximum cumulative transactional amount across all transactions to **₹100,000** in a rolling 24-hour window.
- **High-Frequency Transactions**: Rejects transactions if the client attempts to make more than **5 transactions** within a rolling **10-minute window**.
- **KYC Status Validation**: For transfers and deposits, the transaction service connects to the `user-service` via a Feign client and checks the receiver's KYC status. If the recipient's KYC status is missing or not `APPROVED`, the transaction is immediately blocked and logs a `fraudReason`.

### 5. Multi-channel Asynchronous Notifications
When a transaction executes successfully or gets blocked due to a fraud check:
- **Emails** are dispatched via the SMTP template engine to the recipient and sender, viewable locally via MailHog.
- **SMS alerts** are generated and logged directly into the notification service console output (free of cost).
- **In-App Notifications** are written to the postgres `notification_db` and can be retrieved dynamically by calling `/api/v1/notifications` from the frontend dashboard.

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

### Step 2: Spin Up Infrastructure Containers
Start the infrastructure services via Docker Compose:
```bash
docker compose -f docker/docker-compose.infra.yml up -d
```
Verify that Postgres (`5432`), Redis (`6379`), Kafka (`9092`/`29092`), and MailHog (`1025`/`8025`) are healthy and bound to their ports.

### Step 3: Build the Multi-Module Project
Run a clean install compile inside the project root:
```bash
mvn clean install -DskipTests
```

### Step 4: Run Microservices (Backend)
Start each service in individual terminals, or in the background:
```bash
# Start Auth Service (Port 8081)
cd auth-service && mvn spring-boot:run

# Start Gateway Service (Port 8080)
cd ../gateway && mvn spring-boot:run

# Start User Service (Port 8082)
cd ../user-service && mvn spring-boot:run

# Start Account Service (Port 8083)
cd ../account-service && mvn spring-boot:run

# Start Transaction Service (Port 8084)
cd ../transaction-service && mvn spring-boot:run

# Start Notification Service (Port 8085)
cd ../notification-service && mvn spring-boot:run
```

### Step 5: Start the Frontend Application
```bash
# Navigate to the frontend directory
cd ../frontend

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
- **Email**: Go to the MailHog web dashboard at [http://localhost:8025](http://localhost:8025) to check the transaction email alerts.
- **SMS Logs**: Check the console log of the running `notification-service` to inspect mock SMS prints.
- **In-App Notifications**:
  ```powershell
  Invoke-RestMethod -Uri "http://localhost:8080/api/v1/notifications" `
      -Method Get `
      -Headers @{"Authorization"="Bearer $token"}
  ```
