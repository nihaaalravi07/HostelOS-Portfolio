import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, tokenStore } from './lib/api'
import type { User } from './lib/types'

interface AuthState {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<User>
  logout: () => void
}
const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const qc = useQueryClient()
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(() => tokenStore.get() !== null)

  // Restore the session from the stored token (the backend re-validates it).
  useEffect(() => {
    if (!tokenStore.get()) return
    api<User>('/api/auth/me').then(setUser).catch(() => tokenStore.clear()).finally(() => setLoading(false))
  }, [])

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
    qc.clear()
  }, [qc])

  useEffect(() => {
    window.addEventListener('hostelos:unauthorized', logout)
    return () => window.removeEventListener('hostelos:unauthorized', logout)
  }, [logout])

  const login = useCallback(async (email: string, password: string) => {
    const res = await api<{ token: string; user: User }>('/api/auth/login', { body: { email, password } })
    tokenStore.set(res.token)
    qc.clear()
    setUser(res.user)
    return res.user
  }, [qc])

  const value = useMemo(() => ({ user, loading, login, logout }), [user, loading, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

export const homeFor = (role: User['role']) => ({
  STUDENT: '/', ADMIN: '/admin', WARDEN: '/warden', MESS_STAFF: '/mess',
  HOUSEKEEPING_STAFF: '/housekeeping', MAINTENANCE_STAFF: '/maintenance',
} as const)[role]
