# CSRF strategy

## Credentials accepted
The `/api/**` filter chain accepts only a Bearer JWT in the `Authorization` header
(obtained from `POST /api/auth/login`). It accepts no cookies and creates no session (STATELESS).

## Why CSRF is disabled here
A CSRF attack works because the browser attaches credentials (cookies) automatically to a
request sent from an evil site. An `Authorization: Bearer` header is never attached
automatically. JavaScript must add it, and another origin cannot read our token.
With no cookie credential there is nothing to forge, so CSRF protection adds nothing.

## If a same-origin SPA with a session cookie were added
The cookie would be sent automatically, so CSRF protection must be ON for that chain:

```java
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
```

The SPA reads the `XSRF-TOKEN` cookie and sends it back in a header:

```js
const token = decodeURIComponent(
  document.cookie.split('; ').find(c => c.startsWith('XSRF-TOKEN='))?.split('=')[1] ?? '');

fetch('/api/orders', {
  method: 'POST',
  credentials: 'same-origin',
  headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token },
  body: JSON.stringify({ productId: 1, quantity: 2 })
});
```

# shop-secure (Lab-05: Spring Security + JWT)

## Run
```powershell
$env:JWT_SECRET = "<at least 32 characters>"
$env:SHOP_SEED_ADMIN_PASSWORD = "<value>"
$env:SHOP_SEED_CUSTOMER_PASSWORD = "<value>"
./mvnw spring-boot:run
```
See `.env.example` for the variable names. Never commit real values.

## Task 1: default behaviour
Generated password line: `Using generated security password: <paste your line here>`
Produced by `UserDetailsServiceAutoConfiguration` (checked with `--debug`).

## What was built
1. Explicit `SecurityFilterChain`, problem+json 401/403
2. JPA users, bcrypt hashes, `/api/register`
3. Roles, `@PreAuthorize`, owner checks, role hierarchy
4. JWT login (HS256) and a stateless resource server
5. Hardening: CSRF decision (`docs/csrf.md`), CORS for http://localhost:5173, security headers,
   XSS-safe reviews (`th:text`), secrets in environment variables, `SECURITY` event logging

## Tests
```powershell
./mvnw -q clean verify
```