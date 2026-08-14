import { useLayoutEffect, useMemo, useRef } from 'react'
import {
  DAYS,
  addDays,
  daysInMonth,
  mondayOf,
  normalizeTime,
  toDateKey,
} from '../lib/dates'
import { fmtHours, netHoursForPerson } from '../lib/hours'
import { isAbsenceLocation, absenceLabel, isFrancoLocation } from '../lib/locations'
import { initials, paletteFor } from '../lib/palette'

const DAY_COL_PX = 80
const PERSON_COL_PX = 140

export default function MonthGantt({
  monthDate,
  people,
  shifts,
  lunchHours = 0,
  focusNonce = 0,
  focusDateKey = null,
  canAdd = true,
  canEdit = true,
  canEditVacations = true,
  onEdit,
  onAdd,
  onRangeAssign,
  onAbsence,
}) {
  const year = monthDate.getFullYear()
  const month = monthDate.getMonth()
  const nDays = daysInMonth(monthDate)
  const todayKey = toDateKey(new Date())
  const scrollRef = useRef(null)

  const currentWeekKeys = useMemo(() => {
    void focusNonce
    const start = mondayOf(new Date())
    const keys = new Set()
    for (let i = 0; i < 7; i++) keys.add(toDateKey(addDays(start, i)))
    return keys
  }, [focusNonce])

  const focusDay = useMemo(() => {
    void focusNonce
    if (focusDateKey) {
      const d = focusDateKey.slice(0, 10)
      const [y, m, day] = d.split('-').map(Number)
      if (y === year && m === month + 1 && day >= 1 && day <= nDays) {
        return day
      }
    }
    const start = mondayOf(new Date())
    for (let i = 0; i < 7; i++) {
      const d = addDays(start, i)
      if (d.getFullYear() === year && d.getMonth() === month) return d.getDate()
    }
    return 1
  }, [focusNonce, focusDateKey, year, month, nDays])

  const dayHeaders = useMemo(() => {
    const list = []
    for (let day = 1; day <= nDays; day++) {
      const d = new Date(year, month, day)
      const key = toDateKey(d)
      list.push({
        day,
        key,
        dow: DAYS[(d.getDay() + 6) % 7],
        weekend: d.getDay() === 0 || d.getDay() === 6,
        currentWeek: currentWeekKeys.has(key),
      })
    }
    return list
  }, [year, month, nDays, currentWeekKeys])

  const byPersonDay = useMemo(() => {
    const map = new Map()
    for (const s of shifts) {
      const key = `${s.personId}|${s.workDate}`
      if (!map.has(key)) map.set(key, [])
      map.get(key).push(s)
    }
    for (const list of map.values()) {
      list.sort(
        (a, b) =>
          normalizeTime(a.startTime).localeCompare(normalizeTime(b.startTime)),
      )
    }
    return map
  }, [shifts])

  useLayoutEffect(() => {
    const scroller = scrollRef.current
    if (!scroller) return
    const raw = getComputedStyle(scroller).getPropertyValue('--gantt-day-w')
    const dayW = Number.parseFloat(raw) || DAY_COL_PX
    scroller.scrollLeft = Math.max(0, (focusDay - 1) * dayW)
  }, [focusDay, year, month, nDays, focusNonce])

  const rows = people.length
    ? people
    : [{ id: '__empty', name: 'Sin personas', _empty: true }]

  const gridStyle = {
    '--gantt-days': nDays,
    '--gantt-day-w': `${DAY_COL_PX}px`,
    '--gantt-person-w': `${PERSON_COL_PX}px`,
  }

  function openAddFor(personId, header) {
    onAdd?.({
      personId,
      workDate: header.key,
    })
  }

  return (
    <div className={`gantt-wrap${canAdd ? '' : ' gantt-readonly'}`}>
      <div className="gantt-scroll" ref={scrollRef} style={gridStyle}>
        <div className="gantt-head">
          <div className="gantt-corner">
            <span className="gantt-corner-title">Persona</span>
            {canAdd && onRangeAssign ? (
              <span className="gantt-corner-hint">Nombre → rango</span>
            ) : null}
          </div>
          {dayHeaders.map((h) => (
            <div
              key={h.key}
              className={`gantt-day-head${h.weekend ? ' is-weekend' : ''}${
                h.key === todayKey ? ' is-today' : ''
              }${h.currentWeek ? ' is-current-week' : ''}`}
            >
              <span className="gantt-dow">{h.dow}</span>
              <span className="gantt-dom">{h.day}</span>
            </div>
          ))}
        </div>

        {rows.map((person) => {
          if (person._empty) {
            return (
              <div className="gantt-row" key={person.id}>
                <div className="gantt-person gantt-person-empty">{person.name}</div>
                {dayHeaders.map((h) => (
                  <div
                    key={h.key}
                    className={`gantt-cell${h.currentWeek ? ' is-current-week' : ''}`}
                  />
                ))}
              </div>
            )
          }

          const av = paletteFor(person.id, people)
          const monthHours = netHoursForPerson(person.id, shifts, lunchHours)
          return (
            <div className="gantt-row" key={person.id}>
              <div className="gantt-person">
                <span className="gantt-av" style={{ background: av.c }}>
                  {initials(person.name)}
                </span>
                <div className="gantt-person-text">
                  {canAdd && onRangeAssign ? (
                    <button
                      type="button"
                      className="gantt-pname gantt-pname-btn"
                      title={`Asignar rango a ${person.name}`}
                      onClick={() => onRangeAssign(person)}
                    >
                      {person.name}
                    </button>
                  ) : (
                    <span className="gantt-pname">{person.name}</span>
                  )}
                  <span
                    className="gantt-phours"
                    title="Horas del mes (sin vacaciones ni francos)"
                  >
                    {fmtHours(monthHours)} h
                  </span>
                </div>
              </div>
              {dayHeaders.map((h) => {
                const cellShifts = byPersonDay.get(`${person.id}|${h.key}`) || []
                return (
                  <div
                    key={h.key}
                    className={`gantt-cell${
                      h.weekend ? ' is-weekend' : ''
                    }${h.key === todayKey ? ' is-today' : ''}${
                      h.currentWeek ? ' is-current-week' : ''
                    }`}
                  >
                    {cellShifts.map((s) => {
                      const isAbsence = isAbsenceLocation(s.locationId)
                      const color = s.locationColor || '#5B6675'
                      const soft = s.locationColorSoft || '#EEF1F5'
                      if (isAbsence) {
                        const label = absenceLabel(s.locationId)
                        const barClass = isFrancoLocation(s.locationId)
                          ? 'gantt-bar gantt-bar-franco'
                          : 'gantt-bar gantt-bar-vacation'
                        return (
                          <button
                            type="button"
                            key={s.id}
                            className={barClass}
                            style={{
                              '--bar-color': color,
                              '--bar-bg': soft,
                              cursor: canEditVacations ? 'pointer' : 'default',
                            }}
                            title={
                              canEditVacations
                                ? `${label} · tocá para quitar`
                                : label
                            }
                            onClick={() => {
                              if (canEditVacations) onAbsence?.(s)
                            }}
                          >
                            <span className="gantt-bar-loc">{label}</span>
                            <span className="gantt-bar-time">Todo el día</span>
                          </button>
                        )
                      }
                      const start = normalizeTime(s.startTime).slice(0, 5)
                      const end = normalizeTime(s.endTime).slice(0, 5)
                      return (
                        <button
                          type="button"
                          key={s.id}
                          className="gantt-bar"
                          style={{
                            '--bar-color': color,
                            '--bar-bg': soft,
                            cursor: canEdit ? 'pointer' : 'default',
                          }}
                          title={`${s.locationName || s.locationId}: ${start}–${end}`}
                          onClick={() => {
                            if (canEdit) onEdit(s)
                          }}
                        >
                          <span className="gantt-bar-loc">
                            {s.locationName || s.locationId}
                          </span>
                          <span className="gantt-bar-time">
                            {start}–{end}
                          </span>
                        </button>
                      )
                    })}
                    {canAdd && (
                      <button
                        type="button"
                        className="gantt-add"
                        title={`Asignar a ${person.name} el ${h.dow} ${h.day}`}
                        onClick={() => openAddFor(person.id, h)}
                      >
                        +
                      </button>
                    )}
                  </div>
                )
              })}
            </div>
          )
        })}
      </div>
    </div>
  )
}
