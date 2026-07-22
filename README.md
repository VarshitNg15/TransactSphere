# TransactSphere Banking Platform

TransactSphere is an enterprise-grade, event-driven microservices banking application designed for scalability, security, and high performance. It features isolated databases per service, global API routing and rate-limiting, secure JWT-based authentication, Redis caching, real-time fraud checking, asynchronous notifications, and dynamic service discovery.

---

## 📖 Detailed Summary & Architecture

TransactSphere is architected around the **Database-per-Service** pattern to guarantee strict service boundary isolation. Services are seamlessly deployed using **Kubernetes**, relying on **Kubernetes Native Service Discovery (CoreDNS)** for internal routing and load balancing without hardcoded URLs. They communicate either synchronously via **Spring Cloud OpenFeign** (for immediate consistency operations like debit/credit checks) or asynchronously via **Apache Kafka** (for eventual consistency flows like triggering email/SMS alerts upon transaction completion).

### Key Architectural Concepts
1. **Kubernetes Service Discovery**: Microservices utilize Kubernetes CoreDNS for native service discovery. The API Gateway and inter-service Feign clients seamlessly route requests directly to Kubernetes service names (e.g., `auth-service:8081`).
2. **API Gateway Security**: All client traffic flows through the `gateway` module, which validates JWT tokens, resolves client IP addresses to apply granular rate-limits (using Redis), and injects verified edge headers (`X-User-Id`, `X-User-Email`, `X-User-Roles`) to downstream services resolved via Kubernetes DNS.
3. **Database Isolation & Persistent Storage**: The infrastructure initializes isolated PostgreSQL databases for each service instance (e.g., `auth_db`, `user_db`, `account_db`) rather than using a single shared database. Kubernetes **Persistent Volume Claims (PVCs)** ensure stateful data durability across pod crashes or restarts.
4. **Anti-Stampede Caching Strategy**: The `account-service` employs an advanced caching system utilizing **Probabilistic Early Expiration (XFetch)** and **SingleFlight**. This completely eliminates cache misses for hot keys, ensures zero wait times for end-users during refreshes, and prevents database stampedes.
5. **Asynchronous Notification Routing**: High-latency notification delivery (Email, SMS) is decoupled from the transaction journey. The `transaction-service` publishes events to Kafka, allowing the `notification-service` to process them asynchronously.

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
- **Containerization & Orchestration**: Docker & Kubernetes (Minikube / Managed K8s)
- **Relational Database**: PostgreSQL 15 (Alpine with Persistent Volumes)
- **Key-Value Store / Cache**: Redis 7 (Alpine with Append-Only Persistency)
- **Message Broker**: Apache Kafka 3.4.0 (Bitnami KRaft mode, with Persistent Volumes)

---

## 📂 Repository Structure

The project is structured as a Maven multi-module monorepo:

