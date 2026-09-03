import {
  addDays,
  eachDateKey,
  normalizeTime,
  parseDateKey,
  toDateKey,
  toMinutes,
} from './dates'
import { isAbsenceLocation } from './locations'

function shiftHours(shift) {
  const start = toMinutes(normalizeTime(shift.startTime))
  const end = toMinutes(normalizeTime(shift.endTime))
  if (end <= start) return 0
  return (end - start) / 60
}

/**
 * Horas netas: sin vacaciones ni francos; descuenta almuerzo por día trabajado.
 * Si se pasan dateKeys, solo cuenta turnos de esas fechas.
 */
export function netHoursForUser(userId, shifts, lunchHours = 0, dateKeys = null) {
  const dates = dateKeys ? new Set(dateKeys) : null
  const work = (shifts || []).filter((s) => {
    if (s.userId !== userId || isAbsenceLocation(s.locationId)) return false
    if (!dates) return true
    return dates.has(String(s.workDate).slice(0, 10))
  })
  if (!work.length) return 0
  const gross = work.reduce((acc, s) => acc + shiftHours(s), 0)
  const days = new Set(work.map((s) => String(s.workDate).slice(0, 10))).size
  const lunch = Math.max(0, Number(lunchHours) || 0)
  return Math.max(0, Math.round((gross - lunch * days) * 100) / 100)
}

/** YYYY-MM-DD de lunes a domingo para una semana. */
export function weekDateKeys(weekStart) {
  if (!weekStart) return []
  const startKey =
    typeof weekStart === 'string' ? weekStart : toDateKey(weekStart)
  return eachDateKey(startKey, toDateKey(addDays(parseDateKey(startKey), 6)))
}

export function fmtHours(total) {
  return total % 1 === 0 ? String(total) : total.toFixed(1)
}
