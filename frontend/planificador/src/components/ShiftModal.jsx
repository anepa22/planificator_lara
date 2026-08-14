import { useEffect, useMemo, useRef, useState } from 'react'
import {
  DAYS,
  absenceDayBounds,
  addDays,
  fromMinutes,
  normalizeTime,
  parseDateKey,
  snapMinutes,
  splitRangeAssignDates,
  timesOverlap,
  toMinutes,
} from '../lib/dates'
import {
  FRANCO_LOCATION_ID,
  francoLocation,
  isAbsenceLocation,
  workLocations,
} from '../lib/locations'
import { initials, paletteFor } from '../lib/palette'
import { findContiguousWorkShiftRange } from '../lib/vacations'
import ConfirmModal from './ConfirmModal'
import VacationRemoveModal from './VacationRemoveModal'

function shiftLabel(shift, locations) {
  const loc = locations.find((l) => l.id === shift.locationId)
  const start = normalizeTime(shift.startTime).slice(0, 5)
  const end = normalizeTime(shift.endTime).slice(0, 5)
  return `${loc?.name || 'otro local'} (${start}–${end})`
}

function sameWorkDay(s, ctx, dateKeys) {
  if (dateKeys?.length) return dateKeys.includes(String(s.workDate))
  if (ctx.workDate) return String(s.workDate) === String(ctx.workDate)
  return Number(s.dayIndex) === ctx.dayIndex
}

function buildConflicts({
  ctx,
  locationId,
  personIds,
  dayShiftsByPerson,
  people,
  locations,
  startN,
  endN,
}) {
  if (!ctx || !personIds.length) return []
  if (toMinutes(endN) <= toMinutes(startN)) return []
  const locId = locationId || ctx.locationId

  return personIds.flatMap((id) => {
    const dayShifts = dayShiftsByPerson.get(id) || []
    const overlapping = dayShifts.filter((s) =>
      timesOverlap(startN, endN, s.startTime, s.endTime),
    )
    if (!overlapping.length) return []
    const emp = people.find((p) => p.id === id)
    return overlapping.map((existing) => {
      const sameLoc = existing.locationId === locId
      const where = shiftLabel(existing, locations)
      const day = String(existing.workDate).slice(8, 10)
      return {
        shiftId: existing.id,
        text: sameLoc
          ? `${emp?.name || 'Persona'} (día ${day}) ya tiene un turno que se solapa en este local: ${where}`
          : `${emp?.name || 'Persona'} (día ${day}) ya está en ${where} en ese horario`,
      }
    })
  })
}

function dedupeConflicts(list) {
  const byId = new Map()
  for (const c of list) byId.set(c.shiftId, c)
  return [...byId.values()]
}

function PersonStatusBadges({
  dayShifts,
  start,
  end,
  locationId,
  locations,
}) {
  const startN = normalizeTime(start)
  const endN = normalizeTime(end)
  const overlapping =
    toMinutes(endN) > toMinutes(startN)
      ? dayShifts.filter((s) =>
          timesOverlap(startN, endN, s.startTime, s.endTime),
        )
      : []
  const otherOk = dayShifts.filter(
    (s) => !overlapping.some((o) => o.id === s.id),
  )
  const conflictShift = overlapping[0]
  const busyElsewhere =
    conflictShift && conflictShift.locationId !== locationId
  const busyHere = conflictShift && conflictShift.locationId === locationId
  const infoShift = !conflictShift && otherOk[0]

  if (!conflictShift && !infoShift) return null

  return (
    <>
      {conflictShift && (
        <span
          className={`pp-badge${busyElsewhere ? ' pp-badge-move' : ''}`}
          title={shiftLabel(conflictShift, locations)}
        >
          {busyHere
            ? 'solapa aquí'
            : `solapa ${locations.find((l) => l.id === conflictShift.locationId)?.name || ''}`}
        </span>
      )}
      {infoShift && (
        <span
          className="pp-badge pp-badge-ok"
          title={otherOk.map((s) => shiftLabel(s, locations)).join(' · ')}
        >
          también{' '}
          {locations.find((l) => l.id === infoShift.locationId)?.name || 'otro'}
        </span>
      )}
    </>
  )
}

