# shop-secure (Lab-05: Spring Security + JWT)

A Spring Boot shop API secured with Spring Security. Users live in the database with BCrypt hashes, access is controlled by roles and method security, and the API is stateless: clients log in once and send a signed JWT (HS256) as a Bearer token. The challenge part adds rotating refresh tokens, login rate limiting, account lockout, a security audit log and an OWASP Top 10 checklist.

## Status

| Part | What it covers | Done |
|---|---|---|
| Task 1 | Default protection observed, first `SecurityFilterChain` | yes |
| Task 2 | JPA users, BCrypt hashes, `POST /api/register` | yes |
| Task 3 | Roles (`ADMIN`, `CUSTOMER`), `@PreAuthorize` / `@PostAuthorize`, role hierarchy | yes |
| Task 4 | JWT login, stateless resource server | yes |
| Task 5 | CSRF decision, CORS, security headers, XSS-safe output, secrets, security logging | yes |
| Challenge | Refresh tokens with rotation and reuse detection, rate limiting, account lockout, audit log, OWASP checklist, `AuthLifecycleIT` | yes |

The curl transcripts for each task are in [`docs/`](docs). The OWASP Top 10 table is in [`docs/owasp-checklist.md`](docs/owasp-checklist.md).

## Requirements

- JDK 21 or newer (`java.version` is 21 in `pom.xml`)
- Nothing else to install: the Maven wrapper (`mvnw`) is included
- Spring Boot 4.2.0, in-memory H2 database

## Run

The JWT secret and the seed passwords come from environment variables, so no secret is stored in the repository. `.env.example` lists the names.

PowerShell:
```powershell
$env:JWT_SECRET = "<at least 32 characters>"
$env:SHOP_SEED_ADMIN_PASSWORD = "<12+ chars, upper, lower, digit>"
$env:SHOP_SEED_CUSTOMER_PASSWORD = "<12+ chars, upper, lower, digit>"
./mvnw spring-boot:run
```

cmd:
```
set JWT_SECRET=<at least 32 characters>
set SHOP_SEED_ADMIN_PASSWORD=<value>
set SHOP_SEED_CUSTOMER_PASSWORD=<value>
mvnw.cmd spring-boot:run
```

The server starts on `http://localhost:8080`.

- `JWT_SECRET` is required, has no default, and must be at least 32 characters (the app refuses to start otherwise).
- The two seed passwords are only used to create the demo accounts below. Set your own.

Demo accounts created at startup (if missing): `admin` (roles `ADMIN`, `CUSTOMER`) and `alice` (role `CUSTOMER`).

## Quick try

Replace the values in capitals with your own. In Windows cmd do not leave `<` or `>` in a command, they are treated as file redirection.

```
curl.exe -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"alice\",\"password\":\"ALICE_PASSWORD\"}"
curl.exe -s localhost:8080/api/me -H "Authorization: Bearer ACCESS_TOKEN"
curl.exe -s -X POST localhost:8080/api/auth/refresh -H "Content-Type: application/json" -d "{\"refreshToken\":\"REFRESH_TOKEN\"}"
```

The login answer looks like this:
```json
{"token":"eyJ...","tokenType":"Bearer","expiresIn":900,"refreshToken":"kX9x...","refreshExpiresIn":604800}
```

## API

| Method and path | Who | Result |
|---|---|---|
| `POST /api/auth/login` | public | 200 with an access token (15 minutes) and a refresh token (7 days), 401 problem+json on bad credentials, 429 when rate limited |
| `POST /api/auth/refresh` | public (needs a valid refresh token) | 200 with a new access token and a new refresh token, 401 when the token is unknown, expired, revoked or reused |
| `POST /api/auth/logout` | public (needs the refresh token) | 204, revokes the whole token family |
| `POST /api/register` | public | 201 (new `CUSTOMER`), 400 on weak password or bad username, 409 if the username is taken |
| `GET /api/public/hello` | public | 200 |
| `GET /api/private/hello` | logged in | 200 |
| `GET /api/me` | logged in | name and roles of the caller |
| `POST /api/orders` | logged in | 201 with `Location`; the order always belongs to the caller |
| `GET /api/orders` | owner or `ADMIN` | own history; `?customer=` only for `ADMIN` |
| `GET /api/orders/{id}` | owner or `ADMIN` | 200, 403 for another customer's order, 404 if missing |
| `POST /api/orders/{id}/ship` | `ADMIN` | 204, 403 for `CUSTOMER` |
| `POST /api/reviews` | logged in | 201, input validated (stars 1 to 5, text up to 1000) |
| `GET /api/products/{id}/reviews` | public | reviews as JSON |
| `GET /products/{id}/reviews.html` | public | reviews page (Thymeleaf, escaped output) |
| `GET /api/admin/audit?username=` | `ADMIN` | the last 200 security events, optionally for one user |
| `PUT /api/admin/users/{username}/roles` | `ADMIN` | replaces the roles of a user (body `{"roles":["ADMIN","CUSTOMER"]}`), recorded as `ROLE_CHANGED` |

