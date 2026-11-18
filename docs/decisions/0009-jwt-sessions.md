# 0009: JWT access tokens with refresh tokens

- Date: 2026-11-18
- Decided by: owner
- Status: accepted

## Decision

After GitHub login (see `0006-github-oauth-auth.md`), the API issues two tokens:

- **Access token:** a JWT. Lifetime **1 day**.
- **Refresh token:** lifetime **30 days**. Stored in **both Redis and Postgres**.

### Refresh flow

When the access token has expired, the API looks up the refresh token in Redis. On a cache miss it queries Postgres. A valid refresh token yields a new access token.

### Inactivity rule

If a user has no activity for **7 days**: blacklist their current access token and expire their refresh token.

### Revocation

Tokens can be revoked. Revoked access tokens go on a blacklist in Redis (see `0012-redis-token-blacklist.md`).

### Failure behavior

If Redis is down, **reject** requests (fail closed). Authenticated calls fail until Redis is back.

## Note on the inactivity rule

The access token only lives 1 day, so by day 7 it has already expired. Blacklisting it adds nothing for the inactivity case; expiring the refresh token is what ends the session. The blacklist matters for explicit revocation (logout, forced sign-out) inside the 1-day window. Recorded as the owner stated it. If activity is measured by refresh time, a user inactive for 7 days simply never refreshed in that period.

## Decided later the same day (owner)

- **Refresh token rotation:** a refresh token is replaced when it expires (30 days). It does not rotate on every use.
- **Refresh token format:** an opaque value, base64-encoded. Not a JWT.
- **Browser storage of the refresh token:** an httpOnly cookie. Should also be `Secure`, `SameSite`, and scoped to the refresh endpoint path (Claude's recommendation, not yet owner-confirmed). A cookie needs CSRF protection on the refresh endpoint.
- **Rate-limit revocation:** if a user sends too many requests, log them out and revoke their tokens (blacklist the access token, expire the refresh token). "Revoke" means that user's tokens, not the ES256 signing key.

## Notes

- Base64 is an encoding, not protection. The token is only secure if its bytes come from a cryptographically secure random source and are long enough. Whether the stored copy in Postgres and Redis is hashed is not decided (hashing means a database leak does not leak usable tokens).
- Rotating only at expiry means a stolen refresh token stays valid for up to 30 days unless revoked. This is the owner's choice.
- A request-rate threshold that logs users out can also hit legitimate heavy use (for example a dashboard polling). Threshold is open.

## Not decided here

- Key rotation scheme (signing is decided: ES256 with Docker secrets, see `0015-jwt-es256-docker-secrets.md`)
- Whether stored refresh tokens are hashed
- Where the browser keeps the access token (memory, cookie, other)
- Request-rate threshold and window for forced logout
- What counts as "activity" for the 7-day rule (any API call vs refresh only)
- Other revocation triggers (logout, user deletion, suspected compromise)
- How dev keys are generated and kept out of git (see `0015-jwt-es256-docker-secrets.md`)
