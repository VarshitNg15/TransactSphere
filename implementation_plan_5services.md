# Implementation Plan: Implementing Remaining Banking Microservices (Phase 5)

This plan details the design and implementation of the remaining 5 microservices defined in the API Gateway's `application.yml` but not yet implemented: **Fraud Service, Analytics Service, Audit Service, Statement Service, and Admin Service**.

These services will be added to the multi-module Maven project and integrated with PostgreSQL, Apache Kafka, and OpenFeign for synchronous and asynchronous communication.

---

## User Review Required

> [!IMPORTANT]
> **Database Initialization**
> - The databases `fraud_db`, `analytics_db`, `audit_db`, and `statement_db` are already declared in the local PostgreSQL setup.
> - **Admin Service** does not have a dedicated database in the original `init-db.sql`. We propose keeping the Admin Service stateless (no local database) and having it fetch information dynamically via OpenFeign client calls from other services, which is highly clean and lightweight.
> - **Hibernate DDL-Auto**: We will configure Hibernate DDL-Auto as `update` in `application.yml` for all services to auto-create tables upon startup.

> [!IMPORTANT]
> **API Gateway & Routing**
> - The API Gateway's [application.yml](file:///c:/Users/akars/Desktop/Java Stack/TransactSphere/TransactSphere/gateway/src/main/resources/application.yml) already has routing rules mapping `/api/v1/fraud/**`, `/api/v1/analytics/**`, `/api/v1/audit/**`, `/api/v1/statements/**`, and `/api/v1/admin/**` to their respective service ports (8086-8090).
> - No Gateway changes are needed.

---

## Open Questions

> [!NOTE]
> **Verification Tools**
> We will verify all 5 microservices compiling successfully via Maven and run local integration and unit tests. Do you have any specific requirements for additional security filters on the internal Feign endpoints, or should they follow the standard `X-User-Roles` header validation?

---

## Proposed Changes

### [Component] Parent Module Configuration

#### [MODIFY] [pom.xml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/pom.xml)
- Add new submodules under the `<modules>` block:
  ```xml
  <module>fraud-service</module>
  <module>analytics-service</module>
  <module>audit-service</module>
  <module>statement-service</module>
  <module>admin-service</module>
  ```

---

### [Component] Transaction Service Update

#### [NEW] [InternalTransactionController.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/transaction-service/src/main/java/com/transactsphere/transaction/controller/InternalTransactionController.java)
- Expose an internal GET endpoint to fetch transaction history for an account:
  - `GET /internal/transactions/account/{accountNumber}`: Fetches all completed/failed/fraudulent transactions matching this account as source or target.

---

### [Component] Fraud Service (`fraud-service`)
Responsible for consuming transaction alerts flagged as fraudulent from the `transaction.fraudulent` Kafka topic, persisting them, and providing management endpoints.

#### [NEW] [pom.xml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/fraud-service/pom.xml)
- Set up Maven dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-kafka`, `postgresql`, `lombok`, and testing tools.

#### [NEW] [application.yml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/fraud-service/src/main/resources/application.yml)
- Set port to `8086`.
- Connect to `jdbc:postgresql://${DB_HOST:localhost}:5432/fraud_db`.
- Configure Kafka consumer for topic `transaction.fraudulent`.

#### [NEW] [FraudApplication.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/fraud-service/src/main/java/com/transactsphere/fraud/FraudApplication.java)
- Spring Boot main entry point.

#### [NEW] [FraudLog.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/fraud-service/src/main/java/com/transactsphere/fraud/model/FraudLog.java)
- Entity: `id`, `transactionId`, `sourceAccountNumber`, `targetAccountNumber`, `amount`, `transactionType`, `status`, `timestamp`, `fraudReason`, `resolved` (boolean), `resolvedAt`, `resolvedBy`.

#### [NEW] [FraudLogRepository.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/fraud-service/src/main/java/com/transactsphere/fraud/repository/FraudLogRepository.java)
- JPA Repository for querying and modifying `FraudLog`.

#### [NEW] [FraudService.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/fraud-service/src/main/java/com/transactsphere/fraud/service/FraudService.java)
- Kafka Listener consuming from `transaction.fraudulent`. Saves flags into database.
- Resolution logic for resolving flagged attempts.