Errors are `application/problem+json`: 400, 401 (with `WWW-Authenticate: Bearer`), 403, 409 and 429 (with `Retry-After`).

## How the security works

**Filter chain** (`config/SecurityConfig`): `/api/auth/**`, `/api/public/**`, `/api/register` and the public review reads are open; `/api/admin/**` and `POST /api/orders/*/ship` need `ADMIN`; everything else needs authentication. Sessions are `STATELESS`, so there is no `JSESSIONID`.

**Users and passwords**: `UserAccount` entities with a set of `Role`s, loaded by `JpaUserDetailsService`. Passwords are stored as `{bcrypt}` hashes (delegating encoder), never as plain text. Registration requires a 12 to 72 character password with upper case, lower case and a digit.

**Roles and method security** (`config/MethodSecurityConfig`, `service/OrderService`): `@EnableMethodSecurity` with the hierarchy `ROLE_ADMIN > ROLE_CUSTOMER`. `place` and `history` use `@PreAuthorize` with an owner check (`#customerId == authentication.name`), `get` uses `@PostAuthorize` on the returned order, `ship` is `ADMIN` only. Role changes and the audit query are `ADMIN` only at both the URL and the method level.

**JWT** (`config/JwtConfig`, `service/TokenService`, `web/AuthController`): login authenticates against the database users and issues an HS256 access token with issuer `shop-api`, subject, issued-at, expiry (15 minutes by default, `shop.security.access-token-ttl`), a unique `jti` and a `roles` claim. Every other request is validated as an OAuth2 resource server: signature, expiry and issuer are checked and the `roles` claim becomes the authorities. Expired or invalid tokens get a 401 problem+json.

## Challenge: token life cycle, abuse protection and audit

**Refresh tokens with rotation** (`service/RefreshTokenService`, `domain/RefreshToken`)
- Login also returns an opaque refresh token: 32 random bytes from `SecureRandom`, base64url. Only its SHA-256 hash is stored, in the `refresh_tokens` table (`id`, `tokenHash`, `username`, `familyId`, `expiresAt` after 7 days, `revokedAt`, `replacedBy`).
- `POST /api/auth/refresh` exchanges a valid refresh token for a new access token and a new refresh token in the same family, and revokes the old one (`replacedBy` points to the new one).
- Presenting an already rotated token again means it was stolen or replayed. The whole family is revoked (so the newest token stops working too), `REFRESH_REUSE_DETECTED` is audited, and the client gets a generic 401. The revoke is committed even though the call ends with an exception (`noRollbackFor`).
- `POST /api/auth/logout` revokes the whole family. A locked, disabled or deleted account cannot refresh. The roles are read again from the database at every refresh, so a role change applies at the next refresh.
- A database row lock stops two requests from using the same refresh token at the same moment.

**Rate limiting** (`security/LoginRateLimiter`): `POST /api/auth/login` allows 5 attempts per minute per IP address and per username (a `ConcurrentHashMap` with expiring windows). The 6th attempt gets 429 problem+json with `Retry-After`.

**Account lockout** (`security/LockoutListener`, `domain/UserAccount`): driven by Spring Security's authentication events. After 5 consecutive failures the account is locked for 15 minutes (`failedAttempts`, `lockedUntil`). A locked account gets the same generic 401 `Bad credentials` as a wrong password, so clients cannot find out which accounts exist or are locked; only the audit log records `LOCKED`. A successful login resets the counter.

**Audit log** (`service/AuditService`, table `security_audit`): columns `at`, `type`, `username`, `ip`, `requestId`, `detail`. Rows are written in `REQUIRES_NEW` transactions, so they survive a rollback of the caller. Event types: `LOGIN_OK`, `LOGIN_FAILED`, `LOCKED`, `TOKEN_REFRESHED`, `REFRESH_REUSE_DETECTED`, `LOGOUT`, `ACCESS_DENIED`, `ROLE_CHANGED`. Only `ADMIN` can read it, with `GET /api/admin/audit?username=`.

