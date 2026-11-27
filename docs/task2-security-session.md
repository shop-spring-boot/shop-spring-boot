# Security Session Log — `shop-secure`

## Task 2 — Database-Backed Users, Password Encoding & Registration

### Summary of Implementation
- **Domain & Repository:**
  - `edu.itc.shop.domain.Role` (`CUSTOMER`, `ADMIN`)
  - `edu.itc.shop.domain.UserAccount` JPA entity mapped to `users` and `user_roles`
  - `edu.itc.shop.repository.UserAccountRepository` (`findByUsername`, `existsByUsername`)
- **Password Encoding:**
  - `edu.itc.shop.config.PasswordConfig` exposing `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (defaulting to bcrypt with `{bcrypt}` prefix)
- **Authentication:**
  - `edu.itc.shop.security.JpaUserDetailsService` loading users and roles from H2/JPA
  - Updated `edu.itc.shop.config.SecurityConfig` to remove the temporary Task 1 `InMemoryUserDetailsManager` and permit `/api/register`
- **Registration & Validation:**
  - `edu.itc.shop.service.AccountService` & `UsernameTakenException`
  - `edu.itc.shop.web.dto.RegisterRequest` enforcing username pattern (`[a-z0-9_.]+`, 3–40 chars) and password policy (12–72 chars, upper-case, lower-case, and digit)
  - `edu.itc.shop.web.RegisterController` (`POST /api/register`, `GET /api/me`)
  - `edu.itc.shop.web.ApiExceptionHandler` returning `application/problem+json` (`400 Bad Request` for validation errors, `409 Conflict` for duplicate usernames)
- **Seeding & Configuration:**
  - `CommandLineRunner` in `ShopSecureApplication` seeding `admin` (`ADMIN`, `CUSTOMER`) and `alice` (`CUSTOMER`)
  - `application.properties` configured with `jdbc:h2:mem:shop`, `spring.h2.console.enabled=false`, and `shop.seed.*` properties with environment variable overrides

---

### Verification (`curl` Session)

#### 1. Weak password rejected (`400 Bad Request`)
```bash
curl -i -X POST http://localhost:8080/api/register \
  -H "Content-Type: application/json" \
  -d '{"username":"bob","password":"short"}'
```
```http
HTTP/1.1 400
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/problem+json
Transfer-Encoding: chunked
Date: Fri, 27 Nov 2026 03:30:39 GMT
Connection: close

{"detail":"Validation failed","status":400,"title":"Bad Request","errors":[{"message":"must contain an upper-case letter, a lower-case letter and a digit","field":"password"},{"message":"size must be between 12 and 72","field":"password"}]}
```

#### 2. Valid registration (`201 Created`)
```bash
curl -i -X POST http://localhost:8080/api/register \
  -H "Content-Type: application/json" \
  -d '{"username":"bob","password":"Correct-Horse-9"}'
```
```http
HTTP/1.1 201
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/json
Content-Length: 18
Date: Fri, 27 Nov 2026 03:30:58 GMT

{"username":"bob"}
```

#### 3. Duplicate username rejected (`409 Conflict`)
```bash
curl -i -X POST http://localhost:8080/api/register \
  -H "Content-Type: application/json" \
  -d '{"username":"bob","password":"Correct-Horse-9"}'
```
```http
HTTP/1.1 409
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/problem+json
Transfer-Encoding: chunked
Date: Fri, 27 Nov 2026 03:31:06 GMT

{"detail":"Username already taken: bob","status":409,"title":"Conflict"}
```

#### 4. Login with newly registered DB user (`200 OK`)
```bash
curl -i -u bob:Correct-Horse-9 http://localhost:8080/api/me
```
```http
HTTP/1.1 200
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/json
Content-Length: 58
Date: Fri, 27 Nov 2026 03:31:14 GMT

{"roles":["ROLE_CUSTOMER","FACTOR_PASSWORD"],"name":"bob"}
```

---

### H2 Database Verification (`{bcrypt}` Hash)

Started temporarily with:
```cmd
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.h2.console.enabled=true"
```

Queried `jdbc:h2:mem:shop` via `/h2-console`:
```sql
SELECT id, username, password_hash, enabled FROM users;
```

| ID | USERNAME | PASSWORD_HASH | ENABLED |
|---|---|---|---|
| 3 | bob | `{bcrypt}$2a$10$<PASTE_YOUR_HASH_HERE>` | TRUE |