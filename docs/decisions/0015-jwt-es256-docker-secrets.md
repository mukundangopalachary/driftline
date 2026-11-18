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

## Not decided here

- Key rotation scheme (`kid`, JWKS endpoint, overlap period)
- How dev keys are generated and kept out of git
- Production secret storage (depends on hosting)
- Whether other services verify tokens at all
