import { DAYS, addDays, isSameDate } from '../lib/dates'

export default function DayTabs({ weekStart, selectedDay, onSelect }) {
  const today = new Date()

  return (
    <div className="day-tabs">
      {DAYS.map((name, i) => {
        const d = addDays(weekStart, i)
        const isToday = isSameDate(d, today)
        const selected = i === selectedDay
        return (
          <div
            key={i}
            className={`day-tab${isToday ? ' is-today' : ''}${selected ? ' selected' : ''}`}
            onClick={() => onSelect(i)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') onSelect(i)
            }}
          >
            <div className="dn">{name}</div>
            <div className="dd">{d.getDate()}</div>
          </div>
        )
      })}
    </div>
  )
}
