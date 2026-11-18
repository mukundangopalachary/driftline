# 0009: JWT for authenticated sessions

- Date: 2026-11-18
- Decided by: owner
- Status: accepted

## Decision

After GitHub login (see `0006-github-oauth-auth.md`), the API issues JWTs for authenticated requests.

## Not decided here

- Token lifetime, refresh strategy, revocation
- Where the browser stores the token (httpOnly cookie vs other)
- Signing algorithm and key management
