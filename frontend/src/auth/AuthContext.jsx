import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import {
  getCurrentUser,
  login as requestLogin,
  logout as requestLogout,
} from '../api/authApi.js'

const AuthContext = createContext(null)
let initialUserRequest

function requestInitialUser() {
  if (!initialUserRequest) {
    initialUserRequest = getCurrentUser()
  }

  return initialUserRequest
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [authError, setAuthError] = useState(false)

  const refreshUser = useCallback(async () => {
    try {
      const currentUser = await getCurrentUser()
      setUser(currentUser)
      setAuthError(false)
      return currentUser
    } catch (error) {
      if (error.status === 401) {
        setUser(null)
        setAuthError(false)
        return null
      }

      setAuthError(true)
      throw error
    }
  }, [])

  useEffect(() => {
    let active = true

    requestInitialUser()
      .then((currentUser) => {
        if (active) {
          setUser(currentUser)
          setAuthError(false)
        }
      })
      .catch((error) => {
        if (!active) return

        if (error.status === 401) {
          setUser(null)
          setAuthError(false)
        } else {
          setAuthError(true)
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  const login = useCallback(async (credentials) => {
    await requestLogin(credentials)
    return refreshUser()
  }, [refreshUser])

  const logout = useCallback(async () => {
    try {
      await requestLogout()
    } catch (error) {
      if (error.status !== 401) throw error
    }

    setUser(null)
    setAuthError(false)
  }, [])

  const value = useMemo(() => ({
    user,
    loading,
    authError,
    login,
    logout,
    refreshUser,
  }), [user, loading, authError, login, logout, refreshUser])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }

  return context
}
