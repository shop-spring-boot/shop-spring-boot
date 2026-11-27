
# Security Session and Authentication Guide

## 1. Authentication Overview

The Shop Secure API uses Spring Security and JWT-based authentication.

- Passwords are stored using BCrypt hashing.
- Login returns a JWT access token and an opaque refresh token.
- Access tokens are valid for 15 minutes.
- Refresh tokens are valid for 7 days.
- The API uses stateless session management.

## 2. Login

Endpoint: `POST /api/auth/login`

Example request:

```json
{
  "username": "alice",
  "password": "your-password"
}
```

A successful login returns an access token and a refresh token.

Include the access token in protected API requests:

`Authorization: Bearer <access-token>`

## 3. Refresh Token Rotation

Endpoint: `POST /api/auth/refresh`

Send the current refresh token to obtain a new access token and refresh token.

The previous refresh token is revoked after successful rotation.

If a revoked refresh token is reused, the application detects reuse and revokes the entire token family.

Only the SHA-256 hash of each refresh token is stored in the database.

## 4. Logout

Endpoint: `POST /api/auth/logout`

Send the refresh token to revoke its token family.

A revoked refresh token cannot be used to obtain another access token.

## 5. Login Protection

The application implements:

- Maximum five login attempts per minute per IP address and username.
- HTTP 429 response when the login rate limit is exceeded.
- Account lockout after five consecutive failed login attempts.
- A 15-minute account lockout period.
- Successful login resets the failed-attempt counter.

## 6. Security Audit

Audit records are stored in the `security_audit` database table.

Recorded event types include:

- LOGIN_OK
- LOGIN_FAILED
- LOCKED
- TOKEN_REFRESHED
- REFRESH_REUSE_DETECTED
- LOGOUT
- ACCESS_DENIED
- ROLE_CHANGED (hook available for future role-management integration)

Administrators can access:

`GET /api/admin/audit`

An optional username filter is supported:

`GET /api/admin/audit?username=alice`

Only users with the ADMIN role can access this endpoint.

## 7. Environment Variables

Configure these environment variables before running the application:

- `JWT_SECRET`
- `SHOP_SEED_ADMIN_PASSWORD`
- `SHOP_SEED_CUSTOMER_PASSWORD`

See `.env.example` for placeholder values.

Never commit real secrets to Git.

## 8. Important Security Notes

- Never share passwords, access tokens, or refresh tokens.
- Never log raw tokens or passwords.
- Use HTTPS in production.
- The current in-memory rate limiter is intended for a single application instance.
- Review dependency vulnerabilities before production deployment.
- This project is a learning implementation and requires further security review before production use.