export default function ShiftModal({
  open,
  ctx,
  people,
  locations,
  shifts,
  weekStart,
  busy,
  canWriteFrancos = false,
  showAddSame = true,
  onClose,
  onSave,
  onDelete,
  onAddSame,
}) {
  const [selectedIds, setSelectedIds] = useState([])
  const [locationId, setLocationId] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [includeSundays, setIncludeSundays] = useState(false)
  const [asFranco, setAsFranco] = useState(false)
  const [francoWeekdays, setFrancoWeekdays] = useState([])
  const [start, setStart] = useState('09:00')
  const [end, setEnd] = useState('17:00')
  const [endInvalid, setEndInvalid] = useState(false)
  const [rangeInvalid, setRangeInvalid] = useState(false)
  const [warn, setWarn] = useState('')
  const [confirmMove, setConfirmMove] = useState(false)
  const [pendingDelete, setPendingDelete] = useState(false)
  const confirmPendingRef = useRef(false)
  const timesBeforeFrancoRef = useRef(null)

  const francoLoc = useMemo(() => francoLocation(locations), [locations])
  const { startTime: francoStart, endTime: francoEnd } = useMemo(
    () => absenceDayBounds(),
    [],
  )
  const workDeleteRange = useMemo(() => {
    if (!open || !ctx || ctx.mode !== 'edit') return null
    return findContiguousWorkShiftRange(
      shifts,
      ctx.personId,
      ctx.workDate,
      ctx.locationId,
      ctx.startTime,
      ctx.endTime,
    )
  }, [open, ctx, shifts])
  const multiDayDelete =
    !!workDeleteRange &&
    workDeleteRange.dateFrom !== workDeleteRange.dateTo
  const rangeMode = !!ctx?.rangeAssign
  const dayAssignMode = !!ctx && ctx.mode === 'add' && !rangeMode
  const showFrancoCheck =
    canWriteFrancos &&
    !!francoLoc &&
    ((dayAssignMode && !!ctx?.pickLocation) ||
      (ctx?.mode === 'edit' && !isAbsenceLocation(ctx.locationId)))
  const showRangeFrancos = rangeMode && canWriteFrancos && !!francoLoc

  const { workDates: rangeKeys, francoDates: francoRangeKeys } = useMemo(() => {
    if (!rangeMode || !dateFrom || !dateTo) {
      return { workDates: [], francoDates: [] }
    }
    if (parseDateKey(dateFrom) > parseDateKey(dateTo)) {
      return { workDates: [], francoDates: [] }
    }
    return splitRangeAssignDates(dateFrom, dateTo, {
      includeSundays,
      francoWeekdays,
    })
  }, [rangeMode, dateFrom, dateTo, includeSundays, francoWeekdays])

  const conflictDateKeys = useMemo(() => {
    if (!rangeMode) return null
    return [...new Set([...rangeKeys, ...francoRangeKeys])]
  }, [rangeMode, rangeKeys, francoRangeKeys])

  const francoOnlyRange =
    rangeMode && francoRangeKeys.length > 0 && rangeKeys.length === 0
  const rangeHasNoDays =
    rangeMode && !rangeKeys.length && !francoRangeKeys.length
  const rangeNeedsLocation = rangeMode && rangeKeys.length > 0
  const dayNeedsLocation = !rangeMode && !!ctx?.pickLocation

  const pickLocations = useMemo(() => workLocations(locations), [locations])

  const effectiveLocationId = asFranco
    ? FRANCO_LOCATION_ID
    : locationId || ctx?.locationId || ''

  const loc = useMemo(
    () => locations.find((l) => l.id === effectiveLocationId),
    [locations, effectiveLocationId],
  )

  const dDate = useMemo(() => {
    if (!ctx) return null
    if (ctx.workDate) return parseDateKey(ctx.workDate)
    return addDays(weekStart, ctx.dayIndex)
  }, [ctx, weekStart])

  const dayShiftsByPerson = useMemo(() => {
    if (!ctx) return new Map()
    const map = new Map()
    for (const s of shifts) {
      if (!sameWorkDay(s, ctx, rangeMode ? conflictDateKeys : null)) continue
      if (ctx.mode === 'edit' && s.id === ctx.shiftId) continue
      if (!map.has(s.personId)) map.set(s.personId, [])
      map.get(s.personId).push(s)
    }
    return map
  }, [ctx, shifts, rangeMode, conflictDateKeys])

  const activePersonIds = useMemo(() => {
    if (!ctx) return []
    return ctx.mode === 'edit' ? [ctx.personId] : selectedIds
  }, [ctx, selectedIds])

  function clearConfirm() {
    confirmPendingRef.current = false
    setConfirmMove(false)
  }

  useEffect(() => {
    if (!open || !ctx) return
    setWarn('')
    setEndInvalid(false)
    setRangeInvalid(false)
    setAsFranco(false)
    setFrancoWeekdays([])
    timesBeforeFrancoRef.current = null
    setPendingDelete(false)
    clearConfirm()

    if (ctx.mode === 'edit') {
      setSelectedIds([])
      setLocationId(ctx.locationId || '')
      setDateFrom(ctx.workDate || '')
      setDateTo(ctx.workDate || '')
      setStart(ctx.startTime)
      setEnd(ctx.endTime)
    } else {
      setSelectedIds(ctx.preselect || [])
      setLocationId(
        ctx.locationId || workLocations(locations)[0]?.id || '',
      )
      setDateFrom(ctx.dateFrom || ctx.workDate || '')
      setDateTo(ctx.dateTo || ctx.workDate || '')
      setIncludeSundays(false)
      setStart(ctx.startTime || '09:00')
      setEnd(ctx.endTime || (rangeMode ? '17:00' : '10:00'))
    }
  }, [open, ctx, locations, rangeMode])

  function setFrancoChecked(checked) {
    clearConfirm()
    if (checked) {
      timesBeforeFrancoRef.current = {
        start,
        end,
        locationId: locationId || ctx?.locationId || '',
      }
      setAsFranco(true)
      setStart(francoStart)
      setEnd(francoEnd)
      setEndInvalid(false)
    } else {
      const prev = timesBeforeFrancoRef.current
      setAsFranco(false)
      if (prev) {
        setStart(prev.start)
        setEnd(prev.end)
        if (prev.locationId) setLocationId(prev.locationId)
      }
      timesBeforeFrancoRef.current = null
    }
  }

  function toggleFrancoWeekday(dayIdx) {
    clearConfirm()
    setFrancoWeekdays((prev) =>
      prev.includes(dayIdx)
        ? prev.filter((d) => d !== dayIdx)
        : [...prev, dayIdx].sort((a, b) => a - b),
    )
  }

  function dayShiftsForDates(dateKeys) {
    const set = new Set(dateKeys)
    const map = new Map()
    for (const [pid, list] of dayShiftsByPerson) {
      const filtered = list.filter((s) =>
        set.has(String(s.workDate).slice(0, 10)),
      )
      if (filtered.length) map.set(pid, filtered)
    }
    return map
  }

  const conflicts = useMemo(() => {
    if (!ctx || !activePersonIds.length) return []

    if (rangeMode) {
      const workC =
        rangeKeys.length > 0
          ? buildConflicts({
              ctx,
              locationId: locationId || ctx.locationId,
              personIds: activePersonIds,
              dayShiftsByPerson: dayShiftsForDates(rangeKeys),
              people,
              locations,
              startN: normalizeTime(start),
              endN: normalizeTime(end),
            })
          : []
      const francoC =
        francoRangeKeys.length > 0
          ? buildConflicts({
              ctx,
              locationId: FRANCO_LOCATION_ID,
              personIds: activePersonIds,
              dayShiftsByPerson: dayShiftsForDates(francoRangeKeys),
              people,
              locations,
              startN: francoStart,
              endN: francoEnd,
            })
          : []
      return dedupeConflicts([...workC, ...francoC])
    }

    return buildConflicts({
      ctx,
      locationId: effectiveLocationId,
      personIds: activePersonIds,
      dayShiftsByPerson,
      people,
      locations,
      startN: normalizeTime(start),
      endN: normalizeTime(end),
    })
  }, [
    ctx,
    rangeMode,
    rangeKeys,
    francoRangeKeys,
    locationId,
    effectiveLocationId,
    activePersonIds,
    dayShiftsByPerson,
    people,
    locations,
    start,
    end,
    francoStart,
    francoEnd,
  ])

  useEffect(() => {
    clearConfirm()
  }, [
    selectedIds,
    locationId,
    asFranco,
    francoWeekdays,
    dateFrom,
    dateTo,
    includeSundays,
    start,
    end,
  ])

  useEffect(() => {
    if (!open || !ctx) return
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
        w.includes('reemplazará') || w.includes('solapa') || w.includes('solapado')
          ? ''
          : w,
      )
    }
  }, [conflicts, open, ctx, confirmMove])

  if (!open || !ctx) return null

  const noPeople = people.length === 0
  const noLocations = pickLocations.length === 0
  const minDate = ctx.minDate || ''
  const maxDate = ctx.maxDate || ''

  const subtitle = rangeMode
    ? 'Turnos del rango · francos por día de semana (un solo franco: usá +)'
    : asFranco && dDate
      ? `Franco · ${DAYS[ctx.dayIndex]} ${dDate.getDate()}/${dDate.getMonth() + 1} · todo el día`
      : loc && dDate
        ? `${loc.name} · ${DAYS[ctx.dayIndex]} ${dDate.getDate()}/${dDate.getMonth() + 1}${
            ctx.mode === 'add' && !ctx.pickLocation ? ' · misma franja' : ''
          }`
        : dDate
          ? `${DAYS[ctx.dayIndex]} ${dDate.getDate()}/${dDate.getMonth() + 1}`
          : ''

  const editPerson =
    ctx.mode === 'edit' ? people.find((p) => p.id === ctx.personId) : null
  const lockedPersonId =
    ctx.personLocked
      ? ctx.preselect?.[0] || ctx.personId || selectedIds[0]
      : null
  const lockedPerson = lockedPersonId
    ? people.find((p) => p.id === lockedPersonId)
    : null
  const editDayShifts =
    ctx.mode === 'edit' ? dayShiftsByPerson.get(ctx.personId) || [] : []
  const lockedDayShifts = lockedPersonId
    ? dayShiftsByPerson.get(lockedPersonId) || []
    : []
  const showPeoplePick =
    ctx.mode === 'add' && !noPeople && !rangeMode && !ctx.personLocked
  const showLockedPerson =
    ctx.mode === 'add' && !noPeople && !!lockedPerson && !!ctx.personLocked
  const showLocationPick =
    ctx.mode === 'edit' ||
    (ctx.mode === 'add' && (rangeNeedsLocation || dayNeedsLocation))

  function togglePerson(id) {
    clearConfirm()
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    )
  }

  async function handleSave() {
    const startSnapped = asFranco
      ? francoStart
      : fromMinutes(snapMinutes(toMinutes(start)))
    const endSnapped = asFranco
      ? francoEnd
      : fromMinutes(snapMinutes(toMinutes(end)))
    if (!asFranco && toMinutes(endSnapped) <= toMinutes(startSnapped)) {
      setStart(startSnapped)
      setEnd(endSnapped)
      setEndInvalid(true)
      return
    }
    setEndInvalid(false)

    if (ctx.mode === 'add' && !selectedIds.length) {
      setWarn('Elegí al menos una persona para esta franja.')
      return
    }
    const effectiveLoc = asFranco
      ? FRANCO_LOCATION_ID
      : locationId || ctx.locationId
    if (!rangeMode && !effectiveLoc) {
      setWarn(asFranco ? 'No está disponible el local Franco.' : 'Elegí un local.')
      return
    }

    if (rangeMode) {
      if (!dateFrom || !dateTo || parseDateKey(dateFrom) > parseDateKey(dateTo)) {
        setRangeInvalid(true)
        setWarn('Revisá las fechas: "Desde" no puede ser posterior a "Hasta".')
        return
      }
      setRangeInvalid(false)
    }

    const personIds = ctx.mode === 'edit' ? [ctx.personId] : selectedIds
    let currentConflicts
    if (rangeMode) {
      const workC =
        rangeKeys.length > 0
          ? buildConflicts({
              ctx,
              locationId: effectiveLoc,
              personIds,
              dayShiftsByPerson: dayShiftsForDates(rangeKeys),
              people,
              locations,
              startN: startSnapped,
              endN: endSnapped,
            })
          : []
      const francoC =
        francoRangeKeys.length > 0
          ? buildConflicts({
              ctx,
              locationId: FRANCO_LOCATION_ID,
              personIds,
              dayShiftsByPerson: dayShiftsForDates(francoRangeKeys),
              people,
              locations,
              startN: francoStart,
              endN: francoEnd,
            })
          : []
      currentConflicts = dedupeConflicts([...workC, ...francoC])
    } else {
      currentConflicts = buildConflicts({
        ctx,
        locationId: effectiveLoc,
        personIds,
        dayShiftsByPerson,
        people,
        locations,
        startN: startSnapped,
        endN: endSnapped,
      })
    }

    if (currentConflicts.length && !confirmPendingRef.current) {
      confirmPendingRef.current = true
      setConfirmMove(true)
      setStart(startSnapped)
      setEnd(endSnapped)
      return
    }

    const replaceConflictIds = currentConflicts.map((c) => c.shiftId)
    clearConfirm()
    setStart(startSnapped)
    setEnd(endSnapped)

    if (ctx.mode === 'edit') {
      await onSave({
        mode: 'edit',
        shiftId: ctx.shiftId,
        personId: ctx.personId,
        locationId: effectiveLoc,
        dayIndex: ctx.dayIndex,
        workDate: ctx.workDate,
        startTime: startSnapped,
        endTime: endSnapped,
        replaceConflictIds,
      })
      return
    }

    if (rangeMode) {
      if (rangeHasNoDays) {
        setWarn(
          includeSundays || francoWeekdays.length
            ? 'Revisá las fechas del rango.'
            : 'No hay días para asignar (el rango solo tiene domingos, o está vacío). Marcá “Incluir domingos” o un día de franco.',
        )
        return
      }
      if (francoOnlyRange) {
        setWarn(
          'No podés dejar el rango solo con francos. Desmarcá al menos un día de la semana para turno, o cargá el franco día por día con +.',
        )
        return
      }
      if (rangeKeys.length && !effectiveLoc) {
        setWarn('Elegí un local para los días de turno.')
        return
      }
      if (francoRangeKeys.length && !francoLoc) {
        setWarn('No está disponible el local Franco.')
        return
      }
      await onSave({
        mode: 'add',
        rangeAssign: true,
        personIds: selectedIds,
        locationId: effectiveLoc,
        dateFrom,
        dateTo,
        includeSundays,
        francoWeekdays,
        startTime: startSnapped,
        endTime: endSnapped,
        francoStartTime: francoStart,
        francoEndTime: francoEnd,
        replaceConflictIds,
      })
      return
    }

    await onSave({
      mode: 'add',
      personIds: selectedIds,
      locationId: effectiveLoc,
      dayIndex: ctx.dayIndex,
      workDate: ctx.workDate,
      startTime: startSnapped,
      endTime: endSnapped,
      replaceConflictIds,
    })
  }

  return (
    <>
    <div
      className="overlay open"
      onClick={(e) => {
        if (e.target === e.currentTarget && !pendingDelete) onClose()
      }}
    >
      <div className="modal">
        <h3>
          {noPeople
            ? 'Agregá personas primero'
            : ctx.mode === 'edit'
              ? asFranco
                ? 'Editar · franco'
                : 'Editar turno'
              : rangeMode
                ? 'Asignar por rango'
                : asFranco
                  ? 'Asignar franco'
                  : 'Asignar turno'}
        </h3>
        <div className="m-sub">
          {noPeople
            ? 'Cargá personal desde el menú (Personal).'
            : subtitle}
        </div>
        {warn && <div className="m-warn">{warn}</div>}

        {showFrancoCheck && (
          <label className={`check-card${asFranco ? ' is-on' : ''}`}>
            <input
              type="checkbox"
              checked={asFranco}
              disabled={noPeople || busy}
              onChange={(e) => setFrancoChecked(e.target.checked)}
            />
            <span className="check-card-box" aria-hidden />
            <span className="check-card-text">
              <span className="check-card-title">Día de franco</span>
              <span className="check-card-sub">
                Todo el día · se ve en la vista mensual
              </span>
            </span>
          </label>
        )}

        {showLocationPick && !asFranco && (
          <div className="field">
            <label>Local</label>
            {noLocations ? (
              <div className="m-sub" style={{ marginBottom: 0 }}>
                No hay locales disponibles.
              </div>
            ) : (
              <select
                value={locationId}
                disabled={busy}
                onChange={(e) => setLocationId(e.target.value)}
              >
                {pickLocations.map((l) => (
                  <option key={l.id} value={l.id}>
                    {l.name}
                  </option>
                ))}
              </select>
            )}
          </div>
        )}

        {ctx.mode === 'edit' && !noPeople && (
          <div className="field">
            <label>Persona</label>
            <div
              className={`pp-edit-person${conflicts.length ? ' pp-busy' : ''}`}
            >
              <span
                className="pp-av"
                style={{ background: paletteFor(ctx.personId, people).c }}
              >
                {initials(editPerson?.name)}
              </span>
              <span className="pp-name">{editPerson?.name || 'Persona'}</span>
              <PersonStatusBadges
                dayShifts={editDayShifts}
                start={start}
                end={end}
                locationId={effectiveLocationId}
                locations={locations}
              />
            </div>
          </div>
        )}

        {showLockedPerson && (
          <div className="field">
            <label>Persona</label>
            <div
              className={`pp-edit-person${conflicts.length ? ' pp-busy' : ''}`}
            >
              <span
                className="pp-av"
                style={{ background: paletteFor(lockedPerson.id, people).c }}
              >
                {initials(lockedPerson.name)}
              </span>
              <span className="pp-name">{lockedPerson.name}</span>
              <PersonStatusBadges
                dayShifts={lockedDayShifts}
                start={start}
                end={end}
                locationId={effectiveLocationId}
                locations={locations}
              />
            </div>
          </div>
        )}

        {showPeoplePick && (
          <div className="field">
            <label>Personas en esta franja</label>
            <div className="people-pick">
              {people.map((e) => {
                const av = paletteFor(e.id, people).c
                const dayShifts = dayShiftsByPerson.get(e.id) || []
                const startN = normalizeTime(start)
                const endN = normalizeTime(end)
                const hasOverlap =
                  toMinutes(endN) > toMinutes(startN) &&
                  dayShifts.some((s) =>
                    timesOverlap(startN, endN, s.startTime, s.endTime),
                  )
                return (
                  <label
                    className={`pp-item${hasOverlap ? ' pp-busy' : ''}`}
                    key={e.id}
                  >
                    <input
                      type="checkbox"
                      checked={selectedIds.includes(e.id)}
                      onChange={() => togglePerson(e.id)}
                    />
                    <span className="pp-av" style={{ background: av }}>
                      {initials(e.name)}
                    </span>
                    <span className="pp-name">{e.name}</span>
                    <PersonStatusBadges
                      dayShifts={dayShifts}
                      start={start}
                      end={end}
                      locationId={effectiveLocationId}
                      locations={locations}
                    />
                  </label>
                )
              })}
            </div>
          </div>
        )}

        {rangeMode && ctx.mode === 'add' && (
          <>
            <div className="time-row">
              <div className="field">
                <label>Desde</label>
                <input
                  type="date"
                  value={dateFrom}
                  min={minDate || undefined}
                  max={maxDate || undefined}
                  disabled={noPeople || busy}
                  style={rangeInvalid ? { borderColor: '#C4436D' } : undefined}
                  onChange={(e) => setDateFrom(e.target.value)}
                />
              </div>
              <div className="field">
                <label>Hasta</label>
                <input
                  type="date"
                  value={dateTo}
                  min={minDate || undefined}
                  max={maxDate || undefined}
                  disabled={noPeople || busy}
                  style={rangeInvalid ? { borderColor: '#C4436D' } : undefined}
                  onChange={(e) => setDateTo(e.target.value)}
                />
              </div>
            </div>
            <label className={`check-card check-card-compact${includeSundays ? ' is-on' : ''}`}>
              <input
                type="checkbox"
                checked={includeSundays}
                disabled={noPeople || busy}
                onChange={(e) => setIncludeSundays(e.target.checked)}
              />
              <span className="check-card-box" aria-hidden />
              <span className="check-card-text">
                <span className="check-card-title">Incluir domingos en turnos</span>
                <span className="check-card-sub">
                  Independiente de marcar Dom como franco
                </span>
              </span>
            </label>
            {showRangeFrancos && (
              <div className="field">
                <label>Días de franco</label>
                <div className="m-sub" style={{ marginTop: -4, marginBottom: 8 }}>
                  Quedan libres (todo el día). El resto del rango, turno.
                </div>
                <div className="weekday-pick" role="group" aria-label="Días de franco">
                  {DAYS.map((label, idx) => {
                    const on = francoWeekdays.includes(idx)
                    return (
                      <label
                        className={`weekday-chip${on ? ' is-on' : ''}`}
                        key={label}
                      >
                        <input
                          type="checkbox"
                          checked={on}
                          disabled={noPeople || busy}
                          onChange={() => toggleFrancoWeekday(idx)}
                        />
                        <span className="weekday-chip-label">{label}</span>
                      </label>
                    )
                  })}
                </div>
              </div>
            )}
            {dateFrom && dateTo && !rangeInvalid && (
              <div className="m-sub" style={{ marginTop: -4, marginBottom: 10 }}>
                {francoOnlyRange
                  ? 'No podés dejar el rango solo con francos. Desmarcá al menos un día de la semana para que quede como turno, o cargá el franco día por día con +.'
                  : [
                      rangeKeys.length
                        ? `${rangeKeys.length} día${rangeKeys.length === 1 ? '' : 's'} de turno`
                        : null,
                      francoRangeKeys.length
                        ? `${francoRangeKeys.length} día${francoRangeKeys.length === 1 ? '' : 's'} de franco`
                        : null,
                    ]
                      .filter(Boolean)
                      .join(' · ') || 'Ningún día a asignar con esta opción'}
              </div>
            )}
          </>
        )}

        {!francoOnlyRange && (
        <div className="time-row">
          <div className="field">
            <label>Inicio</label>
            <input
              type="time"
              value={start}
              step={900}
              disabled={noPeople || busy || asFranco}
              onChange={(e) => setStart(e.target.value)}
            />
          </div>
          <div className="field">
            <label>Fin</label>
            <input
              type="time"
              value={end}
              step={900}
              disabled={noPeople || busy || asFranco}
              style={endInvalid ? { borderColor: '#C4436D' } : undefined}
              onChange={(e) => setEnd(e.target.value)}
            />
          </div>
        </div>
        )}
        {asFranco && (
          <div className="m-sub" style={{ marginTop: -6, marginBottom: 10 }}>
            El horario queda bloqueado: franco todo el día.
          </div>
        )}

        <div className="modal-actions">
          {ctx.mode === 'edit' && !noPeople && (
            <button
              type="button"
              className="btn btn-delete"
              disabled={busy}
              onClick={() => setPendingDelete(true)}
            >
              Quitar
            </button>
          )}
          {showAddSame && ctx.mode === 'edit' && !noPeople && (
            <button
              type="button"
              className="btn btn-add-same"
              disabled={busy || asFranco}
              onClick={() => onAddSame(start, end)}
            >
              + Otra persona
            </button>
          )}
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Cancelar
          </button>
          <button
            type="button"
            className={`btn btn-primary${confirmMove ? ' btn-confirm-move' : ''}`}
            disabled={
              noPeople ||
              rangeHasNoDays ||
              francoOnlyRange ||
              (rangeNeedsLocation && noLocations) ||
              (dayNeedsLocation && !asFranco && noLocations) ||
              busy
            }
            onClick={handleSave}
          >
            {confirmMove ? 'Sí, reemplazar' : 'Guardar'}
          </button>
        </div>
      </div>
    </div>

    {multiDayDelete ? (
      <VacationRemoveModal
        open={pendingDelete}
        kind="shift"
        personName={editPerson?.name}
        workDate={ctx.workDate}
        dateFrom={workDeleteRange.dateFrom}
        dateTo={workDeleteRange.dateTo}
        busy={busy}
        onClose={() => setPendingDelete(false)}
        onRemoveDay={() => {
          setPendingDelete(false)
          onDelete([ctx.shiftId])
        }}
        onRemoveRange={() => {
          setPendingDelete(false)
          onDelete(workDeleteRange.shiftIds)
        }}
      />
    ) : (
      <ConfirmModal
        open={pendingDelete}
        title="Quitar turno"
        message="¿Quitar este turno? Esta acción no se puede deshacer desde acá."
        busy={busy}
        onClose={() => setPendingDelete(false)}
        onConfirm={() => {
          setPendingDelete(false)
          onDelete([ctx.shiftId])
        }}
      />
    )}
    </>
  )
}
