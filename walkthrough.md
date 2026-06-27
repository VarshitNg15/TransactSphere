# Walkthrough: Phase 2 (Core Banking Services)

We have successfully completed the implementation of **Phase 2** of the Enterprise Banking platform. Below is a detailed walkthrough of the architectural components built, code organization, and instructions on how to test them.

---

## 1. Accomplished Tasks & Architecture

We built three new microservices at the root of the project and modified the parent configurations:

### Submodule Registration & Fixes
- Registered the new modules in the parent [pom.xml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/pom.xml) and configured the compile target to Java 21.
- Fixed compiling issues in `auth-service` by introducing the missing [InvalidTokenException.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/exception/InvalidTokenException.java) and [TokenRefreshRequest.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/auth-service/src/main/java/com/transactsphere/auth/dto/TokenRefreshRequest.java).

### User Service (`user-service`)
- Manages user profiles (first/last names, phone number, address, and KYC status).
- Exposes REST endpoints to query and update profiles. Auto-initializes profile details on first retrieval using Edge headers propagated by the Gateway.
- REST Controller: [UserProfileController.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/user-service/src/main/java/com/transactsphere/user/controller/UserProfileController.java).

### Account Service (`account-service`)
- Manages savings and current accounts. Employs balance caching in Redis to offload PostgreSQL query stress.
- Auto-generates unique 12-digit random account numbers starting with branch code `1000`.
- Exposes secure internal endpoints on `/internal/accounts/**` to allow atomic transaction balance execution.
- REST Controllers: [AccountController.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/account-service/src/main/java/com/transactsphere/account/controller/AccountController.java) (Public APIs) and [InternalAccountController.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/account-service/src/main/java/com/transactsphere/account/controller/InternalAccountController.java) (Internal endpoints for inter-service calls).

### Transaction Service (`transaction-service`)
- Processes deposits, withdrawals, and money transfers between accounts.
- Integrates **Spring Cloud OpenFeign** to query account details and execute balance updates synchronously inside the Account Service.
- Integrates **Spring Kafka** to publish a `transaction.completed` event to Apache Kafka for asynchronous downstream processing.
- REST Controller: [TransactionController.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/transaction-service/src/main/java/com/transactsphere/transaction/controller/TransactionController.java).

---

## 2. Created Files Map

### User Service Module
- Config: [pom.xml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/user-service/pom.xml) / [application.yml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/user-service/src/main/resources/application.yml)
- Main class: [UserApplication.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/user-service/src/main/java/com/transactsphere/user/UserApplication.java)
- JPA Model: [UserProfile.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/user-service/src/main/java/com/transactsphere/user/model/UserProfile.java)
- Service layer: [UserProfileService.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/user-service/src/main/java/com/transactsphere/user/service/UserProfileService.java)
- Controller layer: [UserProfileController.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/user-service/src/main/java/com/transactsphere/user/controller/UserProfileController.java)

### Account Service Module
- Config: [pom.xml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/account-service/pom.xml) / [application.yml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/account-service/src/main/resources/application.yml)
- Caching: [RedisConfig.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/account-service/src/main/java/com/transactsphere/account/config/RedisConfig.java)
- JPA Model: [Account.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/account-service/src/main/java/com/transactsphere/account/model/Account.java)
- Service layer: [AccountService.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/account-service/src/main/java/com/transactsphere/account/service/AccountService.java)
- Controllers: [AccountController.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/account-service/src/main/java/com/transactsphere/account/controller/AccountController.java) / [InternalAccountController.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/account-service/src/main/java/com/transactsphere/account/controller/InternalAccountController.java)

### Transaction Service Module
- Config: [pom.xml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/transaction-service/pom.xml) / [application.yml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/transaction-service/src/main/resources/application.yml)
- Feign Client: [AccountClient.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/transaction-service/src/main/java/com/transactsphere/transaction/client/AccountClient.java)
- JPA Model: [Transaction.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/transaction-service/src/main/java/com/transactsphere/transaction/model/Transaction.java)
- Service layer: [TransactionService.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/transaction-service/src/main/java/com/transactsphere/transaction/service/TransactionService.java)
- Controller: [TransactionController.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/transaction-service/src/main/java/com/transactsphere/transaction/controller/TransactionController.java)

---

## 3. How to Run and Verify Phase 2 Locally

