import { addDays, normalizeTime, parseDateKey, toDateKey } from './dates'

/** Agrupa turnos de un local de ausencia en tramos contiguos por persona. */
function groupAbsenceRanges(shifts, locationId) {
  const byPerson = new Map()
  for (const s of shifts || []) {
    if (s.locationId !== locationId) continue
    const pid = s.personId
    if (!byPerson.has(pid)) byPerson.set(pid, [])
    byPerson.get(pid).push(s)
  }

  const ranges = []
  for (const [personId, list] of byPerson) {
    const sorted = list
      .slice()
      .sort((a, b) =>
        String(a.workDate).localeCompare(String(b.workDate)),
      )
    let run = null
    for (const s of sorted) {
      const day = String(s.workDate).slice(0, 10)
      if (!run) {
        run = { personId, dateFrom: day, dateTo: day, shiftIds: [s.id] }
        continue
      }
      const expected = toDateKey(addDays(parseDateKey(run.dateTo), 1))
      if (day === expected) {
        run.dateTo = day
        run.shiftIds.push(s.id)
      } else {
        ranges.push(run)
        run = { personId, dateFrom: day, dateTo: day, shiftIds: [s.id] }
      }
    }
    if (run) ranges.push(run)
  }

  ranges.sort((a, b) => {
    const byFrom = a.dateFrom.localeCompare(b.dateFrom)
    if (byFrom) return byFrom
    return a.personId.localeCompare(b.personId)
  })
  return ranges
}

/** Rango contiguo de ausencia que incluye dateKey para una persona. */
export function findContiguousAbsenceRange(shifts, personId, dateKey, locationId) {
  const key = String(dateKey).slice(0, 10)
  return (
    groupAbsenceRanges(shifts, locationId).find(
      (r) =>
        r.personId === personId &&
        r.dateFrom <= key &&
        key <= r.dateTo,
    ) || null
  )
}

/**
 * Tramo contiguo de turnos de trabajo: misma persona, local y horario
 * en días calendario seguidos.
 */
export function findContiguousWorkShiftRange(
  shifts,
  personId,
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
        s.personId === personId &&
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
      run = { personId, dateFrom: day, dateTo: day, shiftIds: [s.id] }
      continue
    }
    const expected = toDateKey(addDays(parseDateKey(run.dateTo), 1))
    if (day === expected) {
      run.dateTo = day
      run.shiftIds.push(s.id)
    } else {
      ranges.push(run)
      run = { personId, dateFrom: day, dateTo: day, shiftIds: [s.id] }
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
