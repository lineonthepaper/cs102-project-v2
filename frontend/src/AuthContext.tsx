import { createContext, useContext, useState, useEffect } from 'react'
import type { ReactNode } from 'react'
import { supabase } from './supabase'
import type { User, AuthError } from '@supabase/supabase-js'

const API_BASE_URL = 'http://localhost:8080'

interface AuthContextType {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<{ error: AuthError | null }>
  register: (email: string, password: string, firstName: string, lastName: string) => Promise<{ error: AuthError | null }>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const getSession = async () => {
      const { data: { session } } = await supabase.auth.getSession()
      setUser(session?.user || null)
      setLoading(false)
    }

    getSession()

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      setUser(session?.user || null)
      setLoading(false)
    })

    return () => subscription.unsubscribe()
  }, [])

  const login = async (email: string, password: string) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password })
      })

      if (!response.ok) {
        const errorData = await response.json()
        return { error: { message: errorData.error || 'Failed to login' } as AuthError }
      }

      const data = await response.json()
      
      // Store tokens
      localStorage.setItem('access_token', data.accessToken)
      localStorage.setItem('refresh_token', data.refreshToken)
      
      // Create user object compatible with Supabase User type
      const user = {
        id: data.user.authId,
        email: data.user.email,
        user_metadata: {
          first_name: data.user.firstName,
          last_name: data.user.lastName
        }
      } as User

      setUser(user)
      return { error: null }
    } catch (error: any) {
      return { error: { message: error.message || 'Failed to login' } as AuthError }
    }
  }

  const register = async (email: string, password: string, firstName: string, lastName: string) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password, firstName, lastName })
      })

      if (!response.ok) {
        const errorData = await response.json()
        return { error: { message: errorData.error || 'Failed to register' } as AuthError }
      }

      // After successful registration, automatically log in
      return await login(email, password)
    } catch (error: any) {
      return { error: { message: error.message || 'Failed to register' } as AuthError }
    }
  }

  const logout = async () => {
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    setUser(null)
  }

  const value: AuthContextType = { user, loading, login, register, logout }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