#### [NEW] [FraudController.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/fraud-service/src/main/java/com/transactsphere/fraud/controller/FraudController.java)
- REST APIs under `/api/v1/fraud/**`:
  - `GET /api/v1/fraud/logs` (Admin/Employee only): List all fraud flags.
  - `PUT /api/v1/fraud/resolve/{id}` (Admin/Employee only): Mark a log as resolved.

---

### [Component] Analytics Service (`analytics-service`)
Processes completed transactions from the `transaction.completed` Kafka topic to generate real-time user-level and global dashboard statistics.

#### [NEW] [pom.xml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/analytics-service/pom.xml)
- Standard multi-module dependency configuration on port `8087`, connecting to `analytics_db`.

#### [NEW] [application.yml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/analytics-service/src/main/resources/application.yml)
- Configure port `8087` and Kafka consumer for `transaction.completed` topic.

#### [NEW] [AnalyticsApplication.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/analytics-service/src/main/java/com/transactsphere/analytics/AnalyticsApplication.java)
- Main application launcher.

#### [NEW] [UserAnalytics.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/analytics-service/src/main/java/com/transactsphere/analytics/model/UserAnalytics.java)
- Entity: `id` (userId), `totalVolume`, `totalCount`, `depositVolume`, `withdrawalVolume`, `transferVolume`, `lastTransactionTimestamp`.

#### [NEW] [UserAnalyticsRepository.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/analytics-service/src/main/java/com/transactsphere/analytics/repository/UserAnalyticsRepository.java)
- Repository for `UserAnalytics`.

#### [NEW] [AnalyticsService.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/analytics-service/src/main/java/com/transactsphere/analytics/service/AnalyticsService.java)
- Kafka Listener consuming from `transaction.completed`.
- Aggregate analytics calculation logic.

#### [NEW] [AnalyticsController.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/analytics-service/src/main/java/com/transactsphere/analytics/controller/AnalyticsController.java)
- REST APIs under `/api/v1/analytics/**`:
  - `GET /api/v1/analytics/dashboard` (Admin/Employee only): System-wide stats.
  - `GET /api/v1/analytics/user/{userId}`: Specific user analytics.

---

### [Component] Audit Service (`audit-service`)
Maintains a general audit log of all financial events. It listens to Kafka topics for automated auditing and exposes an endpoint for other services to register general audits.

#### [NEW] [pom.xml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/audit-service/pom.xml)
- Port `8088`, connects to `audit_db`.

#### [NEW] [application.yml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/audit-service/src/main/resources/application.yml)
- Configures DB connectivity and consumes from `transaction.completed` and `transaction.fraudulent` Kafka topics.

#### [NEW] [AuditApplication.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/audit-service/src/main/java/com/transactsphere/audit/AuditApplication.java)
- Spring Boot Main class.

#### [NEW] [AuditLog.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/audit-service/src/main/java/com/transactsphere/audit/model/AuditLog.java)
- Entity: `id`, `eventType`, `message`, `userId`, `timestamp`, `serviceName`.

#### [NEW] [AuditLogRepository.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/audit-service/src/main/java/com/transactsphere/audit/repository/AuditLogRepository.java)
- Repository for querying `AuditLog`.

#### [NEW] [AuditService.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/audit-service/src/main/java/com/transactsphere/audit/service/AuditService.java)
- Kafka listeners for automated transaction audits.
- Core save logic for manual audits.

#### [NEW] [AuditController.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/audit-service/src/main/java/com/transactsphere/audit/controller/AuditController.java)
- REST APIs under `/api/v1/audit/**`:
  - `POST /api/v1/audit/logs`: Exposes manual logging for other microservices.
  - `GET /api/v1/audit/logs` (Admin/Employee only): List audit entries.

---

### [Component] Statement Service (`statement-service`)
Generates bank account statement summaries and printable reports for specific accounts within chosen date windows.

#### [NEW] [pom.xml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/statement-service/pom.xml)
- Configures dependencies including `spring-cloud-starter-openfeign`, JPA, PostgreSQL on port `8089`.

#### [NEW] [application.yml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/statement-service/src/main/resources/application.yml)
- Configures port `8089` and `statement_db` database connection.

