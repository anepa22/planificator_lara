import { useEffect, useRef } from 'react'
import {
  HOUR_END,
  HOUR_START,
  fromMinutes,
  normalizeTime,
  toMinutes,
  yToStartTime,
} from '../lib/dates'
import { assignLanes } from '../lib/lanes'
import { paletteFor } from '../lib/palette'

export default function ScheduleGrid({
  locations,
  people,
  shifts,
  selectedDay,
  isToday,
  hourH,
  onHourHChange,
  canAdd = true,
  canEdit = true,
  vidrieraLocationIds,
  canWriteVidriera = false,
  onToggleVidriera,
  onAdd,
  onEdit,
}) {
  const gridRef = useRef(null)

  useEffect(() => {
    function fit() {
      const numHours = HOUR_END - HOUR_START
      const grid = gridRef.current
      const top = grid ? grid.getBoundingClientRect().top : 220
      const summaryEl = document.querySelector('.summary')
      const summaryH = summaryEl
        ? summaryEl.getBoundingClientRect().height + 24
        : 80
      const available = Math.max(320, window.innerHeight - top - summaryH - 24)
      const next = Math.max(28, Math.min(52, Math.floor(available / numHours)))
      onHourHChange(next)
      document.documentElement.style.setProperty('--hour-h', `${next}px`)
    }
    fit()
    let timer
    const onResize = () => {
      clearTimeout(timer)
      timer = setTimeout(fit, 120)
    }
    window.addEventListener('resize', onResize)
    return () => {
      clearTimeout(timer)
      window.removeEventListener('resize', onResize)
    }
  }, [onHourHChange, locations.length, shifts.length])

  const numHours = HOUR_END - HOUR_START
  const totalHeight = numHours * hourH
  const hours = []
  for (let h = HOUR_START; h < HOUR_END; h++) hours.push(h)

  const locCount = Math.max(locations.length, 1)

  return (
    <div className="grid-wrap" ref={gridRef}>
      <div className="grid-scroll" style={{ '--loc-count': locCount }}>
        <div className="head-row">
          <div className="cell-head corner-head" />
          {locations.map((l) => {
            const vidrieraOn = vidrieraLocationIds?.has(l.id)
            return (
              <div
                className={`cell-head loc-head${vidrieraOn ? ' is-vidriera' : ''}`}
                key={l.id}
              >
                <div className="loc-head-main">
                  <span className="ldot" style={{ background: l.color }} />
                  <span className="lname">{l.name}</span>
                </div>
                {l.supportsVidriera ? (
                  <label
                    className="vidriera-check"
                    title={
                      vidrieraOn
                        ? 'Quitar vidriera de este local en este día'
                        : 'Marcar vidriera en este local este día'
                    }
                  >
                    <input
                      type="checkbox"
                      checked={!!vidrieraOn}
                      disabled={!canWriteVidriera}
                      onChange={() => onToggleVidriera?.(l.id)}
                    />
                    Vidriera
                  </label>
                ) : null}
              </div>
            )
          })}
        </div>
        <div className="body-wrap">
          <div className="hour-col">
            {hours.map((h) => (
              <div className="hour-row" key={h}>
                {String(h).padStart(2, '0')}:00
              </div>
            ))}
          </div>
          {locations.map((loc) => {
            const dayShifts = shifts
              .filter(
                (s) =>
                  s.locationId === loc.id && Number(s.dayIndex) === selectedDay,
              )
              .map((s) => ({
                ...s,
                startTime: normalizeTime(s.startTime),
                endTime: normalizeTime(s.endTime),
              }))
              .sort(
                (a, b) => toMinutes(a.startTime) - toMinutes(b.startTime),
              )

            const { placed, laneCount } = assignLanes(dayShifts)
            const addLaneW = canAdd ? 45 : 0
            const laneW =
              laneCount > 0
                ? addLaneW
                  ? `calc((100% - ${addLaneW}px) / ${laneCount})`
                  : `calc(100% / ${laneCount})`
                : '0px'
            const compactLanes = laneCount > 3

            function openAddAt(y) {
              if (!canAdd) return
              const start = yToStartTime(y, hourH)
              const endMin = Math.min(HOUR_END * 60, toMinutes(start) + 60)
              onAdd({
                locationId: loc.id,
                startTime: start,
                endTime: fromMinutes(endMin),
              })
            }

            return (
              <div
                key={loc.id}
                className={`track${isToday ? ' is-today' : ''}`}
                style={{ height: totalHeight }}
                onClick={(e) => {
                  if (!canAdd) return
                  if (
                    e.target.closest('.shift-block') ||
                    e.target.closest('.add-lane')
                  ) {
                    return
                  }
                  const rect = e.currentTarget.getBoundingClientRect()
                  openAddAt(e.clientY - rect.top)
                }}
              >
                {placed.map((s) => {
                  const startMin = toMinutes(s.startTime)
                  const endMin = toMinutes(s.endTime)
                  const top = ((startMin - HOUR_START * 60) / 60) * hourH
                  const height = Math.max(
                    20,
                    ((endMin - startMin) / 60) * hourH,
                  )
                  const pal = paletteFor(s.personId, people)
                  const sizeClass =
                    height < 22
                      ? ' tiny'
                      : compactLanes || height < 32
                        ? ' compact'
                        : ''
                  return (
                    <div
                      key={s.id}
                      className={`shift-block${sizeClass}`}
                      style={{
                        top,
                        height: height - 2,
                        left: `calc(${laneW} * ${s.lane})`,
                        width: `calc(${laneW} - 4px)`,
                        marginLeft: 2,
                        '--block-color': pal.c,
                        '--block-bg': pal.soft,
                        '--block-ink': pal.ink,
                        cursor: canEdit ? 'pointer' : 'default',
                      }}
                      title={`${s.personName} · ${s.startTime}–${s.endTime}`}
                      onClick={(e) => {
                        e.stopPropagation()
                        if (canEdit) onEdit(s)
                      }}
                    >
                      <div className="sb-inner">
                        <span className="sb-name">{s.personName}</span>
                        <span className="sb-time">
                          {s.startTime} – {s.endTime}
                        </span>
                      </div>
                    </div>
                  )
                })}
                {canAdd && (
                  <div
                    className="add-lane"
                    title="Agregar personas en esta franja"
                    style={{
                      left: `calc(100% - ${addLaneW}px)`,
                      width: addLaneW,
                    }}
                    onClick={(e) => {
                      e.stopPropagation()
                      const rect =
                        e.currentTarget.parentElement.getBoundingClientRect()
                      openAddAt(e.clientY - rect.top)
                    }}
                  >
                    <span>+</span>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}
