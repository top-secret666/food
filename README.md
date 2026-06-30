# Food Delivery Platform (Microservices)

Educational microservices backend: **user-service**, **restaurant-service**, **order-service** with Keycloak auth, PostgreSQL, Liquibase, and Kafka.

## Architecture

```text
Keycloak (:8080) ── JWT ──► user-service (:8084)
                         ├── restaurant-service (:8081)
                         └── order-service (:8082)
order-service ──HTTP──► user-service, restaurant-service
order-service ──Kafka──► restaurant-service (order notifications)
```

## Quickstart

### 1. Environment

Copy `.env.example` to `.env` and fill optional Google OAuth credentials.

### 2. Start infrastructure + apps

```bash
docker compose --profile all up -d --build
```

This starts Postgres, Kafka, Keycloak, Mailhog, and all three microservices.

### 3. Local development (without app containers)

```bash
docker compose --profile all up -d
# exclude app containers: use profiles dbs,kafka,keycloak only, or stop app services

mvn -f user-service/pom.xml spring-boot:run
mvn -f restaurant-service/pom.xml spring-boot:run
mvn -f order-service/pom.xml spring-boot:run
```

## Service URLs

| Service | URL | Swagger |
|---------|-----|---------|
| Keycloak | http://localhost:8080 | admin / admin |
| Mailhog | http://localhost:8025 | — |
| user-service | http://localhost:8084 | /swagger-ui/index.html |
| restaurant-service | http://localhost:8081 | /swagger-ui/index.html |
| order-service | http://localhost:8082 | /swagger-ui/index.html |

Health: `GET /actuator/health` on each service.

## Demo seed data (restaurants)

| Restaurant ID | Name | Sample dish IDs |
|---------------|------|-----------------|
| 1 | Burger House | 1 Classic Burger (450), 2 Cheese Burger (520), 3 Fries (180) |
| 2 | Pizza Roma | 4 Margherita (380), 5 Pepperoni (420), 6 Quattro Formaggi (460) |
| 3 | Sushi Zen | 7 California Roll (550), 8 Salmon Nigiri (320), 9 Miso Soup (150) |

## E2E flow (curl)

### Register and login (email/password)

```bash
# Register
curl -s -X POST http://localhost:8084/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123","fullName":"Test User"}'

# Verify email via Mailhog UI (http://localhost:8025), then login
curl -s -X POST http://localhost:8084/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
# Save access_token from response
```

### Profile

```bash
curl -s http://localhost:8084/api/users/me \
  -H "Authorization: Bearer <access_token>"
```

### Browse catalog

```bash
curl -s http://localhost:8081/api/restaurants
curl -s http://localhost:8081/api/restaurants/1/dishes
```

### Place order

```bash
curl -s -X POST http://localhost:8082/api/orders \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "paymentMethod": "CARD",
    "items": [{"dishId": 1, "quantity": 2}]
  }'
```

Expected `totalPrice`: 900 (2 × 450). Prices are fetched from restaurant-service, not from the client.

## Google OAuth

1. Create OAuth client in Google Cloud Console.
2. Redirect URI: `http://localhost:8080/realms/rgr/broker/google/endpoint`
3. Set in `.env`:
   ```
   GOOGLE_CLIENT_ID=...
   GOOGLE_CLIENT_SECRET=...
   ```
4. Restart Keycloak stack: `docker compose --profile keycloak up -d keycloak-init`
5. Login via Keycloak (Google button) or Authorization Code flow with `kc_idp_hint=google`.
6. Sync user to local DB:
   ```bash
   curl -s -X POST http://localhost:8084/api/auth/sync \
     -H "Authorization: Bearer <access_token>"
   ```

`GET /api/users/me` also auto-syncs on first access if the user exists in Keycloak but not yet in `user_db`.

## Environment variables

See [`.env.example`](.env.example) for the full list. Key variables:

- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` — Google IdP in Keycloak
- `REQUIRE_EMAIL_VERIFIED` — block unverified JWTs (default `true`)
- `CORS_ALLOWED_ORIGINS` — frontend origin (default `http://localhost:3000`)

## Tests

```bash
mvn -f user-service/pom.xml verify
mvn -f restaurant-service/pom.xml verify
mvn -f order-service/pom.xml verify
```

Integration tests use Testcontainers (requires Docker). Unit tests run without Docker.

## Legacy

- [`old-backend/`](old-backend/) — previous LoL tournament monolith (not used)
- [`front/`](front/) — legacy Next.js UI (separate frontend repo planned)

## Author

Dana Stukalova