```
TransactSphere/
├── pom.xml                           # Parent Maven configuration
├── .env.example                      # Shell environment config template
├── README.md                         # Project documentation (this file)
├── k8s/
│   ├── infrastructure.yaml           # K8s Deployments/Services/PVCs for Postgres, Redis, Kafka
│   └── services.yaml                 # K8s Deployments/Services for all backend microservices
├── docker/
│   ├── docker-compose.infra.yml      # Legacy docker infrastructure fallback
│   └── postgres/
│       └── init-db.sql               # PostgreSQL initial DB creations
├── gateway/                          # API Gateway (Spring Cloud Gateway) - Port 8080 (Local: 8081)
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
| **API Gateway** | `8080` (Local: `8081`) | *None* | Redis (IP-based Request Rate Limiting), K8s DNS |
| **Auth Service** | `8081` | `auth_db` | PostgreSQL, Redis (JWT Blacklisting), K8s DNS |
| **User Service** | `8082` | `user_db` | PostgreSQL, Kafka, K8s DNS |
| **Account Service** | `8083` | `account_db` | PostgreSQL, Redis (Balance Caching), Kafka, K8s DNS |
| **Transaction Service** | `8084` | `transaction_db` | PostgreSQL, OpenFeign, Kafka, K8s DNS |
| **Notification Service** | `8085` | `notification_db` | PostgreSQL, Kafka (Consumer), SMTP (e.g., Gmail), K8s DNS |
| **Fraud Service** | `8086` | `fraud_db` | PostgreSQL, Kafka (Consumer), K8s DNS |
| **Analytics Service** | `8087` | `analytics_db` | PostgreSQL, Kafka (Consumer), K8s DNS |
| **Audit Service** | `8088` | `audit_db` | PostgreSQL, Kafka (Consumer), K8s DNS |
| **Statement Service** | `8089` | `statement_db` | PostgreSQL, OpenFeign, K8s DNS |
| **Admin Service** | `8090` | *None* | OpenFeign, K8s DNS |
| **React Frontend** | `5173` | *None (Local Storage)*| Browser Client connecting to Gateway |
| **PostgreSQL** | `5432` | *N/A (Multi-DB)* | Stores data for all backend services |
| **Redis Cache** | `6379` | *N/A* | In-memory cache & session store |
| **Kafka Broker** | `9092` / `29092` | *N/A* | Asynchronous communication pipeline (KRaft) |

---

## 🛡️ Core Features

### 1. Kubernetes Native Service Discovery
- Seamless internal routing leveraging Kubernetes CoreDNS.
- API Gateway directly routes to internal Kubernetes service names.
- Internal communication via `@FeignClient` resolves seamlessly using K8s service names, removing the need for an external registry (Eureka).

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
- **SMS alerts** are sent globally in real-time via the **Twilio API**. Requires Twilio credentials and an E.164 formatted phone number.
- **In-App Notifications** are written to the postgres `notification_db` and can be viewed or deleted dynamically by calling `/api/v1/notifications` from the frontend dashboard.

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
- **Kubernetes Environment**: Minikube, Docker Desktop K8s, or cloud-managed K8s cluster
- **kubectl**: installed and configured

### Step 1: Clone and Configure Environment
Copy `.env.example` to `.env` in the root directory and fill in your secrets (like Twilio credentials):
```bash
cp .env.example .env
```
Create the Kubernetes secret for the environment variables:
```bash
kubectl create secret generic banking-secrets --from-env-file=.env
```
By default, the application is pre-configured to locate resources on internal Kubernetes DNS.

### Step 2: Spin Up Full Stack using Kubernetes
You can deploy the entire infrastructure and microservices ecosystem to your Kubernetes cluster natively:
```bash
# 1. Start persistent infrastructure (Postgres, Redis, Kafka)
kubectl apply -f k8s/infrastructure.yaml

# 2. Deploy all microservices
kubectl apply -f k8s/services.yaml
```
This will create deployments, services, and persistent volumes for:
- Postgres (`5432` with PVC)
- Redis (`6379` with PVC)
- Kafka (`9092` with PVC)
- API Gateway (`8080`)
- Auth, User, Account, Transaction, Notification, Fraud, Analytics, Audit, Statement, and Admin Services

### Step 3: Expose the API Gateway
If you are running Minikube, you can port-forward the API gateway to access it locally:
```bash
kubectl port-forward svc/gateway 8081:8080
```

### Step 4: Start the Frontend Application
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

$user = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/register" `
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

$loginResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/login" `
    -Method Post `
    -Headers @{"Content-Type"="application/json"} `
    -Body $loginBody

$token = $loginResponse.accessToken
```

### 3. Check and Update Profile
```powershell
# Retrieve profile (triggers automatic profile creation)
$profile = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/users/profile" `
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

Invoke-RestMethod -Uri "http://localhost:8081/api/v1/users/profile" `
    -Method Put `
    -Headers @{"Authorization"="Bearer $token"; "Content-Type"="application/json"} `
    -Body $updateProfileBody
```

### 4. Open a Savings Account
```powershell
$accountBody = @{
    accountType = "SAVINGS"
} | ConvertTo-Json

$account = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/accounts" `
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

Invoke-RestMethod -Uri "http://localhost:8081/api/v1/transactions/deposit" `
    -Method Post `
    -Headers @{"Authorization"="Bearer $token"; "Content-Type"="application/json"} `
    -Body $depositBody
```

### 6. Verify Events & Notification Delivery
- **Email**: Check your configured SMTP email inbox to verify the transaction email alerts.
- **Kubernetes Pods**: Run `kubectl get pods` to see all running microservice instances.
- **SMS**: Check your verified phone for a live Twilio SMS. If it fails due to an unverified trial number, the error will appear in the `notification-service` logs (`kubectl logs -l app=notification-service`).
- **In-App Notifications**:
  ```powershell
  Invoke-RestMethod -Uri "http://localhost:8081/api/v1/notifications" `
      -Method Get `
      -Headers @{"Authorization"="Bearer $token"}
  ```
