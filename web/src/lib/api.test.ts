import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch } from './api.ts'

const fetchMock = vi.fn<typeof fetch>()

function respond(status: number) {
  return new Response(null, { status })
}

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock)
  document.cookie = 'XSRF-TOKEN=csrf-123; path=/'
})

afterEach(() => {
  fetchMock.mockReset()
  vi.unstubAllGlobals()
  document.cookie = 'XSRF-TOKEN=; path=/; max-age=0'
})

function headersOf(callIndex: number): Headers {
  return new Headers((fetchMock.mock.calls[callIndex][1] as RequestInit).headers)
}

describe('apiFetch', () => {
  it('sends the CSRF header on POST but not on GET', async () => {
    fetchMock.mockResolvedValue(respond(204))

    await apiFetch('/api/things', { method: 'POST' })
    await apiFetch('/api/things')

    expect(headersOf(0).get('X-XSRF-TOKEN')).toBe('csrf-123')
    expect(headersOf(1).get('X-XSRF-TOKEN')).toBeNull()
  })

  it('refreshes once on 401 and retries the original request', async () => {
    fetchMock
      .mockResolvedValueOnce(respond(401)) // original
      .mockResolvedValueOnce(respond(204)) // refresh
      .mockResolvedValueOnce(respond(200)) // retry

    const response = await apiFetch('/api/things')

    expect(response.status).toBe(200)
    expect(fetchMock.mock.calls.map((c) => c[0])).toEqual([
      '/api/things',
      '/api/auth/refresh',
      '/api/things',
    ])
    const refreshInit = fetchMock.mock.calls[1][1] as RequestInit
    expect(refreshInit.method).toBe('POST')
    expect(new Headers(refreshInit.headers).get('X-XSRF-TOKEN')).toBe('csrf-123')
  })

  it('returns the 401 and does not retry when the refresh fails', async () => {
    fetchMock.mockResolvedValueOnce(respond(401)).mockResolvedValueOnce(respond(401))

    const response = await apiFetch('/api/things')

    expect(response.status).toBe(401)
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('shares one refresh between simultaneous 401s', async () => {
    fetchMock.mockImplementation(async (input) => {
      const url = String(input)
      if (url === '/api/auth/refresh') return respond(204)
      // First call to each path is 401, the retry succeeds.
      const seen = fetchMock.mock.calls.filter((c) => String(c[0]) === url).length
      return respond(seen <= 1 ? 401 : 200)
    })

    const [a, b] = await Promise.all([apiFetch('/api/a'), apiFetch('/api/b')])

    expect([a.status, b.status]).toEqual([200, 200])
    const refreshCalls = fetchMock.mock.calls.filter((c) => c[0] === '/api/auth/refresh')
    expect(refreshCalls).toHaveLength(1)
  })

  it('never tries to refresh for the refresh or logout endpoints', async () => {
    fetchMock.mockResolvedValue(respond(401))

    await apiFetch('/api/auth/refresh', { method: 'POST' })
    await apiFetch('/api/auth/logout', { method: 'POST' })

    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
