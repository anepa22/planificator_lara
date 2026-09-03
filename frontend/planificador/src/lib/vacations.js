import { addDays, normalizeTime, parseDateKey, toDateKey } from './dates'

/** Agrupa turnos de un local de ausencia en tramos contiguos por usuario. */
function groupAbsenceRanges(shifts, locationId) {
  const byUser = new Map()
  for (const s of shifts || []) {
    if (s.locationId !== locationId) continue
    const uid = s.userId
    if (!byUser.has(uid)) byUser.set(uid, [])
    byUser.get(uid).push(s)
  }

  const ranges = []
  for (const [userId, list] of byUser) {
    const sorted = list
      .slice()
      .sort((a, b) =>
        String(a.workDate).localeCompare(String(b.workDate)),
      )
    let run = null
    for (const s of sorted) {
      const day = String(s.workDate).slice(0, 10)
      if (!run) {
        run = { userId, dateFrom: day, dateTo: day, shiftIds: [s.id] }
        continue
      }
      const expected = toDateKey(addDays(parseDateKey(run.dateTo), 1))
      if (day === expected) {
        run.dateTo = day
        run.shiftIds.push(s.id)
      } else {
        ranges.push(run)
        run = { userId, dateFrom: day, dateTo: day, shiftIds: [s.id] }
      }
    }
    if (run) ranges.push(run)
  }

  ranges.sort((a, b) => {
    const byFrom = a.dateFrom.localeCompare(b.dateFrom)
    if (byFrom) return byFrom
    return a.userId.localeCompare(b.userId)
  })
  return ranges
}

/** Rango contiguo de ausencia que incluye dateKey para un usuario. */
export function findContiguousAbsenceRange(shifts, userId, dateKey, locationId) {
  const key = String(dateKey).slice(0, 10)
  return (
    groupAbsenceRanges(shifts, locationId).find(
      (r) =>
        r.userId === userId &&
        r.dateFrom <= key &&
        key <= r.dateTo,
    ) || null
  )
}

/**
 * Tramo contiguo de turnos de trabajo: mismo usuario, local y horario
 * en días calendario seguidos.
 */
export function findContiguousWorkShiftRange(
  shifts,
  userId,
  dateKey,
  locationId,
  startTime,
  endTime,
) {
  const key = String(dateKey).slice(0, 10)
  const start = normalizeTime(startTime)
  const end = normalizeTime(endTime)
  const list = (shifts || [])
    .filter(
      (s) =>
        s.userId === userId &&
        s.locationId === locationId &&
        normalizeTime(s.startTime) === start &&
        normalizeTime(s.endTime) === end,
    )
    .slice()
    .sort((a, b) => String(a.workDate).localeCompare(String(b.workDate)))

  let run = null
  const ranges = []
  for (const s of list) {
    const day = String(s.workDate).slice(0, 10)
    if (!run) {
      run = { userId, dateFrom: day, dateTo: day, shiftIds: [s.id] }
      continue
    }
    const expected = toDateKey(addDays(parseDateKey(run.dateTo), 1))
    if (day === expected) {
      run.dateTo = day
      run.shiftIds.push(s.id)
    } else {
      ranges.push(run)
      run = { userId, dateFrom: day, dateTo: day, shiftIds: [s.id] }
    }
  }
  if (run) ranges.push(run)

  return (
    ranges.find((r) => r.dateFrom <= key && key <= r.dateTo) || null
  )
}

export function fmtVacationDay(dateKey) {
  return parseDateKey(dateKey).toLocaleDateString('es-AR', {
    day: 'numeric',
    month: 'short',
  })
}

export function fmtVacationRangeLabel(from, to) {
  if (!from || !to) return ''
  if (from === to) return fmtVacationDay(from)
  return `${fmtVacationDay(from)} – ${fmtVacationDay(to)}`
}
