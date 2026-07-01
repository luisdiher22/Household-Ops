import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import axios from 'axios'
import type { AuthResponse } from '../types'
import { clearAuth, loadAuth, saveAuth } from '../api/authStorage'

interface AuthContextValue {
  auth: AuthResponse | null
  login: (email: string, password: string) => Promise<void>
  logout: () => void
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
