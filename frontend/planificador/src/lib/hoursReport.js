import { isAbsenceLocation } from './locations.js'

/** Primer y último día del mes calendario en America/Argentina/Buenos_Aires. */
export function currentMonthRange(now = new Date()) {
  const today = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Argentina/Buenos_Aires',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(now)
  const [year, month] = today.split('-')
  const last = new Date(Date.UTC(Number(year), Number(month), 0)).getUTCDate()
  return {
    from: `${year}-${month}-01`,
    to: `${year}-${month}-${String(last).padStart(2, '0')}`,
  }
}

export function reportMatchesSearch(report, search) {
  if (!report || !search) return false
  return (
    report.from === search.from &&
    report.to === search.to &&
    (report.personId || '') === (search.personId || '') &&
    (report.locationId || '') === (search.locationId || '') &&
    Number(report.lunchHours) === Number(search.lunchHours)
  )
}

export function salesLocationOptions(locations) {
  return (locations || []).filter((location) => location?.id && !isAbsenceLocation(location.id))
}

export function fmtReportHours(total) {
  const n = Math.round(Number(total) * 100) / 100
  if (!Number.isFinite(n)) return '0'
  if (Number.isInteger(n)) return String(n)
  return n.toFixed(2).replace(/0+$/, '').replace('.', ',')
}
