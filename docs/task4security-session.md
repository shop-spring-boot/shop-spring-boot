# Task 4: JWT session

## Decoded token

Header: { "alg": "HS256" }
Payload: { "iss":"shop-api", "sub":"alice", "iat":..., "exp":..., "jti":"...", "roles":["ROLE_CUSTOMER"] }

The payload is only Base64URL-encoded, not encrypted, so anyone can read it. That is fine because it holds no secrets and the HMAC signature makes any change detectable.

## Transcript

(paste your curl outputs here)
