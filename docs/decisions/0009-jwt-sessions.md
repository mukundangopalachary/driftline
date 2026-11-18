# 0009: JWT for authenticated sessions

- Date: 2026-11-18
- Decided by: owner
- Status: accepted

## Decision

After GitHub login (see `0006-github-oauth-auth.md`), the API issues JWTs for authenticated requests.

Added later the same day (owner):
- Refresh tokens are supported.
- Tokens can be revoked.
- A blacklist of revoked tokens is kept, in Redis (see `0012-redis-token-blacklist.md`).

## Not decided here

- Access and refresh token lifetimes
- Where refresh tokens are persisted (Postgres, Redis, or both) and whether they rotate on use
- Where the browser stores tokens (httpOnly cookie vs other)
- Signing algorithm and key management
- What triggers revocation (logout, password-less so no reset, suspected compromise, user deletion)
