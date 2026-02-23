![maxresdefault](https://github.com/user-attachments/assets/eb9f611d-7be9-4b27-b985-86e7179c36e7)

# Food Delivery Platform (Microservices) 🍔

```text
┌──────────────────────────────────────────────────────────────────┐
│  QUEST: FOOD DELIVERY PLATFORM                                    │
│  MODE : MICROSERVICES • SECURITY • MESSAGING                      │
└──────────────────────────────────────────────────────────────────┘
```

Educational, production-like project demonstrating **microservices architecture** with secure auth and async communication.
3 independent microservices (User, Restaurant, Order) built to showcase a modern backend stack: Spring Boot, Kafka messaging, and Keycloak-based security.

##  Map (Architecture)

```text
		          ┌──────────────────────────┐
		          │        Keycloak          │
		          │   Auth / Roles / SSO     │
                  └───────────┬──────────────┘
                              │  JWT (OAuth2 Resource Server)
┌──────────────────┐          │          ┌─────────────────────┐
│   user-service    │─────────┼──────────│  restaurant-service  │
│  Spring Boot 3    │   Kafka events     │   Spring Boot 3      │
│  PostgreSQL +     │─────────┼──────────│  PostgreSQL +        │
│  Liquibase        │         │          │  Liquibase           │
└──────────────────┘          │          └─────────────────────┘
                              │
                        ┌─────▼─────┐
                        │ order-svc  │
                        │ Spring     │
                        │ PostgreSQL │
                        └────────────┘
```

**Services:** `user-service`, `restaurant-service`, `order-service` (each has its own DB).

##  Tech Stack

```text
Backend     : Java 17, Spring Boot 3, Spring Data JPA, Spring Security (OAuth2)
Messaging   : Apache Kafka
Security    : Keycloak (RBAC, email verification)
Data        : PostgreSQL, Liquibase
Infra       : Docker, Docker Compose
Frontend    : Next.js, TypeScript, Tailwind CSS
Docs/Tooling: OpenAPI/Swagger, Maven, GitHub Actions
```

##  Key Features

- **Microservices** with clear boundaries + dedicated databases
- **Secure auth** via Keycloak as OAuth2 Resource Server
- **RBAC**: `ROLE_USER` / `ROLE_ADMIN`
- **Async communication** via Kafka
- **Reproducible local setup** with Docker Compose

##  Quickstart (Local)

### Prerequisites
- Docker Desktop
- Java 17
- Maven

### 1) Start infrastructure

```bash
docker compose --profile all up -d
```

### 2) Build everything

```bash
mvn -DskipTests -f pom.xml clean package
```

### 3) Run services

```bash
mvn -f user-service/pom.xml spring-boot:run
mvn -f restaurant-service/pom.xml spring-boot:run
mvn -f order-service/pom.xml spring-boot:run
```

## 🔌 Useful Endpoints

- Keycloak UI: `http://localhost:8080` (default admin credentials are in the repo README/config)
- user-service: `http://localhost:8084`
- restaurant-service / order-service: see `application.yml` in each service

##  Project Goals

- Practice microservice architecture in a multi-service environment
- Build secure APIs and integrations (Keycloak, Kafka)
- Provide onboarding-friendly documentation and setup

## Author

Dana Stukalova — Java Backend (Intern / Junior)
