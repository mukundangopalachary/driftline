# 0015: ES256 signing with Docker secrets

- Date: 2026-11-18
- Decided by: owner
- Status: accepted

Part of the JWT design in `0009-jwt-sessions.md`.

## Decision

- Access tokens (JWTs) are signed with **ES256** (ECDSA, P-256, SHA-256).
- The key pair is supplied to services as **Docker secrets** (mounted files), not environment variables.

## Options considered

- Algorithms: HS256, RS256, ES256, EdDSA. ES256 chosen: asymmetric (verifiers can't forge tokens), small tokens, wide library support.
- Key management: env var or file, Docker secrets, rotation with `kid` and JWKS, Vault or cloud KMS.

## Consequences

- Only the service holding the private key (the API) can issue tokens. Any other service can verify with the public key alone.
- Key rotation is not part of this decision. Without a `kid` scheme, rotating the key invalidates all issued tokens.
- Docker secrets in plain Compose are file mounts. Production hosting is open, so the production secret source is still open.

## Amended later the same day (owner): dev keys

For **local development**, keys are generated with `openssl`, stored in a git-ignored `.env` file, and loaded into Spring Boot with a dotenv-style mechanism. This differs from the Docker secrets decision above, which stays as the intended approach for deployed environments. `.gitignore` already excludes `.env`, `*.pem`, `*.key` and `secrets/`.

Practical notes (not decisions):
- A PEM key spans several lines, which is awkward in `.env`. Options: store it base64-encoded on one line, or `\n`-escaped, and decode at startup.
- Docker Compose reads `.env` natively and can pass values to containers as environment variables. Running a service outside Docker needs a loader (for example a Spring config import of the `.env` file, or a dotenv library).
- Environment variables are easier to leak (process dumps, logs) than mounted secret files. That is why dev only.

## Key rotation (owner): manual swap

Rotation is a manual swap: replace the key pair and restart the API. All existing access tokens then fail verification and clients get a new one through the refresh flow. Users are not logged out, because refresh tokens are stored server-side and do not depend on the signing key. No `kid` or key overlap for now. Revisit if scheduled or zero-disruption rotation is needed.

## Not decided here

- Key rotation scheme (`kid`, JWKS endpoint, overlap period)
- Key encoding in `.env` and the loader library for Spring Boot (settled when `api/` is scaffolded)
- Production secret storage (depends on hosting)
- Whether other services verify tokens at all
