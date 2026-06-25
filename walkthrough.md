# Walkthrough: Phase 1 (Infrastructure & Authentication)

We have completed the implementation of **Phase 1** of the Enterprise Banking platform. Below is a detailed summary of the architectural components built and instructions on how to run and test them.

---

## 1. Accomplished Tasks

### Infrastructure Setup
- Created `docker/postgres/init-db.sql`: Automatically spins up all 9 separate database schemas for the microservices to enforce the **Database-per-Service** isolation pattern.
- Created `docker/docker-compose.infra.yml`: Orchestrates:
  - **PostgreSQL**: Standard relational store for business transaction logs.
  - **Redis**: Fast, key-value memory store for JWT blacklists, caching, and API Gateway rate-limit counts.
  - **Apache Kafka (KRaft mode)**: Standard event streaming bus (dual listener setup allows container-to-container routing inside Docker and local debugging from the host machine on port `29092`).
  - **MailHog**: Mock SMTP server with a web UI on port `8025` to inspect outbound emails without third-party email providers.

### API Gateway (`gateway`)
- Exposes port `8080`.
- Configured dynamic routing mapping requests (e.g. `/api/v1/auth/**` -> `auth-service`).
- Implemented **JWT Edge Validation** (`JwtAuthenticationFilter.java`): Intercepts requests, validates signatures, parses user metadata (ID, name, roles), and injects them as headers (`X-User-Id`, `X-User-Name`, `X-User-Roles`) to secure internal backend communications.
- Configured **Redis Token Bucket Rate Limiting** (`RateLimiterConfig.java`): Limits api calls dynamically per IP address.

### Authentication Service (`auth-service`)
- Exposes port `8081`.
- Built JPA persistence mapping: `users` table mapped in `auth_db`.
- Integrated **Spring Security 6 & BCrypt**: Password hashing and authentication.
- Implemented **JWT Generation & Refresh**: Creates 1-hour access tokens and 7-day refresh tokens.
- Implemented **Redis Blacklist Session Invalidation** (`JwtService.java`): Revokes JWT validity on logout.

---

## 2. Created Files Map

- **Parent POM**: [pom.xml](file:///e:/Project/TransactSphere/pom.xml)
- **Infrastructure Compose**: [docker-compose.infra.yml](file:///e:/Project/TransactSphere/docker/docker-compose.infra.yml)
- **Database Init SQL**: [init-db.sql](file:///e:/Project/TransactSphere/docker/postgres/init-db.sql)
- **Gateway Module**:
  - [pom.xml](file:///e:/Project/TransactSphere/gateway/pom.xml)
  - [application.yml](file:///e:/Project/TransactSphere/gateway/src/main/resources/application.yml)
  - [GatewayApplication.java](file:///e:/Project/TransactSphere/gateway/src/main/java/com/transactsphere/gateway/GatewayApplication.java)
  - [RateLimiterConfig.java](file:///e:/Project/TransactSphere/gateway/src/main/java/com/transactsphere/gateway/config/RateLimiterConfig.java)
  - [JwtAuthenticationFilter.java](file:///e:/Project/TransactSphere/gateway/src/main/java/com/transactsphere/gateway/filter/JwtAuthenticationFilter.java)
  - [Dockerfile](file:///e:/Project/TransactSphere/gateway/Dockerfile)
- **Auth Service Module**:
  - [pom.xml](file:///e:/Project/TransactSphere/auth-service/pom.xml)
  - [application.yml](file:///e:/Project/TransactSphere/auth-service/src/main/resources/application.yml)
  - [AuthApplication.java](file:///e:/Project/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/AuthApplication.java)
  - [SecurityConfig.java](file:///e:/Project/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/config/SecurityConfig.java)
  - [RedisConfig.java](file:///e:/Project/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/config/RedisConfig.java)
  - [User.java](file:///e:/Project/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/model/User.java)
  - [Role.java](file:///e:/Project/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/model/Role.java)
  - [RegisterRequest.java](file:///e:/Project/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/dto/RegisterRequest.java)
  - [LoginRequest.java](file:///e:/Project/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/dto/LoginRequest.java)
  - [AuthResponse.java](file:///e:/Project/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/dto/AuthResponse.java)
  - [TokenRefreshRequest.java](file:///e:/Project/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/dto/TokenRefreshRequest.java)
  - [UserRepository.java](file:///e:/Project/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/repository/UserRepository.java)
  - [AuthService.java](file:///e:/Project/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/service/AuthService.java)
  - [JwtService.java](file:///e:/Project/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/service/JwtService.java)
  - [AuthController.java](file:///e:/Project/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/controller/AuthController.java)
  - [GlobalExceptionHandler.java](file:///e:/Project/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/exception/GlobalExceptionHandler.java)
  - [Dockerfile](file:///e:/Project/TransactSphere/auth-service/Dockerfile)

---

## 3. How to Start and Verify Phase 1 Locally

Since the IDE's execution tool has a permission restriction for launching subprocesses on your machine, you can easily run these commands in your standard PowerShell, Cmd, or VS Code terminal:

### Step A: Spin Up the Infrastructure
Navigate to the root directory and run:
```bash
docker compose -f docker/docker-compose.infra.yml up -d
```
Check that the databases, Redis cache, MailHog, and Kafka KRaft cluster are running:
```bash
docker ps
```

### Step B: Build the Services
In the root directory of the project, compile the Maven projects and run the automated tests:
```bash
mvn clean install
```
*(This will compile the parent POM, the `gateway` module, the `auth-service` module, and run the controller and service unit test suites).*

### Step C: Boot the Microservices
In separate terminal windows (or in the background), start the Gateway and Auth Service:

**Start Auth Service (Port 8081)**
```bash
cd auth-service
mvn spring-boot:run
```

**Start Gateway Service (Port 8080)**
```bash
cd gateway
mvn spring-boot:run
```

### Step D: Perform API Verification Tests

#### 1. Register a Customer User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "customer_one",
    "email": "customer_one@bank.com",
    "password": "securepassword123",
    "role": "CUSTOMER"
  }'
```
*Expected Output:* `{"message":"User registered successfully"}` with HTTP Status `201 Created`.

#### 2. Log In
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "customer_one",
    "password": "securepassword123"
  }'
```
*Expected Output:* An `accessToken`, `refreshToken`, and user attributes with HTTP Status `200 OK`.

#### 3. Access a Protected Route Without Token
Let's query a fake route on the user service through the Gateway:
```bash
curl -i -X GET http://localhost:8080/api/v1/users/profile
```
*Expected Output:* HTTP Status `401 Unauthorized` due to gateway JWT filter validation failure.

#### 4. Access a Protected Route With Token
```bash
curl -i -X GET http://localhost:8080/api/v1/users/profile \
  -H "Authorization: Bearer <PASTE_YOUR_ACCESS_TOKEN_HERE>"
```
*Expected Output:* Since `user-service` is not yet running, you will get a gateway forwarding connection error (e.g. `503 Service Unavailable` or `504 Gateway Timeout`), showing that the gateway passed the security check and successfully attempted to forward the request to the backend microservice!
