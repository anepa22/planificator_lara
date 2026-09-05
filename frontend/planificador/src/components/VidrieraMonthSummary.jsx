import { useMemo } from 'react'
import { fmtMonthLabel, fmtVidrieraDay, parseDateKey, workDateKey } from '../lib/dates'

export default function VidrieraMonthSummary({
  monthDate,
  vidrieras = [],
  locations = [],
}) {
  const locById = useMemo(() => {
    const map = new Map()
    for (const loc of locations) map.set(loc.id, loc)
    return map
  }, [locations])

  const days = useMemo(() => {
    const y = monthDate.getFullYear()
    const m = monthDate.getMonth()
    const byDate = new Map()
    for (const row of vidrieras) {
      const key = workDateKey(row.workDate)
      if (!key) continue
      const d = parseDateKey(key)
      if (d.getFullYear() !== y || d.getMonth() !== m) continue
      const locs = byDate.get(key) || []
      if (!locs.some((loc) => loc.id === row.locationId)) {
        const loc = locById.get(row.locationId)
        locs.push({
          id: row.locationId,
          name: row.locationName || loc?.name || row.locationId,
          color: loc?.color || '#E3B505',
        })
        byDate.set(key, locs)
      }
    }
    return [...byDate.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([dateKey, locs]) => ({
        dateKey,
        label: fmtVidrieraDay(dateKey),
        locations: locs.sort((a, b) => a.name.localeCompare(b.name, 'es')),
      }))
  }, [vidrieras, monthDate, locById])

  const monthLabel = fmtMonthLabel(monthDate)

  return (
    <div className="summary vidriera-month-summary">
      <h3>Vidrieras · {monthLabel}</h3>
      {days.length === 0 ? (
        <div className="summary-sub">No hay vidrieras este mes.</div>
      ) : (
        <>
          <div className="summary-sub">
            {days.length} día{days.length === 1 ? '' : 's'} con vidriera
          </div>
          <div className="vidriera-month-days">
            {days.map((day) => (
              <div className="vidriera-month-day" key={day.dateKey}>
                <span className="vidriera-month-date">{day.label}</span>
                <span className="vidriera-month-locs">
                  {day.locations.map((loc) => (
                    <span className="vidriera-month-chip" key={loc.id}>
                      <span className="dot" style={{ background: loc.color }} />
                      {loc.name}
                    </span>
                  ))}
                </span>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}
