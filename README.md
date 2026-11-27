# shop-secure (Lab-05: Spring Security + JWT)

A Spring Boot shop API secured with Spring Security. Users live in the database with BCrypt hashes, access is controlled by roles and method security, and the API is stateless: clients log in once and send a signed JWT (HS256) as a Bearer token.

## Status

| Part | What it covers | Done |
|---|---|---|
| Task 1 | Default protection observed, first `SecurityFilterChain` | yes |
| Task 2 | JPA users, BCrypt hashes, `POST /api/register` | yes |
| Task 3 | Roles (`ADMIN`, `CUSTOMER`), `@PreAuthorize` / `@PostAuthorize`, role hierarchy | yes |
| Task 4 | JWT login, stateless resource server | yes |
| Task 5 | CSRF decision, CORS, security headers, XSS-safe output, secrets, security logging | yes |

The curl transcripts for each task are in [`docs/`](docs).

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
- The two seed passwords are optional and only used to create the demo accounts below. Set your own.

Demo accounts created at startup (if missing): `admin` (roles `ADMIN`, `CUSTOMER`) and `alice` (role `CUSTOMER`).

## Quick try

```
curl.exe -i -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"alice\",\"password\":\"<alice password>\"}"
curl.exe -i http://localhost:8080/api/me -H "Authorization: Bearer <token>"
```

## API

| Method and path | Who | Result |
|---|---|---|
| `POST /api/auth/login` | public | 200 with a Bearer token and its lifetime, 401 problem+json on bad credentials |
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

Errors are `application/problem+json`: 401 (with `WWW-Authenticate: Bearer`), 403, 400, 409.

## How the security works

**Filter chain** (`config/SecurityConfig`): `/api/auth/**`, `/api/public/**`, `/api/register` and the public review reads are open; `POST /api/orders/*/ship` needs `ADMIN`; everything else needs authentication. Sessions are `STATELESS`, so there is no `JSESSIONID`.

**Users and passwords**: `UserAccount` entities with a set of `Role`s, loaded by `JpaUserDetailsService`. Passwords are stored as `{bcrypt}` hashes (delegating encoder), never as plain text. Registration requires a 12 to 72 character password with upper case, lower case and a digit.

**Roles and method security** (`config/MethodSecurityConfig`, `service/OrderService`): `@EnableMethodSecurity` with the hierarchy `ROLE_ADMIN > ROLE_CUSTOMER`. `place` and `history` use `@PreAuthorize` with an owner check (`#customerId == authentication.name`), `get` uses `@PostAuthorize` on the returned order, `ship` is `ADMIN` only.

**JWT** (`config/JwtConfig`, `web/AuthController`): login authenticates with the database users and issues an HS256 token with issuer `shop-api`, subject, issued-at, expiry (15 minutes by default, `shop.security.access-token-ttl`), a unique `jti` and a `roles` claim. Every other request is validated as an OAuth2 resource server: signature, expiry and issuer are checked and the `roles` claim becomes the authorities. Expired or invalid tokens get a 401 problem+json.

## Hardening (Task 5)

- **CSRF**: disabled on purpose. The API accepts only an `Authorization: Bearer` header and no cookies, so a browser can't attach credentials by itself. If a cookie-based same-origin SPA were added, CSRF would have to be turned on. Reasoning and code sample in `docs/task5-Spring Security-JWT.md`.
- **CORS**: only `http://localhost:5173`, methods GET, POST, PUT, DELETE, headers `Authorization` and `Content-Type`, exposes `Location`, preflight cached for 1 hour. Applies to `/api/**`.
- **Security headers**: `Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; object-src 'none'`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, `Strict-Transport-Security` (forced on over http only for local testing; it should only be sent over HTTPS in production), plus the Spring defaults (`X-Content-Type-Options: nosniff`, no-cache).
- **XSS**: the reviews page uses `th:text` (output encoding), review input is validated, and the CSP blocks inline and foreign scripts.
- **Secrets**: the JWT secret and seed passwords come from environment variables, `.env` is git-ignored, `.env.example` holds names only.
- **Logging**: a `SECURITY` logger records `login.ok`, `login.failed` and `access.denied` events. Each request gets a short request id (`X-Request-Id` header and `rid=` in every log line).

## Tests

```
./mvnw -q clean verify
```

The tests need `JWT_SECRET` available, so set the environment variables from the Run section in the same terminal first. They cover: the Task 1 rules (public 200, private 401 without or with a wrong login, 200 with a correct one), method security on `OrderService` (owner and `ADMIN` rules), the HTTP side of order access (401 and 403), and the full flow register, login and call with the token (`AuthFlowIT`, run it directly with `./mvnw -q -Dtest=AuthFlowIT test`).

## Project layout

```
shop-secure/
├── pom.xml
├── .env.example
├── docs/                      curl transcripts and notes for Tasks 1 to 5
└── src/
    ├── main/java/edu/itc/shop/
    │   ├── ShopSecureApplication.java   seeds admin and alice
    │   ├── config/                      SecurityConfig, JwtConfig, MethodSecurityConfig,
    │   │                                CorsConfig, PasswordConfig, SecurityProps,
    │   │                                ProblemAuthenticationEntryPoint
    │   ├── domain/                      UserAccount, Role, Order, OrderStatus, Review
    │   ├── repository/                  UserAccount, Order and Review repositories
    │   ├── security/                    JpaUserDetailsService, RequestIdFilter, SecurityEventLogger
    │   ├── service/                     AccountService, OrderService, PlaceOrder
    │   └── web/                         controllers, DTOs, exception handlers
    ├── main/resources/                  application.properties, templates/reviews.html
    └── test/java/edu/itc/shop/          security, MockMvc and flow tests
```
