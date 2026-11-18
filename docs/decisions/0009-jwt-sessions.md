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

## Not decided here

- Signing algorithm and key management (options given to the owner, awaiting choice)
- Whether refresh tokens rotate on use
- Refresh token format (random opaque value vs JWT) and whether stored hashed
- Where the browser stores tokens (httpOnly cookie vs other)
- What counts as "activity" for the 7-day rule (any API call vs refresh only)
- Other revocation triggers (logout, user deletion, suspected compromise)
