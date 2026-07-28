Listed directory TransactSphere
Viewed docker-compose.yml:1-298

Here are 30 questions covering the architecture, technologies, and business domains of the TransactSphere project, broken down by difficulty:

### 🟢 Easy
1. **Service Discovery:** What is the role of the `discovery-server` (Eureka) in the TransactSphere microservices architecture?
2. **Authentication:** Which service is responsible for validating user credentials and issuing JWTs (JSON Web Tokens)?
3. **Data Storage:** What database is used as the primary relational data store across the microservices in this project?
4. **API Gateway:** In the provided architecture, which service acts as the single entry point for all client requests?
5. **Local Testing:** How does the system test sending emails in the local development environment without using a real SMTP provider?
6. **Orchestration:** Which tool and specific file are used to define and run this multi-container application locally?
7. **Messaging:** Which message broker is utilized for asynchronous communication between services (e.g., between `transaction-service` and `fraud-service`)?
8. **Caching:** What technology does TransactSphere use for caching, and which container provides this functionality?
9. **Networking:** In the `docker-compose.yml`, what is the purpose of the `banking-network`?
10. **Reverse Proxy:** What is the purpose of the `nginx` container sitting in front of the API gateway?

### 🟡 Medium
1. **Gateway Caching/Rate Limiting:** How does the `gateway` service utilize Redis in its operations?
2. **Event-Driven Flow:** Explain the interaction between the `transaction-service`, `kafka`, and the `audit-service`. How do events flow between them?
3. **Resilience:** If the `discovery-server` fails, how does it impact the ability of the `gateway` to route requests to the `user-service`?
4. **Kafka Configuration:** What role do the `KAFKA_CFG_PROCESS_ROLES` and `KAFKA_CFG_CONTROLLER_QUORUM_VOTERS` environment variables play in the Kafka container setup, and why is there no Zookeeper container?
5. **Data Persistence:** How is data persistence handled for the Postgres and Redis containers in Docker Compose to prevent data loss when the containers are destroyed?
6. **Container Dependencies:** Why does the `gateway` specify `depends_on` for both `redis` and `discovery-server`? How does this affect the startup sequence?
7. **Domain Logic:** Describe the potential flow of a user initiating a transfer, including how the `fraud-service` might be involved asynchronously via Kafka to approve or block it.
8. **Security Automation:** Explain how the `nginx` container automatically generates self-signed SSL certificates upon startup based on its `command` script.
9. **Configuration Management:** How are sensitive environment variables (like database credentials) passed to services such as `auth-service` without hardcoding them in the `docker-compose.yml`?
10. **Data Aggregation:** In a distributed architecture like TransactSphere, how might the `analytics-service` aggregate data from other domains (like `account-service` and `transaction-service`) without tightly coupling to their databases?

### 🔴 Hard
1. **Failure Handling:** Design a failure scenario where Kafka becomes temporarily unavailable. How should services like `notification-service` and `transaction-service` be implemented (e.g., Outbox pattern) to handle this gracefully without losing data?
2. **Production Security:** Discuss the security implications of exposing internal ports (e.g., Kafka's 9092, Postgres's 5432) to the host machine via Docker Compose, and propose a more secure networking configuration for a production Kubernetes deployment.
3. **Observability:** How would you implement distributed tracing (e.g., using Zipkin, Jaeger, or OpenTelemetry) across the TransactSphere services to track a single request from the `gateway` all the way to the `statement-service`?
4. **Performance Optimization:** Assuming the `account-service` experiences high read traffic (checking balances) and lower write traffic, how would you optimize its interaction with the Postgres database and Redis cache to ensure low latency?
5. **Distributed Transactions:** If a fraudulent transaction is detected by the `fraud-service` *after* the initial transfer is recorded, describe the Saga pattern implementation required to successfully rollback or compensate the transaction across `account-service` and `transaction-service` ensuring eventual consistency.
6. **Stateless Authentication:** The `auth-service` generates tokens. Explain how the `gateway` can be configured to validate these JWTs and implement Role-Based Access Control (RBAC) without making a synchronous, blocking network call to the `auth-service` for every single request.
7. **Database Migrations:** Propose a data migration strategy using tools like Flyway or Liquibase for the microservices. How do you handle a scenario where version 2.0 of `user-service` requires a breaking schema change while version 1.0 is still running (Blue-Green deployment)?
8. **Kubernetes Migration:** Looking at the `k8s` directory, how would the dependencies (`depends_on`) and service discovery mechanisms currently handled by Docker Compose and Eureka change when migrating this entire stack to native Kubernetes?
9. **Idempotency:** How should the `transaction-service` be designed to handle duplicate payment requests coming from the `frontend` (e.g., if a user clicks "Submit" twice due to network lag) to ensure a user's account is only debited once?
10. **Event Sourcing:** Contrast the current state-based architecture of the `account-service` with an Event Sourcing approach. What would be the architectural benefits and complexities of rebuilding the account balance dynamically from a Kafka event stream?