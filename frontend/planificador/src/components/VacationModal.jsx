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
import { getVacationQuota, saveVacationAdjustment } from '../api/client'
import { enjoymentLabel, enjoymentYear } from '../lib/vacations'

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
  const currentEnjoyment = enjoymentYear(new Date())
  const cupoYears = [...new Set([currentEnjoyment - 1, currentEnjoyment, currentEnjoyment + 1, cupoYear])].sort((a, b) => a - b)

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
    const initial = selectedIds[0] || staff[0]?.id || ''
    setCupoUserId(initial)
    setCupoYear(dateFrom ? enjoymentYear(dateFrom) : enjoymentYear(new Date()))
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

  useEffect(() => {
    if (!open) return
    if (conflicts.length) {
      const n = conflicts.length
      const suffix = confirmMove
        ? ' Confirmá para reemplazar el/los turno(s) que se solapan.'
        : ` Si guardás, se reemplazarán ${n} turno(s) solapado(s).`
      const preview = conflicts
        .slice(0, 3)
        .map((c) => c.text)
        .join(' · ')
      const more = n > 3 ? ` · +${n - 3} más` : ''
      setWarn(preview + more + '.' + suffix)
    } else {
      setWarn((w) =>
        w.includes('reemplazará') ||
        w.includes('solapa') ||
        w.includes('solapado')
          ? ''
          : w,
      )
    }
  }, [conflicts, open, confirmMove])

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
    await onSave({
      userIds: selectedIds,
      dateFrom,
      dateTo,
      startTime: VAC_START,
      endTime: VAC_END,
      locationId: VACATION_LOCATION_ID,
      replaceConflictIds: currentConflicts.map((c) => c.shiftId),
    })
  }

  return (
    <div
      className="overlay open"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className="modal vacation-modal">
        <div className="vacation-modal-head">
          <h3>{cupoOpen ? 'Cupo' : 'Vacaciones'}</h3>
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
        {!cupoOpen && (
        <div className="m-sub">
          Todo el día. Las vacaciones cargadas se ven y se quitan en la vista mensual.
        </div>
        )}

        {warn && !cupoOpen && <div className="m-warn">{warn}</div>}

        {cupoOpen ? (
          <div>
            <div className="m-sub">{enjoymentLabel(cupoYear)}. De este período y del período anterior no se editan.</div>
            <div className="field">
              <label htmlFor="cupo-person">Asistente</label>
              <select
                id="cupo-person"
                value={cupoUserId}
                disabled={busy || cupoBusy}
                onChange={(e) => {
                  setQuota(null)
                  setCupoUserId(e.target.value)
                }}
              >
                {staff.map((person) => (
                  <option key={person.id} value={person.id}>{person.name}</option>
                ))}
              </select>
            </div>
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
            {cupoError && <div className="m-warn">{cupoError}</div>}
            {cupoBusy && <div className="m-sub">Cargando…</div>}
            {quota && quota.hireDate == null && !cupoBusy && !cupoError && (
              <div className="m-sub">Falta la fecha de ingreso. Cargala en Usuarios.</div>
            )}
            {quota?.hireDate != null && (
              <div className="vacation-quota">
                <div><span>De este período</span><span>{quota.thisPeriod}</span></div>
                <div><span>Del período anterior</span><span>{quota.carried}</span></div>
                <div><span>Gozados</span><span>{quota.enjoyed}</span></div>
                <div>
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
                <div className="is-remaining"><span>Quedan</span><span>{quota.remaining}</span></div>
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
                Guardar
              </button>
            </div>
          </div>
        ) : (
        <>
        <div className="time-row">
          <div className="field">
            <label>Desde</label>
            <input
              type="date"
              value={dateFrom}
              disabled={busy}
              onChange={(e) => setDateFrom(e.target.value)}
            />
          </div>
          <div className="field">
            <label>Hasta</label>
            <input
              type="date"
              value={dateTo}
              disabled={busy}
              onChange={(e) => setDateTo(e.target.value)}
            />
          </div>
        </div>

        <div className="field">
          <label>Asistentes</label>
          <div className="pick-list">
            {!staff.length && (
              <div className="pp-empty">Todavía no hay asistentes.</div>
            )}
            {staff.map((e) => {
              const av = paletteFor(e.id, staff).c
              const dayShifts = pool.filter((s) => s.userId === e.id)
              const hasOverlap =
                dateKeys.length > 0 &&
                dayShifts.some(
                  (s) =>
                    dateKeys.includes(String(s.workDate).slice(0, 10)) &&
                    timesOverlap(VAC_START, VAC_END, s.startTime, s.endTime),
                )
              return (
                <label
                  className={`pp-item${hasOverlap ? ' pp-busy' : ''}`}
                  key={e.id}
                >
                  <input
                    type="checkbox"
                    checked={selectedIds.includes(e.id)}
                    disabled={busy}
                    onChange={() => toggleMember(e.id)}
                  />
                  <span className="pp-av" style={{ background: av }}>
                    {initials(e.name)}
                  </span>
                  <span className="pp-name">{e.name}</span>
                  {hasOverlap && (
                    <span className="pp-badge pp-badge-move">solapa</span>
                  )}
                </label>
              )
            })}
          </div>
        </div>

        {loadingPool && (
          <div className="m-sub" style={{ marginBottom: 8 }}>
            Revisando turnos del rango…
          </div>
        )}

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
              {confirmMove ? 'Confirmar y guardar' : 'Guardar'}
            </button>
          )}
        </div>
        </>
        )}
      </div>
    </div>
  )
}
