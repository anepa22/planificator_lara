export const DAYS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']
export const HOUR_START = 8
export const HOUR_END = 22
const SNAP_MIN = 15

export function mondayOf(date) {
  const d = new Date(date)
  const day = (d.getDay() + 6) % 7
  d.setDate(d.getDate() - day)
  d.setHours(0, 0, 0, 0)
  return d
}

export function weekStartForOffset(offset) {
  const base = mondayOf(new Date())
  base.setDate(base.getDate() + offset * 7)
  return base
}

/** YYYY-MM-DD en calendario local (no UTC) */
export function toDateKey(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function addDays(date, n) {
  const d = new Date(date)
  d.setDate(d.getDate() + n)
  return d
}

export function fmtWeekRange(start) {
  const end = addDays(start, 6)
  const o = { day: 'numeric', month: 'short' }
  return `${start.toLocaleDateString('es-AR', o)} – ${end.toLocaleDateString('es-AR', o)}`
}

export function isSameDate(a, b) {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  )
}

export function toMinutes(t) {
  const [h, m] = String(t).slice(0, 5).split(':').map(Number)
  return h * 60 + m
}

export function fromMinutes(m) {
  const h = Math.floor(m / 60)
  const mm = m % 60
  return `${String(h).padStart(2, '0')}:${String(mm).padStart(2, '0')}`
}

export function snapMinutes(m) {
  return Math.round(m / SNAP_MIN) * SNAP_MIN
}

export function normalizeTime(t) {
  if (!t) return '09:00'
  return String(t).slice(0, 5)
}

/** Solapamiento half-open [start, end) como en Postgres tsrange '[)'. */
export function timesOverlap(aStart, aEnd, bStart, bEnd) {
  const a0 = toMinutes(normalizeTime(aStart))
  const a1 = toMinutes(normalizeTime(aEnd))
  const b0 = toMinutes(normalizeTime(bStart))
  const b1 = toMinutes(normalizeTime(bEnd))
  return a0 < b1 && b0 < a1
}

export function yToStartTime(y, hourH) {
  const raw = HOUR_START * 60 + (y / hourH) * 60
  const snapped = Math.max(
    HOUR_START * 60,
    Math.min(HOUR_END * 60 - SNAP_MIN, snapMinutes(raw)),
  )
  return fromMinutes(snapped)
}

export function workDateFor(weekStart, dayIndex) {
  return toDateKey(addDays(weekStart, dayIndex))
}

/** Primer día del mes (día 1, 00:00 local). `monthOffset` relativo al mes actual. */
export function monthStartForOffset(monthOffset = 0) {
  const now = new Date()
  const d = new Date(now.getFullYear(), now.getMonth() + monthOffset, 1)
  d.setHours(0, 0, 0, 0)
  return d
}

export function daysInMonth(date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate()
}

export function fmtMonthLabel(date) {
  const label = date.toLocaleDateString('es-AR', { month: 'long', year: 'numeric' })
  return label.charAt(0).toUpperCase() + label.slice(1)
}

/** Lunes (YYYY-MM-DD) de todas las semanas que tocan el mes. */
export function weekStartsOverlappingMonth(monthDate) {
  const year = monthDate.getFullYear()
  const month = monthDate.getMonth()
  const first = new Date(year, month, 1)
  const last = new Date(year, month + 1, 0)
  return weekStartsOverlappingRange(toDateKey(first), toDateKey(last))
}

/** Lunes (YYYY-MM-DD) de todas las semanas que tocan [fromKey, toKey]. */
export function weekStartsOverlappingRange(fromKey, toKey) {
  if (!fromKey || !toKey) return []
  let start = parseDateKey(fromKey)
  let end = parseDateKey(toKey)
  if (start > end) {
    const tmp = start
    start = end
    end = tmp
  }
  let cur = mondayOf(start)
  const keys = []
  while (cur <= end) {
    keys.push(toDateKey(cur))
    cur = addDays(cur, 7)
  }
  return keys
}

/** Offset de semana (relativo a la semana actual) para una fecha. */
export function weekOffsetForDate(date) {
  const target = mondayOf(date)
  const current = mondayOf(new Date())
  return Math.round((target.getTime() - current.getTime()) / (7 * 24 * 60 * 60 * 1000))
}

export function parseDateKey(key) {
  const [y, m, d] = String(key).split('-').map(Number)
  const date = new Date(y, m - 1, d)
  date.setHours(0, 0, 0, 0)
  return date
}

/** Lista inclusiva de YYYY-MM-DD entre fromKey y toKey. */
export function eachDateKey(fromKey, toKey) {
  let d = parseDateKey(fromKey)
  const end = parseDateKey(toKey)
  if (d > end) return []
  const keys = []
  while (d <= end) {
    keys.push(toDateKey(d))
    d = addDays(d, 1)
  }
  return keys
}

function isSunday(dateKey) {
  return parseDateKey(dateKey).getDay() === 0
}

/** 0=Lun … 6=Dom (mismo índice que DAYS). */
function weekdayIndex(dateKey) {
  return (parseDateKey(dateKey).getDay() + 6) % 7
}

/** Fechas del rango; por defecto omite domingos. */
function eachWorkDateKey(fromKey, toKey, { includeSundays = false } = {}) {
  const keys = eachDateKey(fromKey, toKey)
  if (includeSundays) return keys
  return keys.filter((k) => !isSunday(k))
}

/** Fechas del rango cuyo día de semana está en weekdayIndexes (0=Lun…6=Dom). */
function eachWeekdayDateKey(fromKey, toKey, weekdayIndexes) {
  const set = new Set(weekdayIndexes || [])
  if (!set.size) return []
  return eachDateKey(fromKey, toKey).filter((k) => set.has(weekdayIndex(k)))
}

/** Parte un rango en días de turno vs días de franco (por día de semana). */
export function splitRangeAssignDates(
  fromKey,
  toKey,
  { includeSundays = false, francoWeekdays = [] } = {},
) {
  const francoSet = new Set(francoWeekdays)
  const francoDates = eachWeekdayDateKey(fromKey, toKey, francoWeekdays)
  const workDates = eachWorkDateKey(fromKey, toKey, { includeSundays }).filter(
    (k) => !francoSet.has(weekdayIndex(k)),
  )
  return { workDates, francoDates }
}

/** Horario de ausencia a día completo (vacaciones / franco). */
export function absenceDayBounds() {
  return {
    startTime: fromMinutes(HOUR_START * 60),
    endTime: fromMinutes(HOUR_END * 60),
  }
}

export function monthBounds(monthDate) {
  const y = monthDate.getFullYear()
  const m = monthDate.getMonth()
  return {
    min: toDateKey(new Date(y, m, 1)),
    max: toDateKey(new Date(y, m + 1, 0)),
  }
}
