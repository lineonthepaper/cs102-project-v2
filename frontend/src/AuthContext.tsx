import { createContext, useContext, useEffect, useState } from 'react'

const API_BASE_URL = 'http://localhost:8080'

interface User {
  id: string
  authId: string
  email: string
  firstName: string
  lastName: string
  isStudent: boolean
  isInstructor: boolean
  isTA: boolean
  enabled: boolean
}

interface AuthContextType {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<{ error: string | null }>
  register: (
    email: string,
    password: string,
    firstName: string,
    lastName: string,
    role: 'student' | 'instructor'
  ) => Promise<{ error: string | null }>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      setUser(JSON.parse(storedUser))
    }
    setLoading(false)
  }, [])

  const login = async (email: string, password: string) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })

      if (!response.ok) {
        const errorData = await response.json()
        return { error: { message: errorData.error || 'Failed to login' } }
      }

      const userData = await response.json()
      localStorage.setItem('user', JSON.stringify(userData))
      setUser(userData)

      return { error: null }
    } catch (err: any) {
      return { error: err.message || 'Login failed' }
    }
  }

  const register = async (
    email: string,
    password: string,
    firstName: string,
    lastName: string,
    role: 'student' | 'instructor'
  ) => {
    try {
      const body = {
        email,
        password,
        firstName,
        lastName,
        isStudent: role === 'student',
        isInstructor: role === 'instructor',
      }

      const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      })

      if (!response.ok) {
        const errorData = await response.json()
        return { error: errorData.error || 'Registration failed' }
      }

      // Automatically log in after registering
      return await login(email, password)
    } catch (err: any) {
      return { error: err.message || 'Registration failed' }
    }
  }

  const logout = () => {
    localStorage.removeItem('user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within an AuthProvider')
  return context
}
