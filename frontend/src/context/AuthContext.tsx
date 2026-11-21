import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { authApi } from '../api/endpoints'
import { getAccessToken, setTokens } from '../api/client'
import type { Role, UserSummary } from '../lib/types'

interface AuthContextValue {
  user: UserSummary | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (data: { email: string; password: string; firstName: string; lastName: string; role: Role }) => Promise<void>
  logout: () => Promise<void>
  refreshCurrentUser: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    async function bootstrap() {
      if (getAccessToken()) {
        try {
          const me = await authApi.me()
          if (!cancelled) setUser(me)
        } catch {
          setTokens(null)
        }
      }
      if (!cancelled) setLoading(false)
    }
    bootstrap()
    return () => {
      cancelled = true
    }
  }, [])

  async function login(email: string, password: string) {
    const res = await authApi.login({ email, password })
    setUser(res.user)
  }

  async function register(data: { email: string; password: string; firstName: string; lastName: string; role: Role }) {
    const res = await authApi.register(data)
    setUser(res.user)
  }

  async function logout() {
    await authApi.logout()
    setUser(null)
  }

  async function refreshCurrentUser() {
    const me = await authApi.me()
    setUser(me)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refreshCurrentUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
