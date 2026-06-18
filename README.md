# Spring Boot JWT Auth

A Spring Boot 4 / Java 21 REST API implementing stateless JWT authentication with refresh token rotation, backed by PostgreSQL.

## Stack

- **Java 21**
- **Spring Boot 4.1** (Web, Security, Data JPA)
- **PostgreSQL**
- **JJWT 0.12.6** — JWT signing/parsing
- **Lombok** — boilerplate reduction

## Features

- User registration with BCrypt password hashing
- Stateless login issuing a short-lived **access token** (JWT) and a long-lived **refresh token** (opaque UUID, stored server-side)
- Refresh token **rotation** — every refresh issues a new pair and invalidates the old refresh token
- One active refresh token per user (logging in or refreshing revokes any previous one)
- Logout endpoint that revokes the refresh token
- Stateless `JwtAuthFilter` validating the `Authorization: Bearer <token>` header on every request
- CORS configured from environment

## Project Structure

```
src/main/java/app/
├── Application.java
├── config/
│   └── SecurityConfig.java        # Filter chain, CORS, password encoder, auth provider
├── controller/
│   └── AuthController.java        # /auth/register, /login, /refresh, /logout
├── dto/
│   └── Auth.java                  # Request/response records
├── entity/
│   ├── User.java
│   └── RefreshToken.java
├── repository/
│   ├── UserRepository.java
│   └── RefreshTokenRepository.java
├── security/
│   ├── JwtUtil.java                # Sign / parse / validate JWTs
│   └── JwtAuthFilter.java          # Authenticates each request from the Bearer token
└── service/
    ├── UserDetailsServiceImpl.java
    └── RefreshTokenService.java    # Create / verify / rotate / revoke refresh tokens
```

## Prerequisites

- JDK 21+
- PostgreSQL running locally (or update `.env` to point elsewhere)
- Maven (or use the bundled `mvnw` / `mvnw.cmd` wrapper)

## Configuration

Configuration is loaded from a `.env` file at the project root (see `application.properties`):

```env
DATABASE_URL=localhost
DATABASE_PORT=5432
DATABASE_NAME=jwt_auth
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=root
ALLOWED_ORIGINS=http://localhost:4321
JWT_SECRET=replace-with-a-long-random-256-bit-secret
```

> **Important:** Set `JWT_SECRET` to a strong, random value before running anywhere outside local dev. The fallback in `application.properties` is a placeholder only and must not be used in production.

Other JWT settings (in `application.properties`):

```properties
jwt.access-token-expiration-ms=900000      # 15 minutes
jwt.refresh-token-expiration-ms=604800000  # 7 days
```

## Running

Create the database first:

```bash
createdb jwt_auth
```

Then run the app:

```bash
./mvnw spring-boot:run
```

Schema is auto-managed via `spring.jpa.hibernate.ddl-auto=update`.

## API Reference

All endpoints are prefixed with `/auth` and are publicly accessible (no token required). Every other endpoint in the app requires a valid access token.

### Register

```
POST /auth/register
Content-Type: application/json

{
  "username": "alice",
  "password": "hunter2"
}
```

**200 OK**
```json
{ "message": "User registered successfully" }
```

**400 Bad Request** — username already taken
```json
{ "message": "Username already taken" }
```

### Login

```
POST /auth/login
Content-Type: application/json

{
  "username": "alice",
  "password": "hunter2"
}
```

**200 OK**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "tokenType": "Bearer"
}
```

### Refresh

Exchanges a valid refresh token for a new access/refresh pair. The old refresh token is invalidated (rotation).

```
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

**200 OK** — same shape as login.

### Logout

Revokes the user's refresh token.

```
POST /auth/logout
Content-Type: application/json

{
  "refreshToken": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

**200 OK**
```json
{ "message": "Logged out successfully" }
```

### Calling protected endpoints

Send the access token on every request:

```
GET /some/protected/endpoint
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

## Notes / Gotchas

- **Lombok requires explicit annotation-processor wiring** in `pom.xml` for this Spring Boot/Java combination — `maven-compiler-plugin` is configured with `annotationProcessorPaths` pointing at Lombok. If you bump Lombok's version, update `lombok.version` in `pom.xml`.
- `DaoAuthenticationProvider` in this Spring Security version only accepts a `UserDetailsService` in its constructor; the `PasswordEncoder` is set afterward via `setPasswordEncoder()`.
- Access tokens are stateless and **not** revocable before expiry — keep `jwt.access-token-expiration-ms` short. Refresh tokens are the revocable, stateful half of the system.
