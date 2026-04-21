# whatsnext-auth-api

![CI](https://github.com/kevinaryeldev/whatsnext-auth-api/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)

Secure authentication REST API for the WhatsNext ecosystem.
Powers the login at [front-to-do](https://kevinaryeldev.github.io/front-to-do) and serves as a Rest Assured test target.

## Tech Stack

Spring Boot 3 · Spring Security 6 · PostgreSQL · Flyway · JJWT 0.12 · Bucket4j · SpringDoc

## Quick Start

**Prerequisites:** Docker, JDK 21, Maven 3.9+

```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

API: http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui.html

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/auth/register | Public | Register (3 req/min/IP) |
| POST | /api/v1/auth/login | Public | Login (5 req/min/IP) |
| POST | /api/v1/auth/refresh | Public | Rotate refresh token |
| POST | /api/v1/auth/logout | Bearer | Logout + blacklist |
| GET | /api/v1/users/me | Bearer | Authenticated profile |
| GET | /actuator/health | Public | Health check |

## Key Design Decisions

- **Refresh token is an opaque UUID** stored in the database — enables revocation without a blacklist query
- **Access token blacklist uses SHA-256 hash** — never stores raw tokens
- **Anti-enumeration**: wrong email and wrong password return identical 401 responses
- **BCrypt strength=12** — intentionally slower than the default
- **CORS is profile-scoped**: `localhost:5173` only in `local`, GitHub Pages only in `prod`
- **Rate limiting is in-memory** (Bucket4j) — resets on restart, documented trade-off for single-instance deploy

## Running Tests

```bash
mvn test                    # all tests (JUnit + MockMvc, H2)
mvn verify                  # tests + JaCoCo coverage report
```

Coverage report: `target/site/jacoco/index.html`

## Deploy (Render)

`render.yaml` at the root defines the full infrastructure.  
Set `JWT_SECRET` manually in the Render dashboard (min 32 chars).  
Override `DB_URL` with the JDBC format after the database is provisioned.

**Limitation:** The Render free tier spins down after 15 min of inactivity (~30s cold start on next request). Scheduled token cleanup jobs (`TokenCleanupService` at 03:00 and 03:30 UTC) will not run on nights when the instance is sleeping.

## Environment Variables

| Variable | Required in prod | Default |
|----------|-----------------|---------|
| DB_URL | Yes | — |
| DB_USERNAME | Yes | — |
| DB_PASSWORD | Yes | — |
| JWT_SECRET | Yes | — |
| CORS_ALLOWED_ORIGINS | No | https://kevinaryeldev.github.io |
| JWT_ACCESS_EXPIRATION | No | 900 (seconds) |
| JWT_REFRESH_EXPIRATION | No | 604800 (seconds) |
