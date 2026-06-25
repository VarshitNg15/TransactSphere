-- Database initialization script for TransactSphere
-- Creates separate databases for each microservice to maintain database-per-service isolation

CREATE DATABASE auth_db;
CREATE DATABASE user_db;
CREATE DATABASE account_db;
CREATE DATABASE transaction_db;
CREATE DATABASE notification_db;
CREATE DATABASE fraud_db;
CREATE DATABASE analytics_db;
CREATE DATABASE audit_db;
CREATE DATABASE statement_db;
