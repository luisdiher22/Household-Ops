import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const DEMO_ACCOUNTS = [
  { label: 'Owner (Evelyn Cross)', email: 'owner@householdops.dev' },
  { label: 'House Manager (Marcus Bell)', email: 'manager@householdops.dev' },
  { label: 'Staff (Priya Nair)', email: 'staff@householdops.dev' },
  { label: 'Vendor (Dana Ruiz)', email: 'vendor@householdops.dev' },
]
const DEMO_PASSWORD = 'password123'

export function LoginPage() {
  const { auth, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (auth) return <Navigate to="/" replace />

  async function doLogin(loginEmail: string, loginPassword: string) {
    setError(null)
    setSubmitting(true)
    try {
      await login(loginEmail, loginPassword)
      navigate('/')
    } catch {
      setError('Login failed -- check the email and password.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50">
      <div className="w-full max-w-sm space-y-6 rounded-lg border border-slate-200 bg-white p-8 shadow-sm">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Household Ops</h1>
          <p className="text-sm text-slate-500">Sign in to manage your household</p>
        </div>

        <form
          className="space-y-3"
          onSubmit={(e) => {
            e.preventDefault()
            void doLogin(email, password)
          }}
        >
          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
            required
          />
          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
            required
          />
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
          >
            Sign in
          </button>
        </form>

        <div>
          <p className="mb-2 text-xs font-medium uppercase tracking-wide text-slate-400">Demo accounts</p>
          <div className="grid gap-2">
            {DEMO_ACCOUNTS.map((account) => (
              <button
                key={account.email}
                type="button"
                disabled={submitting}
                onClick={() => void doLogin(account.email, DEMO_PASSWORD)}
                className="rounded border border-slate-200 px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-50 disabled:opacity-50"
              >
                {account.label}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
