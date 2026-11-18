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

If a user has no activity for **14 days** (changed from 7, see below): blacklist their current access token and expire their refresh token.

### Revocation

Tokens can be revoked. Revoked access tokens go on a blacklist in Redis (see `0012-redis-token-blacklist.md`).

### Failure behavior

If Redis is down, **reject** requests (fail closed). Authenticated calls fail until Redis is back.

## Note on the inactivity rule

The access token only lives 1 day, so by day 14 it has already expired. Blacklisting it adds nothing for the inactivity case; expiring the refresh token is what ends the session. The blacklist matters for explicit revocation (logout, forced sign-out) inside the 1-day window. Recorded as the owner stated it. If activity is measured by refresh time, a user inactive for 14 days simply never refreshed in that period.

## Decided later the same day (owner)

- **Refresh token rotation:** a refresh token is replaced when it expires (30 days). It does not rotate on every use.
- **Refresh token format:** an opaque value, base64-encoded. Not a JWT.
- **Browser storage of the refresh token:** an httpOnly cookie. Should also be `Secure`, `SameSite`, and scoped to the refresh endpoint path (Claude's recommendation, not yet owner-confirmed). A cookie needs CSRF protection on the refresh endpoint.
- **Rate-limit revocation:** if a user sends too many requests, log them out and revoke their tokens (blacklist the access token, expire the refresh token). "Revoke" means that user's tokens, not the ES256 signing key.

## Confirmed and changed later the same day (owner)

- **Inactivity window is 14 days, not 7.** Reason: a user may review the code once every couple of weeks. This replaces the 7-day rule above. After 14 days with no activity: blacklist the current access token and expire the refresh token.
- **What counts as activity:** the user visiting the site. One visit per day is enough to count (daily granularity is fine, no need to track every request).
- **Rate-limit revocation confirmed:** revoke the user's access token and refresh token (not the signing key).
- **Cookie:** httpOnly confirmed. A **CSRF token** is also used.

## Decided later still, same day (owner)

- **Refresh token storage:** the token is generated random, base64-encoded and sent to the browser. The server stores only a **hash** of it (in Postgres and in Redis), never the raw value. On refresh, the server hashes the presented token and looks that up.
- **Access token storage:** also an **httpOnly cookie** (not JS memory).
- **CSRF:** use Spring Security's built-in CSRF protection.
- **Other revocation triggers:** the owner asked Claude to add any that were missed. See the list below.
- **Rate threshold:** the owner asked Claude to choose. See below.

## Claude's choices (delegated by owner, change any)

- **Hash function:** SHA-256 for the refresh token hash. A slow password hash (bcrypt, argon2) is not needed, because the token is long random data, not a guessable password. Token should be at least 32 random bytes from a secure random source.
- **CSRF repository:** Spring Security's default stores the CSRF token in an HTTP session, which would add server sessions to a stateless JWT design. The standard variant for a single-page app with cookies is `CookieCsrfTokenRepository` (a readable cookie plus a request header). Use that. Flagged because "Spring default" and "stateless" pull in different directions.
- **Cookie flags:** both cookies `HttpOnly`, `Secure`, `SameSite=Lax` or stricter. Refresh cookie scoped to the refresh endpoint path only. The access cookie goes with every API request. All state-changing requests need the CSRF token, since the access token is now a cookie.
- **Rate limit rule:** more than **300 authenticated requests per minute per user** (sliding or fixed one-minute window) triggers forced logout and revocation of that user's access and refresh tokens. Reasoning: one page of the dashboard is expected to make roughly 5 to 20 calls, so ordinary use (even with polling) stays well under 100 per minute. 300 leaves headroom and still catches scripts and runaway loops. Revisit with real traffic numbers. No separate throttle tier (429) is added, since the owner asked only for logout.
- **Revocation triggers (full list):**
  1. User logs out (this browser): blacklist its access token, delete its refresh token.
  2. User chooses "log out everywhere": revoke all of that user's tokens.
  3. Account deletion: revoke all of that user's tokens.
  4. Inactivity for 14 days: expire refresh token (and blacklist the current access token).
  5. Rate limit exceeded: revoke that user's tokens.
  6. Manual revoke by the owner or operator for a user (suspected compromise).
  7. Signing key compromise: rotate the ES256 key, which invalidates all access tokens (users re-authenticate or refresh).
  Not added: password change (there are no passwords), refresh token reuse detection (not applicable while tokens do not rotate on use).

## Deferred and confirmed (owner)

- **14-day inactivity rule: deferred.** Not built in Stage 1. Implement near the end of the project. The "visit" definition and the daily activity tracking are deferred with it. Until then, sessions end only by refresh token expiry (30 days) or another revocation trigger. The design above is kept as the intended behavior.
- **Rate limit confirmed:** 300 authenticated requests per minute per user, then forced logout and revocation of that user's access and refresh tokens.
- **Rate limit scope:** authenticated endpoints only. Unauthenticated endpoints get no rate limit for now. The owner will say if they want one.

## Notes

- Base64 is an encoding, not protection. The token is only secure if its bytes come from a cryptographically secure random source and are long enough. The server-side hash means a database or Redis leak does not leak usable tokens.
- Rotating only at expiry means a stolen refresh token stays valid for up to 30 days unless revoked. This is the owner's choice.
- Because both tokens are cookies, the CSRF token is now important for every state-changing request, not just refresh.

## Not decided here

- Key rotation scheme (signing is decided: ES256 with Docker secrets, see `0015-jwt-es256-docker-secrets.md`). Explained to the owner, can be deferred.
- ~~Where the request counter for the rate limit lives~~ decided: Redis, see `0012-redis-token-blacklist.md`
- Exact definition of a "visit" for the 14-day rule. Deferred with the rule itself, see below.

## Implemented (2026-11-18)

What the code in `api/` does, including choices made while building it.

**Tokens and cookies**
- Access token: ES256 JWT with `iss`, `sub` (user id), `jti`, `iat`, `exp` (1 day). Cookie `access_token`, path `/`.
- Refresh token: 32 random bytes, base64url without padding (URL-safe base64). Cookie `refresh_token`, path `/api/auth` only. Stored as a SHA-256 hash in Postgres (`refresh_tokens`) and Redis. Lifetime 30 days.
- Both cookies: `HttpOnly`, `SameSite=Lax`, `Secure` (set `COOKIE_SECURE=false` only if a browser refuses Secure on http://localhost).
- Only ES256 is accepted. Tokens signed with another key, expired, tampered, or unsigned (`alg: none`) are rejected. The Authorization header is not accepted.

**Endpoints** (`/api/auth`)
- `GET /me` current user. `POST /refresh` new access token from the refresh cookie. `POST /logout` this browser. `POST /logout-all` every token of this user.
- All POSTs need the CSRF header (`X-XSRF-TOKEN` copied from the `XSRF-TOKEN` cookie). Spring's CSRF protection with the cookie repository and the plain request handler (the default XOR handler would stop the frontend reading the value).
- `refresh` and `logout` do not read the access token, so an expired cookie cannot block them.

**Revocation** (resolves the open question on revoking all of a user's tokens)
- Single token: blacklist its `jti` in Redis until it would expire anyway.
- All of a user's tokens: store a per-user cutoff time in Redis. Access tokens issued at or before it are invalid. Refresh tokens are deleted from Postgres and Redis. The cutoff entry lives 1 day plus 5 minutes.
- Edge case: a token issued in the same second as the revoke counts as revoked (safe side). A real login takes longer than a second.

**Rate limit**
- 300 authenticated requests per minute per user, fixed one-minute windows, counter in Redis. Over the limit: revoke all of the user's tokens, clear the cookies, answer 401. Unauthenticated endpoints are not limited.
- A burst across a window boundary can count up to twice the limit briefly.

**Redis down**
- Token validation: the token is rejected (401). Rate limit filter, refresh and logout: 503. Postgres is not used as a fallback.

**Redis keys:** `refresh:<sha256>`, `jwt:blacklist:<jti>`, `jwt:revoked-before:<userId>`, `rl:<userId>:<minute>`.

**One deviation from "stateless"**
- The GitHub login handshake keeps its `state` value in a short-lived server session (Spring's default). The success handler invalidates it, and the request cache is switched off so nothing else creates a session. Tests check that ordinary requests never create one. This needs sticky sessions or a shared session store only if the API runs as several instances during a login. Single instance for now.

**Not built yet**
- 14-day inactivity rule (deferred by the owner).
- Account deletion endpoint (the foreign key already cascades to refresh tokens, and the revoke-all call exists).
- Manual revoke tool for the operator.
- Purging expired refresh token rows from Postgres. They stay until the user logs out. Harmless for now, add a scheduled cleanup later.
- Unauthenticated rate limits (owner will ask).
