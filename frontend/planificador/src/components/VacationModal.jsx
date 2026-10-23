import { useEffect, useMemo, useRef, useState } from 'react'
import {
  absenceDayBounds,
  eachDateKey,
  normalizeTime,
  parseDateKey,
  timesOverlap,
  toDateKey,
} from '../lib/dates'
import {
  VACATION_LOCATION_ID,
  absenceLabel,
  isAbsenceLocation,
} from '../lib/locations'
import { initials, paletteFor } from '../lib/palette'
import { actionMessage, getVacationQuota, saveVacationAdjustment } from '../api/client'
import {
  FIRST_ENJOYMENT_YEAR,
  enjoymentLabel,
  enjoymentYear,
  fmtVacationRangeLabel,
  selectableEnjoymentYears,
} from '../lib/vacations'

const { startTime: VAC_START, endTime: VAC_END } = absenceDayBounds()

function shiftWhere(shift, locations) {
  if (isAbsenceLocation(shift.locationId)) {
    return `${absenceLabel(shift.locationId)} (todo el día)`
  }
  const loc = locations.find((l) => l.id === shift.locationId)
  const start = normalizeTime(shift.startTime).slice(0, 5)
  const end = normalizeTime(shift.endTime).slice(0, 5)
  const name = loc?.name || shift.locationName || 'otro local'
  return `${name} (${start}–${end})`
}

function buildVacationConflicts({
  userIds,
  dateKeys,
  shifts,
  staff,
  locations,
}) {
  if (!userIds.length || !dateKeys.length) return []
  const dateSet = new Set(dateKeys)

  return userIds.flatMap((userId) => {
    const emp = staff.find((p) => p.id === userId)
    return shifts
      .filter(
        (s) =>
          s.userId === userId &&
          dateSet.has(String(s.workDate).slice(0, 10)) &&
          timesOverlap(VAC_START, VAC_END, s.startTime, s.endTime),
      )
      .map((existing) => {
        const sameVac = existing.locationId === VACATION_LOCATION_ID
        const day = String(existing.workDate).slice(8, 10)
        const where = shiftWhere(existing, locations)
        return {
          shiftId: existing.id,
          text: sameVac
            ? `${emp?.name || 'Persona'} (día ${day}) ya tiene vacaciones: ${where}`
            : `${emp?.name || 'Persona'} (día ${day}) ya está en ${where}`,
        }
      })
  })
}

