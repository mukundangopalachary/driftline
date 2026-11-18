#!/usr/bin/env bash
# Generate a dev-only ES256 (P-256) key pair and append it to .env as single-line base64 PEM.
# Decisions: docs/decisions/0015-jwt-es256-docker-secrets.md. Dev only. Production keys are separate.
# Manual rotation: delete the two JWT_ lines from .env, run this again, restart the API.
set -euo pipefail

cd "$(dirname "$0")/.."
ENV_FILE=".env"
touch "$ENV_FILE"

# A copy of .env.example has blank JWT_ lines. Drop those so they do not count as existing keys.
sed -i '/^JWT_\(PRIVATE\|PUBLIC\)_KEY_B64=$/d' "$ENV_FILE"

if grep -q '^JWT_PRIVATE_KEY_B64=.' "$ENV_FILE"; then
  echo "JWT keys already in $ENV_FILE. Remove the JWT_* lines first to rotate." >&2
  exit 1
fi

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

# PKCS#8 private key (what Java's KeyFactory reads) and the matching public key.
openssl ecparam -name prime256v1 -genkey -noout \
  | openssl pkcs8 -topk8 -nocrypt -out "$tmp/private.pem"
openssl ec -in "$tmp/private.pem" -pubout -out "$tmp/public.pem" 2>/dev/null

{
  echo "JWT_PRIVATE_KEY_B64=$(base64 -w0 "$tmp/private.pem")"
  echo "JWT_PUBLIC_KEY_B64=$(base64 -w0 "$tmp/public.pem")"
} >> "$ENV_FILE"

echo "Wrote JWT_PRIVATE_KEY_B64 and JWT_PUBLIC_KEY_B64 to $ENV_FILE (git-ignored)."
