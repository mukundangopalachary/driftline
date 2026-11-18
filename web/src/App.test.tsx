import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App.tsx'

const fetchMock = vi.fn<typeof fetch>()

function renderApp(route: string) {
  vi.stubGlobal('fetch', fetchMock)
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[route]}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

afterEach(() => {
  fetchMock.mockReset()
  vi.unstubAllGlobals()
})

describe('App, signed out', () => {
  // Every API call is 401, including the refresh attempt.
  const signedOut = () =>
    fetchMock.mockImplementation(async () => new Response(null, { status: 401 }))

  it('shows the home page with a sign in link', async () => {
    signedOut()
    renderApp('/')
    expect(screen.getByRole('heading', { name: 'Driftline' })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Sign in' })).toHaveAttribute('href', '/login')
  })

  it('login page links to the GitHub login on the API', async () => {
    signedOut()
    renderApp('/login')
    expect(await screen.findByRole('link', { name: 'Sign in with GitHub' })).toHaveAttribute(
      'href',
      '/api/auth/oauth2/authorization/github',
    )
  })

  it('login page explains a failed GitHub sign-in', async () => {
    signedOut()
    renderApp('/login?error=oauth')
    expect(await screen.findByRole('alert')).toHaveTextContent('did not complete')
  })
})

describe('App, signed in', () => {
  const me = { id: 'u1', githubLogin: 'octocat', name: 'Octo Cat', avatarUrl: null }

  it('shows the user and signs out', async () => {
    fetchMock.mockImplementation(async (input) => {
      if (String(input) === '/api/auth/me') return json(me)
      return new Response(null, { status: 204 }) // logout
    })
    renderApp('/')

    expect(await screen.findByText('octocat')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('link', { name: 'Sign in' })).toBeInTheDocument()
    expect(fetchMock.mock.calls.some((c) => c[0] === '/api/auth/logout')).toBe(true)
  })

  it('sends a signed-in user away from the login page', async () => {
    fetchMock.mockImplementation(async () => json(me))
    renderApp('/login')

    expect(await screen.findByRole('heading', { name: 'Driftline' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Sign in with GitHub' })).not.toBeInTheDocument()
  })
})
