import { useMemo } from 'react'
import { fmtWeekRange } from '../lib/dates'
import { fmtHours, netHoursForPerson, weekDateKeys } from '../lib/hours'
import { initials, paletteFor } from '../lib/palette'

export default function SummaryBar({
  people,
  shifts,
  weekStart,
  lunchHours = 0,
}) {
  const dateKeys = useMemo(() => weekDateKeys(weekStart), [weekStart])
  const weekLabel = weekStart ? fmtWeekRange(weekStart) : ''
  const lunch = Math.max(0, Number(lunchHours) || 0)

  if (!people.length) {
    return (
      <div className="summary">
        <h3>Horas de la semana</h3>
        <div className="summary-row">
          <span style={{ fontSize: '12.5px', color: 'var(--ink-soft)' }}>
            Todavía no hay personas cargadas.
          </span>
        </div>
      </div>
    )
  }

  return (
    <div className="summary">
      <h3>Horas de la semana{weekLabel ? ` · ${weekLabel}` : ''}</h3>
      <div className="summary-sub">
        Sin vacaciones ni francos
        {lunch > 0
          ? ` · menos ${fmtHours(lunch)} h de almuerzo por día trabajado`
          : ''}
      </div>
      <div className="summary-row">
        {people.map((p) => {
          const total = netHoursForPerson(p.id, shifts, lunch, dateKeys)
          const av = paletteFor(p.id, people).c
          return (
            <div className="summary-item" key={p.id}>
              <span className="av" style={{ background: av }}>
                {initials(p.name)}
              </span>
              <span className="sname">{p.name}</span>
              <span className="shrs">{fmtHours(total)} h</span>
            </div>
          )
        })}
      </div>
    </div>
  )
}
