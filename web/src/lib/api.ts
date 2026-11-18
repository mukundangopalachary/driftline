// Calls to the Driftline API. Both auth tokens live in httpOnly cookies the browser sends by itself,
// so this code never sees them (decisions/0009). What it does handle:
//  - CSRF: copy the XSRF-TOKEN cookie into the X-XSRF-TOKEN header on state-changing requests
//  - expired access token: on 401, ask the API for a new one using the refresh cookie, then retry once

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

// These endpoints never trigger a refresh: refresh itself, and logout (which must always just run).
const NO_REFRESH_PATHS = new Set(['/api/auth/refresh', '/api/auth/logout'])

export function readCookie(name: string): string | undefined {
  const prefix = `${name}=`
  const found = document.cookie.split('; ').find((c) => c.startsWith(prefix))
  return found ? decodeURIComponent(found.slice(prefix.length)) : undefined
}

function csrfHeaders(): Record<string, string> {
  const token = readCookie('XSRF-TOKEN')
  return token ? { 'X-XSRF-TOKEN': token } : {}
}

// Several requests can fail with 401 at the same moment. They share one refresh call.
let refreshInFlight: Promise<boolean> | null = null

function refreshSession(): Promise<boolean> {
  refreshInFlight ??= fetch('/api/auth/refresh', {
    method: 'POST',
    credentials: 'same-origin',
    headers: csrfHeaders(),
  })
    .then((response) => response.ok)
    .catch(() => false)
    .finally(() => {
      refreshInFlight = null
    })
  return refreshInFlight
}

export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const method = (init.method ?? 'GET').toUpperCase()

  const send = () => {
    const headers = new Headers(init.headers)
    if (!SAFE_METHODS.has(method)) {
      for (const [name, value] of Object.entries(csrfHeaders())) headers.set(name, value)
    }
    return fetch(path, { ...init, method, headers, credentials: 'same-origin' })
  }

  const response = await send()
  if (response.status === 401 && !NO_REFRESH_PATHS.has(path)) {
    if (await refreshSession()) {
      return send()
    }
  }
  return response
}