### Step A: Spin Up Infrastructure
Start PostgreSQL database tables, Redis, Kafka broker, and MailHog:
```bash
docker compose -f docker/docker-compose.infra.yml up -d
```

### Step B: Build the Entire Codebase
Verify compilation and Maven packaging:
```powershell
mvn clean install
```
*(This builds all 5 services: Gateway, Auth, User, Account, and Transaction Services).*

### Step C: Start the Services
Start the following microservices in separate terminals or in the background:

1. **Auth Service (Port 8081)**
   ```powershell
   cd auth-service
   mvn spring-boot:run
   ```
2. **Gateway (Port 8080)**
   ```powershell
   cd gateway
   mvn spring-boot:run
   ```
3. **User Service (Port 8082)**
   ```powershell
   cd user-service
   mvn spring-boot:run
   ```
4. **Account Service (Port 8083)**
   ```powershell
   cd account-service
   mvn spring-boot:run
   ```
5. **Transaction Service (Port 8084)**
   ```powershell
   cd transaction-service
   mvn spring-boot:run
   ```

### Step D: Verification APIs via Gateway (Port 8080)

#### 1. Register and Log In to get an Access Token
```bash
# Register
Invoke-RestMethod -Uri http://localhost:8080/api/v1/auth/register -Method POST -Headers @{"Content-Type"="application/json"} -Body '{"username":"alice_test", "email":"alice@test.com", "password":"password123", "role":"CUSTOMER"}'

# Login
$response = Invoke-RestMethod -Uri http://localhost:8080/api/v1/auth/login -Method POST -Headers @{"Content-Type"="application/json"} -Body '{"username":"alice_test", "password":"password123"}'
$token = $response.accessToken

```
*Take note of the `"accessToken"` in the response payload.*

#### 2. Verify User Profile Auto-Creation
Query User Service through the gateway. Since the profile does not exist yet, the service auto-creates it dynamically using the Edge headers:
```bash
Invoke-RestMethod -Uri http://localhost:8080/api/v1/users/profile -Method GET -Headers @{"Authorization"="Bearer $token"}
```

#### 3. Update User Profile details
```bash
Invoke-RestMethod -Uri http://localhost:8080/api/v1/users/profile -Method PUT -Headers @{"Authorization"="Bearer $token"; "Content-Type"="application/json"} -Body '{"firstName":"Alice", "lastName":"Smith", "phoneNumber":"+1234567890", "email":"alice@test.com", "address":"123 Bank St, NY"}'

```

#### 4. Create Savings Account
```bash
$acc = Invoke-RestMethod -Uri http://localhost:8080/api/v1/accounts -Method POST -Headers @{"Authorization"="Bearer $token"; "Content-Type"="application/json"} -Body '{"accountType":"SAVINGS"}'
$acn = $acc.accountNumber
```
*Note down the `"accountNumber"` in the output (e.g. `100018349271`).*

#### 5. Deposit Funds
Deposit ₹5,000 into the account:
```bash
$body = '{"targetAccountNumber":"' + $acn + '", "amount":5000.00, "description":"Initial Deposit"}'
Invoke-RestMethod -Uri http://localhost:8080/api/v1/transactions/deposit -Method POST -Headers @{"Authorization"="Bearer $token"; "Content-Type"="application/json"} -Body $body
```

#### 6. Verify Balance in Account Service (Verify Caching)
Request the account info. The balance will show ₹5,000, and this request will be cached in Redis for fast access:
```bash
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts/$acn" `
  -Method Get `
  -Headers @{"Authorization"="Bearer $token"}
```

#### 7. Perform Transfer (Verify OpenFeign Integration)
Create a second account (e.g., account number `100049281729`) and transfer ₹1,500 from the first account to the second account:
```bash
$body = '{"sourceAccountNumber":"' + $acn + '", "targetAccountNumber":"' + $acn2 + '", "amount":1500.00, "channel":"INTERNAL", "description":"Rent Payment"}'
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/transactions/transfer" `
  -Method Post `
  -Headers @{"Authorization"="Bearer $token"; "Content-Type"="application/json"} `
  -Body $body
```

#### 8. Verify Kafka Event Publication
Verify that a message is successfully produced to Kafka by opening a consumer stream container-side:
```bash
docker exec -it kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic transaction.completed --from-beginning
```
*Expected output: JSON event details displaying source account, target account, ₹1,500 transfer amount, status `COMPLETED`, and timestamps.*
