import { useMemo } from 'react'
import { fmtMonthLabel, parseDateKey } from '../lib/dates'

function dateLabel(dateKey) {
  const d = parseDateKey(dateKey)
  const raw = d.toLocaleDateString('es-AR', {
    weekday: 'short',
    day: 'numeric',
  })
  return raw.replace('.', '').replace(/^./, (c) => c.toUpperCase())
}

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
      const key = String(row.workDate).slice(0, 10)
      const d = parseDateKey(key)
      if (d.getFullYear() !== y || d.getMonth() !== m) continue
      if (!byDate.has(key)) byDate.set(key, [])
      const loc = locById.get(row.locationId)
      byDate.get(key).push({
        id: row.locationId,
        name: row.locationName || loc?.name || row.locationId,
        color: loc?.color || '#E3B505',
      })
    }
    return [...byDate.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([dateKey, locs]) => ({
        dateKey,
        label: dateLabel(dateKey),
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
