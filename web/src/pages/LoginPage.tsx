import { Navigate, useSearchParams } from 'react-router'
import { useCurrentUser } from '../auth/useAuth.ts'

// Starts the GitHub OAuth login (decisions/0006, 0011). It must be a normal link, not a router link:
// the browser leaves the app, goes to GitHub, and comes back to the API, which sets the cookies.
const GITHUB_LOGIN_URL = '/api/auth/oauth2/authorization/github'

export default function LoginPage() {
  const { data: user, isPending } = useCurrentUser()
  const [params] = useSearchParams()

  if (!isPending && user) return <Navigate to="/" replace />

  return (
    <section>
      <h1 className="text-2xl font-semibold">Sign in</h1>
      {params.get('error') && (
        <p
          role="alert"
          className="mt-3 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
        >
          GitHub sign-in did not complete. Try again.
        </p>
      )}
      <a
        href={GITHUB_LOGIN_URL}
        className="mt-4 inline-block rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
      >
        Sign in with GitHub
      </a>
    </section>
  )
}
