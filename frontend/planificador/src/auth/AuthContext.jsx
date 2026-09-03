import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import { getMe, login as apiLogin, logout as apiLogout } from '../api/client'
import {
  clearToken,
  getToken,
  IDLE_TIMEOUT_MS,
  isIdleExpired,
  setLastActivity,
  setToken,
} from '../lib/authStorage'

const AuthContext = createContext(null)

const ACTIVITY_EVENTS = [
  'mousedown',
  'mousemove',
  'keydown',
  'scroll',
  'touchstart',
  'click',
  'wheel',
]

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [booting, setBooting] = useState(true)
  const idleTimerRef = useRef(null)

  const applySession = useCallback((payload, token) => {
    if (token) setToken(token)
    setLastActivity()
    setUser({
      id: payload.id,
      username: payload.username,
      displayName: payload.displayName,
      personId: payload.personId || null,
      roles: payload.roles || [],
      permissions: new Set(payload.permissions || []),
    })
  }, [])

  const logout = useCallback(async () => {
    if (idleTimerRef.current != null) {
      clearTimeout(idleTimerRef.current)
      idleTimerRef.current = null
    }
    try {
      await apiLogout()
    } catch {
      /* igual cerramos sesión local */
    }
    clearToken()
    setUser(null)
  }, [])

  const refreshMe = useCallback(async () => {
    const me = await getMe()
    applySession(me, getToken())
    return me
  }, [applySession])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const token = getToken()
      if (!token) {
        if (!cancelled) setBooting(false)
        return
      }
      if (isIdleExpired()) {
        clearToken()
        if (!cancelled) {
          setUser(null)
          setBooting(false)
        }
        return
      }
      try {
        await refreshMe()
      } catch {
        clearToken()
        if (!cancelled) setUser(null)
      } finally {
        if (!cancelled) setBooting(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [refreshMe])

  useEffect(() => {
    function onUnauthorized() {
      clearToken()
      setUser(null)
    }
    window.addEventListener('planificator:unauthorized', onUnauthorized)
    return () =>
      window.removeEventListener('planificator:unauthorized', onUnauthorized)
  }, [])

  useEffect(() => {
    if (!user) return

    const armTimer = () => {
      if (idleTimerRef.current != null) clearTimeout(idleTimerRef.current)
      idleTimerRef.current = setTimeout(() => {
        void logout()
      }, IDLE_TIMEOUT_MS)
    }

    let lastBump = 0
    const bump = () => {
      const now = Date.now()
      armTimer()
      if (now - lastBump < 1000) return
      lastBump = now
      setLastActivity(now)
    }

    if (isIdleExpired()) {
      void logout()
      return
    }
    bump()

    for (const ev of ACTIVITY_EVENTS) {
      window.addEventListener(ev, bump, { passive: true })
    }

    const onVisibility = () => {
      if (document.visibilityState !== 'visible') return
      if (isIdleExpired()) void logout()
      else bump()
    }
    document.addEventListener('visibilitychange', onVisibility)

    return () => {
      if (idleTimerRef.current != null) {
        clearTimeout(idleTimerRef.current)
        idleTimerRef.current = null
      }
      for (const ev of ACTIVITY_EVENTS) {
        window.removeEventListener(ev, bump)
      }
      document.removeEventListener('visibilitychange', onVisibility)
    }
  }, [user, logout])

  const login = useCallback(
    async (username, password) => {
      const res = await apiLogin(username, password)
      applySession(res, res.token)
      return res
    },
    [applySession],
  )

  const can = useCallback(
    (permission) => !!user?.permissions?.has(permission),
    [user],
  )

  const value = useMemo(
    () => ({
      user,
      booting,
      login,
      logout,
      can,
    }),
    [user, booting, login, logout, can],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
