import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import axios from 'axios'
import type { AuthResponse } from '../types'
import { clearAuth, loadAuth, saveAuth } from '../api/authStorage'

interface AuthContextValue {
  auth: AuthResponse | null
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  // Lets a mutation hook (e.g. switching to a different property) update the
  // shared session in one place, same as login does -- every query hook that
  // keys off auth.householdId picks up the change automatically.
  setAuthResponse: (response: AuthResponse) => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthResponse | null>(() => loadAuth())

  const value = useMemo<AuthContextValue>(
    () => ({
      auth,
      login: async (email: string, password: string) => {
        const response = await axios.post<AuthResponse>('/api/auth/login', { email, password })
        saveAuth(response.data)
        setAuth(response.data)
      },
      logout: () => {
        clearAuth()
        setAuth(null)
      },
      setAuthResponse: (response: AuthResponse) => {
        saveAuth(response)
        setAuth(response)
      },
    }),
    [auth],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within an AuthProvider')
  return context
}
