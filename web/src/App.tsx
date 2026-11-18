import { Link, Route, Routes } from 'react-router'
import { useCurrentUser, useLogout } from './auth/useAuth.ts'
import HomePage from './pages/HomePage.tsx'
import LoginPage from './pages/LoginPage.tsx'

function UserMenu() {
  const { data: user, isPending } = useCurrentUser()
  const logout = useLogout()

  if (isPending) return null
  if (!user) {
    return (
      <Link to="/login" className="text-sm text-slate-600 hover:text-slate-900">
        Sign in
      </Link>
    )
  }
  return (
    <div className="flex items-center gap-3 text-sm">
      <span className="text-slate-700">{user.githubLogin}</span>
      <button
        type="button"
        onClick={() => logout.mutate()}
        disabled={logout.isPending}
        className="text-slate-600 hover:text-slate-900 disabled:opacity-50"
      >
        Sign out
      </button>
    </div>
  )
}

export default function App() {
  return (
    <div className="min-h-screen bg-white text-slate-900">
      <header className="border-b border-slate-200">
        <nav className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <Link to="/" className="font-semibold">
            Driftline
          </Link>
          <UserMenu />
        </nav>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
        </Routes>
      </main>
    </div>
  )
}