export default function VacationModal({
  open,
  staff,
  locations,
  busy,
  canWrite = true,
  fetchShiftsForRange,
  onClose,
  onSave,
}) {
  const [selectedIds, setSelectedIds] = useState([])
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [pool, setPool] = useState([])
  const [loadingPool, setLoadingPool] = useState(false)
  const [warn, setWarn] = useState('')
  const [confirmMove, setConfirmMove] = useState(false)
  const [cupoOpen, setCupoOpen] = useState(false)
  const [cupoUserId, setCupoUserId] = useState('')
  const [cupoYear, setCupoYear] = useState(() => enjoymentYear(new Date()))
  const [quota, setQuota] = useState(null)
  const [extra, setExtra] = useState('0')
  const [cupoError, setCupoError] = useState('')
  const [cupoBusy, setCupoBusy] = useState(false)
  const confirmPendingRef = useRef(false)
  const cupoYears = selectableEnjoymentYears(new Date(), cupoYear)

  const dateKeys = useMemo(() => {
    if (!dateFrom || !dateTo) return []
    if (parseDateKey(dateFrom) > parseDateKey(dateTo)) return []
    return eachDateKey(dateFrom, dateTo)
  }, [dateFrom, dateTo])

  useEffect(() => {
    if (!open) return
    setWarn('')
    confirmPendingRef.current = false
    setConfirmMove(false)
    setSelectedIds([])
    setCupoOpen(false)
    setQuota(null)
    setCupoError('')
    const today = toDateKey(new Date())
    setDateFrom(today)
    setDateTo(today)
    setCupoYear(enjoymentYear(new Date()))
  }, [open])

  useEffect(() => {
    if (!open || !cupoOpen || !cupoUserId) return
    let cancelled = false
    setCupoBusy(true)
    setCupoError('')
    getVacationQuota(cupoUserId, cupoYear)
      .then((row) => {
        if (cancelled) return
        setQuota(row)
        setExtra(String(row.extra ?? 0))
      })
      .catch((e) => {
        if (!cancelled) {
          setQuota(null)
          setCupoError(e.message)
        }
      })
      .finally(() => {
        if (!cancelled) setCupoBusy(false)
      })
    return () => {
      cancelled = true
    }
  }, [open, cupoOpen, cupoUserId, cupoYear])

  function openCupo() {
    if (selectedIds.length !== 1) {
      setWarn(
        selectedIds.length === 0
          ? 'Elegí un asistente para ver el cupo.'
          : 'Elegí un solo asistente para ver el cupo.',
      )
      return
    }
    setWarn('')
    setCupoUserId(selectedIds[0])
    const year = dateFrom ? enjoymentYear(dateFrom) : enjoymentYear(new Date())
    setCupoYear(Math.max(year, FIRST_ENJOYMENT_YEAR))
    setCupoOpen(true)
  }

  async function saveExtra() {
    const days = Number(extra)
    if (!Number.isInteger(days) || days < 0) {
      setCupoError('Ingresá un número entero de cero o más.')
      return
    }
    setCupoBusy(true)
    setCupoError('')
    try {
      const row = await saveVacationAdjustment({
        userId: cupoUserId,
        periodYear: cupoYear,
        days,
      })
      setQuota(row)
      setExtra(String(row.extra ?? 0))
    } catch (e) {
      setCupoError(e.message)
    } finally {
      setCupoBusy(false)
    }
  }

  useEffect(() => {
    if (!open || !dateFrom || !dateTo) {
      setPool([])
      return
    }
    if (parseDateKey(dateFrom) > parseDateKey(dateTo)) {
      setPool([])
      return
    }
    let cancelled = false
    ;(async () => {
      setLoadingPool(true)
      try {
        const rows = await fetchShiftsForRange(dateFrom, dateTo)
        if (!cancelled) setPool(rows)
      } catch {
        if (!cancelled) setPool([])
      } finally {
        if (!cancelled) setLoadingPool(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [open, dateFrom, dateTo, fetchShiftsForRange])

  const conflicts = useMemo(
    () =>
      buildVacationConflicts({
        userIds: selectedIds,
        dateKeys,
        shifts: pool,
        staff,
        locations,
      }),
    [selectedIds, dateKeys, pool, staff, locations],
  )

  useEffect(() => {
    confirmPendingRef.current = false
    setConfirmMove(false)
  }, [selectedIds, dateFrom, dateTo])

  if (!open) return null

  function toggleMember(id) {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    )
  }

  async function handleSave() {
    if (busy || !canWrite) return
    if (!selectedIds.length) {
      setWarn('Elegí al menos una persona.')
      return
    }
    if (!dateKeys.length) {
      setWarn('Revisá las fechas: "Desde" no puede ser posterior a "Hasta".')
      return
    }

    const currentConflicts = buildVacationConflicts({
      userIds: selectedIds,
      dateKeys,
      shifts: pool,
      staff,
      locations,
    })

    if (currentConflicts.length && !confirmPendingRef.current) {
      confirmPendingRef.current = true
      setConfirmMove(true)
      return
    }

    confirmPendingRef.current = false
    setConfirmMove(false)
    try {
      await onSave({
        userIds: selectedIds,
        dateFrom,
        dateTo,
        startTime: VAC_START,
        endTime: VAC_END,
        locationId: VACATION_LOCATION_ID,
        replaceConflictIds: currentConflicts.map((c) => c.shiftId),
      })
    } catch (e) {
      if (e.status === 401) return
      setWarn(actionMessage(e, 'No se pudieron cargar las vacaciones.'))
    }
  }

  const datesInverted = Boolean(dateFrom && dateTo && parseDateKey(dateFrom) > parseDateKey(dateTo))
  const periodYears = [...new Set(dateKeys.map((key) => enjoymentYear(key)))]
  const cupoPerson = staff.find((person) => person.id === cupoUserId)
  const typedExtra = Number(extra)
  const extraOk = Number.isInteger(typedExtra) && typedExtra >= 0
  const previewRemaining = quota && extraOk
    ? quota.remaining - (quota.extra ?? 0) + typedExtra
    : quota?.remaining
  const dayLabel = dateKeys.length === 1 ? '1 día' : `${dateKeys.length} días`
  const whoLabel = selectedIds.length === 1
    ? staff.find((person) => person.id === selectedIds[0])?.name
    : selectedIds.length > 1
      ? `${selectedIds.length} personas`
      : ''

  return (
    <div
      className="overlay open"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className="modal vacation-modal vacation-load-modal">
        <div className="vacation-modal-head">
          <div>
            <h3>{cupoOpen ? 'Cupo' : 'Cargar vacaciones'}</h3>
            {!cupoOpen && (
              <p className="vacation-load-lead">Cada día elegido queda como vacación de todo el día.</p>
            )}
          </div>
          {!cupoOpen && canWrite && (
            <button
              type="button"
              className="btn btn-ghost"
              disabled={busy || !staff.length}
              onClick={openCupo}
            >
              Cupo
            </button>
          )}
        </div>

        {cupoOpen ? (
          <div className="vacation-card vacation-cupo-card">
            <header className="vacation-card-head">
              <h4 className="vacation-report-name">{cupoPerson?.name}</h4>
            </header>
            <div className="field">
              <label htmlFor="cupo-period">Período</label>
              <select
                id="cupo-period"
                value={cupoYear}
                disabled={busy || cupoBusy}
                onChange={(e) => {
                  setQuota(null)
                  setCupoYear(Number(e.target.value))
                }}
              >
                {cupoYears.map((year) => (
                  <option key={year} value={year}>{enjoymentLabel(year)}</option>
                ))}
              </select>
            </div>
            <p className="vacation-load-lead">De este período y del período anterior no se editan. Los días extra se suman a Quedan.</p>
            {cupoError && <div className="m-warn">{cupoError}</div>}
            {cupoBusy && <div className="m-sub">Cargando…</div>}
            {quota && quota.hireDate == null && !cupoBusy && !cupoError && (
              <div className="m-sub">Falta la fecha de ingreso. Cargala en Usuarios.</div>
            )}
            {quota?.hireDate != null && (
              <div className="vacation-stats">
                <div className="vacation-stat">
                  <span>De este período</span>
                  <strong>{quota.thisPeriod}</strong>
                </div>
                <div className="vacation-stat">
                  <span>Del período anterior</span>
                  <strong>{quota.carried}</strong>
                </div>
                <div className="vacation-stat">
                  <span>Gozados</span>
                  <strong>{quota.enjoyed}</strong>
                </div>
                <div className="vacation-stat vacation-stat-extra">
                  <label htmlFor="cupo-extra">Días extra</label>
                  <input
                    id="cupo-extra"
                    type="number"
                    min="0"
                    step="1"
                    value={extra}
                    disabled={busy || cupoBusy}
                    onChange={(e) => setExtra(e.target.value)}
                  />
                </div>
                <div className={`vacation-stat ${previewRemaining < 0 ? 'is-over' : 'is-remaining'}`}>
                  <span>Quedan</span>
                  <strong>{previewRemaining}</strong>
                </div>
              </div>
            )}
            <div className="modal-actions">
              <button type="button" className="btn btn-ghost" disabled={cupoBusy} onClick={() => setCupoOpen(false)}>
                Volver
              </button>
              <button
                type="button"
                className="btn btn-primary"
                disabled={cupoBusy || !quota?.hireDate}
                onClick={() => void saveExtra()}
              >
                Guardar días extra
              </button>
            </div>
          </div>
        ) : (
        <>
        <section className="vacation-load-block">
          <div className="vacation-load-block-head">
            <h4>Fechas</h4>
            {dateKeys.length > 0 && <span>{dayLabel}</span>}
          </div>
          <div className="time-row">
            <div className="field">
              <label htmlFor="vacation-from">Desde</label>
              <input
                id="vacation-from"
                type="date"
                value={dateFrom}
                disabled={busy}
                onChange={(e) => setDateFrom(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="vacation-to">Hasta</label>
              <input
                id="vacation-to"
                type="date"
                value={dateTo}
                disabled={busy}
                onChange={(e) => setDateTo(e.target.value)}
              />
            </div>
          </div>
          {dateKeys.length > 0 && (
            <div className="vacation-ranges">
              <span className="vacation-range-chip">
                {fmtVacationRangeLabel(dateFrom, dateTo)}
                <em>{dayLabel}</em>
              </span>
              {periodYears.map((year) => (
                <span key={year} className="vacation-range-chip">{enjoymentLabel(year)}</span>
              ))}
            </div>
          )}
          {datesInverted && <div className="m-warn">Hasta no puede ser anterior a Desde.</div>}
        </section>

        <section className="vacation-load-block">
          <div className="vacation-load-block-head">
            <h4>Personas</h4>
            <span>{selectedIds.length === 0 ? 'Ninguna elegida' : whoLabel}</span>
          </div>
          <div className="vacation-people">
            {!staff.length && (
              <div className="pp-empty">Todavía no hay asistentes.</div>
            )}
            {staff.map((person) => {
              const av = paletteFor(person.id, staff).c
              const dayShifts = pool.filter((shift) => shift.userId === person.id)
              const hasOverlap =
                dateKeys.length > 0 &&
                dayShifts.some(
                  (shift) =>
                    dateKeys.includes(String(shift.workDate).slice(0, 10)) &&
                    timesOverlap(VAC_START, VAC_END, shift.startTime, shift.endTime),
                )
              const selected = selectedIds.includes(person.id)
              return (
                <label
                  className={`vacation-person${selected ? ' is-on' : ''}${hasOverlap ? ' is-busy' : ''}`}
                  key={person.id}
                >
                  <input
                    type="checkbox"
                    checked={selected}
                    disabled={busy}
                    onChange={() => toggleMember(person.id)}
                  />
                  <span className="pp-av" style={{ background: av }}>
                    {initials(person.name)}
                  </span>
                  <span className="pp-name">{person.name}</span>
                  {hasOverlap && <span className="pp-badge pp-badge-move">tiene turno</span>}
                </label>
              )
            })}
          </div>
        </section>

        {loadingPool && <div className="m-sub">Revisando turnos del rango…</div>}

        {conflicts.length > 0 && (
          <div className="vacation-conflicts">
            <strong>
              {confirmMove
                ? 'Confirmá para reemplazar estos turnos.'
                : 'Esas fechas ya tienen turno. Al cargar, se reemplazan.'}
            </strong>
            <ul>
              {conflicts.slice(0, 4).map((conflict) => (
                <li key={conflict.shiftId}>{conflict.text}</li>
              ))}
            </ul>
            {conflicts.length > 4 && <p>y {conflicts.length - 4} más</p>}
          </div>
        )}

        {warn && <div className="m-warn vacation-load-warn">{warn}</div>}

        <div className="vacation-load-summary">
          {datesInverted && 'Revisá las fechas.'}
          {!datesInverted && dateKeys.length > 0 && whoLabel && `${dayLabel} para ${whoLabel}.`}
          {!datesInverted && dateKeys.length > 0 && !whoLabel && `${dayLabel}. Elegí al menos una persona.`}
          {!datesInverted && !dateKeys.length && 'Elegí desde y hasta.'}
        </div>

        <div className="modal-actions">
          <button
            type="button"
            className="btn btn-ghost"
            disabled={busy}
            onClick={onClose}
          >
            Cerrar
          </button>
          {canWrite && (
            <button
              type="button"
              className="btn btn-primary"
              disabled={busy || !staff.length || loadingPool}
              onClick={handleSave}
            >
              {confirmMove ? 'Reemplazar y cargar' : 'Cargar vacaciones'}
            </button>
          )}
        </div>
        </>
        )}
      </div>
    </div>
  )
}
