const TOKEN_KEY = 'planificator.token'
const LAST_ACTIVITY_KEY = 'planificator.lastActivity'

/** Minutos sin interacción antes de cerrar sesión. */
export const IDLE_TIMEOUT_MS = 5 * 60 * 1000

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

export function getLastActivity() {
  const raw = localStorage.getItem(LAST_ACTIVITY_KEY)
  if (!raw) return null
  const n = Number(raw)
  return Number.isFinite(n) ? n : null
}

export function setLastActivity(ts = Date.now()) {
  localStorage.setItem(LAST_ACTIVITY_KEY, String(ts))
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(LAST_ACTIVITY_KEY)
}

export function isIdleExpired() {
  const last = getLastActivity()
  if (last == null) return false
  return Date.now() - last > IDLE_TIMEOUT_MS
}
