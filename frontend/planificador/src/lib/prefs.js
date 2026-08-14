const LUNCH_KEY = 'planificador.lunchHours'
const DEFAULT_LUNCH_HOURS = 1

export function loadLunchHours() {
  try {
    const raw = localStorage.getItem(LUNCH_KEY)
    if (raw == null || raw === '') return DEFAULT_LUNCH_HOURS
    const n = Number(raw)
    if (!Number.isFinite(n) || n < 0) return DEFAULT_LUNCH_HOURS
    return Math.min(4, Math.round(n * 100) / 100)
  } catch {
    return DEFAULT_LUNCH_HOURS
  }
}

export function saveLunchHours(hours) {
  const n = Number(hours)
  const value =
    !Number.isFinite(n) || n < 0 ? 0 : Math.min(4, Math.round(n * 100) / 100)
  try {
    localStorage.setItem(LUNCH_KEY, String(value))
  } catch {
    /* ignore */
  }
  return value
}
