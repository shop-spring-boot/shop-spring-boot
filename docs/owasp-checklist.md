
# OWASP Top 10 Security Checklist (2021)

Project: Shop Secure API

| ID | OWASP Risk | Project Control | Evidence / Status |
|---|---|---|---|
| A01 | Broken Access Control | Spring Security role-based access, `@PreAuthorize`, admin-only audit endpoint | AuthFlowIT and AuditAccessIT |
| A02 | Cryptographic Failures | BCrypt password hashing, HS256 JWT signing, SHA-256 refresh-token hashing | Authentication and refresh-token tests |
| A03 | Injection | Spring Data JPA repositories and parameterized queries | API integration tests; no unsafe dynamic SQL identified |
| A04 | Insecure Design | Refresh-token rotation, token-family reuse detection, login rate limiting, account lockout | AuthFlowIT |
| A05 | Security Misconfiguration | Stateless security configuration, security headers, CSRF and CORS configuration | SecurityConfig.java review |
| A06 | Vulnerable and Outdated Components | Dependency versions are managed through Spring Boot. Automated dependency vulnerability scanning is not yet configured. | GAP: Add OWASP Dependency-Check or another dependency scanner |
| A07 | Identification and Authentication Failures | BCrypt, JWT access tokens, 7-day refresh tokens, refresh-token rotation, account lockout | AuthFlowIT |
| A08 | Software and Data Integrity Failures | Maven dependency management and controlled source changes. Automated software-supply-chain verification is not yet configured. | GAP: Add dependency integrity and build verification checks |
| A09 | Security Logging and Monitoring Failures | Security event logger, security_audit table, request IDs, login and access-denied audit events | AuditAccessIT and security audit records |
| A10 | Server-Side Request Forgery (SSRF) | No dedicated SSRF protection or outbound request validation has been implemented because the current API has no server-side URL-fetching feature. | GAP: Add URL allowlisting and outbound network restrictions if such features are introduced |

## Implemented Audit Events

- LOGIN_OK
- LOGIN_FAILED
- LOCKED
- TOKEN_REFRESHED
- REFRESH_REUSE_DETECTED
- LOGOUT
- ACCESS_DENIED
- ROLE_CHANGED (audit hook available for future role-management implementation)

## Important Notes

- Never log passwords, JWT access tokens, raw refresh tokens, or token hashes.
- The in-memory login rate limiter is suitable for a single application instance in this lab, not a distributed production deployment.
- Audit records use a separate transaction so they can survive rollback of the main application transaction.
- Security controls must be reviewed and tested again before production deployment.
