# Food Delivery Platform (Microservices)

3 independent microservices (User, Restaurant, Order) built to showcase a modern backend stack: Spring Boot, Kafka messaging, and Keycloak-based security.

## Tech Stack
- **Backend:** Java 17, Spring Boot 3, Spring Data JPA, Spring Security (OAuth2)
- **Messaging:** Apache Kafka
- **Security:** Keycloak (SSO / OAuth2, RBAC)
- **Infrastructure:** Docker, Docker Compose, PostgreSQL, Liquibase
- **Frontend:** Next.js, TypeScript, Tailwind CSS

## Key Features
- Microservices with dedicated databases
- OAuth2 Resource Server integration via Keycloak
- Role-based access: `ROLE_USER` / `ROLE_ADMIN`
- Async communication via Kafka

## Keycloak + Google SSO + Email Verification

## Quickstart (local)

- Start infra (DBs + Keycloak + Kafka):
	- `docker compose --profile all up -d`
- Keycloak UI: `http://localhost:8080` (admin/admin)
- Realm import: on `docker compose --profile all up -d` Keycloak imports `master-realm.json` automatically.
- Databases exposed on host:
	- user-db: `localhost:5432` (db `user_db`, user `user`, pass `password`)
	- restaurant-db: `localhost:5433` (db `restaurant_db`, user `user`, pass `password`)
	- order-db: `localhost:5434` (db `order_db`, user `user`, pass `password`)
- Build all services:
	- `mvn -DskipTests -f pom.xml clean package`
- Run services (3 terminals):
	- `mvn -f user-service/pom.xml spring-boot:run`
	- `mvn -f restaurant-service/pom.xml spring-boot:run`
	- `mvn -f order-service/pom.xml spring-boot:run`

- Prereqs: Keycloak running, a realm (e.g. `master`), SMTP creds, and a Google OAuth client (Client ID/Secret).
- Issuer: Set `KEYCLOAK_ISSUER_URI` for all services to your realm issuer, e.g. `http://localhost:8080/realms/rgr`.
- Email verification: By default enforced. Disable with `security.require-email-verified=false` per service.

### Keycloak Realm/Client
- Create client: `account-console` or your app client. Access type: public or confidential as needed.
- Client scopes: Ensure `email` scope is included; tokens should carry `email` and `email_verified`.
- Roles: Create realm roles `ROLE_USER`, `ROLE_ADMIN` as needed and map them to users or groups.

### Google Identity Provider (IdP)
- In Google Cloud Console: Create OAuth 2.0 Client (Web), set Authorized redirect URI to `http://localhost:8080/realms/rgr/broker/google/endpoint`.
- This repo supports auto-bootstrapping Google IdP via docker-compose **without committing secrets**:
	- export env vars:
		- `export GOOGLE_CLIENT_ID=...`
		- `export GOOGLE_CLIENT_SECRET=...`
	- then run infra: `docker compose --profile all up -d`
	- the `keycloak-init` container will configure (create/update) the Google IdP in realm `rgr`.
- Manual option: Keycloak UI → Identity Providers → Google → set Client ID/Secret.

### SMTP for Verification Emails
- In Keycloak: Realm → Email → configure SMTP (host, port, from, auth). Enable `Verify email` in Realm → Login.
- Test: Send test email; ensure DMARC/SPF allow delivery from your SMTP.

### Service Configs
- user-service: `server.port=8084`. See `user-service/src/main/resources/application.yml` for DB and issuer.
- restaurant-service, order-service: Similar issuer config and `security.require-email-verified` flag.
- Env vars:
	- `KEYCLOAK_ISSUER_URI`: e.g. `http://localhost:8080/realms/rgr`
	- `REQUIRE_EMAIL_VERIFIED`: `true|false` to override default per service.

### Testing
- Public endpoints:
	- user-service: `POST /api/auth/register`, `POST /api/auth/login`
	- restaurant-service: public GETs for restaurants/dishes
- Protected endpoints require a JWT from Keycloak; ensure the token contains `email_verified=true` when enforcement is on.
	- user-service: `GET /api/users/by-keycloak-id/{keycloakId}` requires JWT (allowed only for the same subject or `ROLE_ADMIN`).

### Notes
- All services run as OAuth2 Resource Servers. If you customize roles, adapt `@PreAuthorize` usages accordingly.
- Liquibase is enabled; Flyway is disabled across services.
