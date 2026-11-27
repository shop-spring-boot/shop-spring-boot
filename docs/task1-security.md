# Task 1 – Default protection and the first SecurityFilterChain

Project: `shop-secure` (Spring Boot 4.2.0, Spring Security, Java 25).
Server: `http://localhost:8080`. Every output below was copied from a real run on 2026-11-27.

## What was built

| File | Purpose |
|---|---|
| `config/SecurityConfig.java` | Our `SecurityFilterChain`: `/api/public/**` is open, everything else needs login (HTTP Basic). CSRF is disabled for now (decided in Task 5). Also a BCrypt `PasswordEncoder` and a temporary in-memory user `user` / `password` (Task 2 moves users to the database). |
| `web/HelloController.java` | `GET /api/public/hello` (public) and `GET /api/private/hello` (needs login). |
| `SecurityTask1Test.java` | 4 MockMvc tests: public = 200, private without login = 401, wrong password = 401, correct login = 200. |

## 1. Public endpoint, no login

```
curl.exe -i http://localhost:8080/api/public/hello
```
```
HTTP/1.1 200
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/json
Content-Length: 28

{"message":"Hello, anyone!"}
```
`permitAll()` for `/api/public/**` works: no credentials needed.

## 2. Private endpoint, no login

```
curl.exe -i http://localhost:8080/api/private/hello
```
```
HTTP/1.1 401
Set-Cookie: JSESSIONID=D54CC38F0559D634D139E07CB897C7C2; Path=/; HttpOnly
WWW-Authenticate: Basic realm="Realm", charset="UTF-8"
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Length: 0
```
`anyRequest().authenticated()` rejects the request with **401** and `WWW-Authenticate: Basic`, which tells the client to send credentials.
Note the `JSESSIONID` cookie: by default Spring Security still creates an HTTP session. Task 4 (stateless JWT) removes this.

## 3. Private endpoint, wrong password

```
curl.exe -i -u user:wrong http://localhost:8080/api/private/hello
```
```
HTTP/1.1 401
WWW-Authenticate: Basic realm="Realm", charset="UTF-8"
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Set-Cookie: JSESSIONID=497C2D0CD9F8124D972C9BDFB99904EF; Path=/; HttpOnly
Content-Length: 0
```
The BCrypt check fails, so the answer is 401 again.

## 4. Private endpoint, correct login

```
curl.exe -i -u user:password http://localhost:8080/api/private/hello
```
```
HTTP/1.1 200
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/json
Content-Length: 26

{"message":"Hello, user!"}
```
The password is compared against the BCrypt hash and `Principal.getName()` returns `user`.

## 5. Tests

```
mvnw.cmd -q verify
```
The build finished with no failures and no test errors (the output only contains Spring Boot startup logs for `ShopSecureApplicationTests` and `SecurityTask1Test`).

## Summary of status codes

| Request | Result |
|---|---|
| `GET /api/public/hello`, no login | 200 |
| `GET /api/private/hello`, no login | 401 |
| `GET /api/private/hello`, wrong password | 401 |
| `GET /api/private/hello`, `user:password` | 200 |

## Security headers seen in every response

Spring Security adds these by default: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-cache, no-store`, `X-XSS-Protection: 0`. They are reviewed again in Task 5.

## Not captured

The "observe the default behaviour" step (generated password, everything locked with no config) is **not recorded here**. In the run above `/api/public/hello` already returned 200, so `SecurityConfig` was active, and the call with `<generated-password>` failed because the placeholder was typed literally. To record it: rename `SecurityConfig.java` to `SecurityConfig.java.bak`, run `mvnw.cmd spring-boot:run`, copy the line `Using generated security password: ...` and call `curl.exe -i http://localhost:8080/api/public/hello` (expected 401) and `curl.exe -i -u user:<the real password> http://localhost:8080/api/public/hello` (expected 200). Then rename the file back.