**OWASP checklist**: [`docs/owasp-checklist.md`](docs/owasp-checklist.md) maps the ten 2021 categories to a control in this project and a test or transcript. A06 (outdated components) and A10 (SSRF) state the gap and the planned mitigation.

## Hardening (Task 5)

- **CSRF**: disabled on purpose. The API accepts only an `Authorization: Bearer` header and no cookies, so a browser can't attach credentials by itself. If a cookie-based same-origin SPA were added, CSRF would have to be turned on. Reasoning and code sample in `docs/task5-Spring Security-JWT.md`.
- **CORS**: only `http://localhost:5173`, methods GET, POST, PUT, DELETE, headers `Authorization` and `Content-Type`, exposes `Location`, preflight cached for 1 hour. Applies to `/api/**`.
- **Security headers**: `Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; object-src 'none'`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, `Strict-Transport-Security` (forced on over http only for local testing; it should only be sent over HTTPS in production), plus the Spring defaults (`X-Content-Type-Options: nosniff`, no-cache).
- **XSS**: the reviews page uses `th:text` (output encoding), review input is validated, and the CSP blocks inline and foreign scripts.
- **Secrets**: the JWT secret and seed passwords come from environment variables, `.env` is git-ignored, `.env.example` holds names only.
- **Logging**: a `SECURITY` logger records login, failure and access-denied events. Each request gets a short request id (`X-Request-Id` header and `rid=` in every log line), which is also stored in the audit table.

## Tests

```
./mvnw -q clean verify
```

No environment variable is needed for the tests: the build gives them a throw-away `JWT_SECRET` (surefire and failsafe configuration in `pom.xml`). Unit and MockMvc tests run in the `test` phase, the `*IT` classes run in the `integration-test` phase.

| Test class | Covers |
|---|---|
| `SecurityTask1Test` | public 200, private 401 without or with a wrong login, 200 with a correct one |
| `OrderServiceSecurityTest` | owner and `ADMIN` rules of the order service (method security) |
| `OrderControllerSecurityTest` | HTTP side of order access: anonymous 401, customer 403, admin 204, garbage token 401 |
| `AuthFlowIT` | register, login, access, expired token 401 |
| `AuthLifecycleIT` | login, access, refresh rotation, reuse of the old refresh token revokes the whole family; logout; lockout after 5 failures with a generic 401; 429 with `Retry-After`; audit log is `ADMIN` only and records `ACCESS_DENIED`, `LOCKED`, `REFRESH_REUSE_DETECTED`, `LOGOUT`, `ROLE_CHANGED`; role change applies at the next refresh |

## Project layout

```
shop-secure/
├── pom.xml
├── .env.example
├── docs/                      curl transcripts, notes and owasp-checklist.md
└── src/
    ├── main/java/edu/itc/shop/
    │   ├── ShopSecureApplication.java   seeds admin and alice
    │   ├── config/                      SecurityConfig, JwtConfig, MethodSecurityConfig,
    │   │                                CorsConfig, PasswordConfig, SecurityProps,
    │   │                                ProblemAuthenticationEntryPoint
    │   ├── domain/                      UserAccount, Role, Order, OrderStatus, Review,
    │   │                                RefreshToken, SecurityAudit, AuditType
    │   ├── repository/                  one repository per entity
    │   ├── security/                    JpaUserDetailsService, RequestIdFilter,
    │   │                                SecurityEventLogger, LockoutListener, LoginRateLimiter
    │   ├── service/                     AccountService, OrderService, PlaceOrder, TokenService,
    │   │                                RefreshTokenService, AuditService
    │   └── web/                         controllers (Auth, Admin, Order, Review, Register), DTOs,
    │                                    exception handlers
    ├── main/resources/                  application.properties, templates/reviews.html
    └── test/java/edu/itc/shop/          security, MockMvc and integration tests
```

## Known limits

- Rate limiting is in memory and per server; behind several servers it needs a shared store such as Redis. Behind a reverse proxy, set `server.forward-headers-strategy` so the real client address is used.
- After a role change, an access token that was already issued keeps its old roles until it expires (15 minutes at most). The next refresh uses the new roles.
- Expired refresh tokens are rejected but not deleted from `refresh_tokens`; a scheduled clean-up job is a planned improvement.