#### [NEW] [StatementApplication.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/statement-service/src/main/java/com/transactsphere/statement/StatementApplication.java)
- Configured with `@EnableFeignClients`.

#### [NEW] [TransactionClient.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/statement-service/src/main/java/com/transactsphere/statement/client/TransactionClient.java)
- Feign Client to query the `transaction-service` internal `/internal/transactions/account/{accountNumber}` endpoint.

#### [NEW] [StatementLog.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/statement-service/src/main/java/com/transactsphere/statement/model/StatementLog.java)
- Entity: `id`, `userId`, `accountNumber`, `startDate`, `endDate`, `generatedAt`.

#### [NEW] [StatementLogRepository.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/statement-service/src/main/java/com/transactsphere/statement/repository/StatementLogRepository.java)
- Database repository for logging statement operations.

#### [NEW] [StatementService.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/statement-service/src/main/java/com/transactsphere/statement/service/StatementService.java)
- Generates JSON summary and formats HTML/CSV bank statement reports.

#### [NEW] [StatementController.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/statement-service/src/main/java/com/transactsphere/statement/controller/StatementController.java)
- REST APIs under `/api/v1/statements/**`:
  - `GET /api/v1/statements/account/{accountNumber}`: Returns JSON statement summary.
  - `GET /api/v1/statements/account/{accountNumber}/download`: Returns a downloadable text/CSV report.

---

### [Component] Admin Service (`admin-service`)
Acts as the central control panel dashboard. It gathers global service metrics, aggregates totals, and performs administrative updates like account freezes or KYC approvals by coordinating downstream services.

#### [NEW] [pom.xml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/admin-service/pom.xml)
- Configures dependencies including `spring-cloud-starter-openfeign` on port `8090` without database dependencies.

#### [NEW] [application.yml](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/admin-service/src/main/resources/application.yml)
- Configures port `8090`.

#### [NEW] [AdminApplication.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/admin-service/src/main/java/com/transactsphere/admin/AdminApplication.java)
- Spring Boot entry point with `@EnableFeignClients`.

#### [NEW] [Clients](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/admin-service/src/main/java/com/transactsphere/admin/client/)
- Downstream clients:
  - `UserClient.java`: Call `user-service` to manage KYC.
  - `AccountClient.java`: Call `account-service` to freeze/unfreeze accounts.
  - `FraudClient.java`: Call `fraud-service` to get and resolve fraud logs.
  - `AnalyticsClient.java`: Call `analytics-service` to get dashboard stats.

#### [NEW] [AdminService.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/admin-service/src/main/java/com/transactsphere/admin/service/AdminService.java)
- Service logic to gather platform statistics and forward administration requests.

#### [NEW] [AdminController.java](file:///c:/Users/akars/Desktop/Java%20Stack/TransactSphere/TransactSphere/admin-service/src/main/java/com/transactsphere/admin/controller/AdminController.java)
- REST APIs under `/api/v1/admin/**` (restricted to Admin roles):
  - `GET /api/v1/admin/stats`: Get dashboard metrics (users, accounts, fraud count).
  - `PUT /api/v1/admin/users/{userId}/kyc`: Update a user's KYC status.
  - `PUT /api/v1/admin/accounts/{accountNumber}/freeze`: Freeze/unfreeze an account.
  - `GET /api/v1/admin/fraud/logs`: List fraud incidents.
  - `PUT /api/v1/admin/fraud/resolve/{id}`: Resolve a fraud incident.

---

## Verification Plan

### Automated Tests
- Run maven build from the root directory to confirm all submodules build cleanly:
  ```bash
  mvn clean install -DskipTests
  ```

### Manual Verification
1. Spin up the infrastructure via docker-compose.
2. Start all 5 new microservices alongside the existing ones.
3. Call `GET /api/v1/analytics/dashboard` through the API Gateway using a valid JWT to verify the Analytics Service.
4. Attempt a fraudulent transaction to trigger `transaction.fraudulent` Kafka publication, and verify it populates a record in `fraud_db` under `fraud-service`.
5. Call `GET /api/v1/statements/account/{accountNumber}` to generate and print statements.
6. Verify Admin Service proxy calls using the Admin endpoints.
