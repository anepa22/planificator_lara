import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  createShift,
  assignTask,
  deleteShift,
  deleteVidriera,
  getHolidays,
  getLocations,
  getShifts,
  getStaff,
  getTaskAssignees,
  getTaskBoard,
  getVidrieras,
  moveTask,
  putVidriera,
  retireTask,
  unassignTask,
  updateShift,
} from './api/client'
import { useAuth } from './auth/useAuth'
import AppMenu from './components/AppMenu'
import AuditModal from './components/AuditModal'
import DayTabs from './components/DayTabs'
import ChangePasswordModal from './components/ChangePasswordModal'
import LoginScreen from './components/LoginScreen'
import LunchModal from './components/LunchModal'
import MonthGantt from './components/MonthGantt'
import ScheduleGrid from './components/ScheduleGrid'
import ShiftModal from './components/ShiftModal'
import SummaryBar from './components/SummaryBar'
import TaskAdminModal from './components/TaskAdminModal'
import TaskBoard from './components/TaskBoard'
import TaskRetentionModal from './components/TaskRetentionModal'
import UsersModal from './components/UsersModal'
import VacationModal from './components/VacationModal'
import VacationRemoveModal from './components/VacationRemoveModal'
import VidrieraModal from './components/VidrieraModal'
import WeekNav from './components/WeekNav'
import {
  addDays,
  eachDateKey,
  fmtMonthLabel,
  fmtWeekRange,
  monthBounds,
  monthStartForOffset,
  normalizeTime,
  parseDateKey,
  splitRangeAssignDates,
  absenceDayBounds,
  timesOverlap,
  toDateKey,
  weekOffsetForDate,
  weekStartForOffset,
  weekStartsOverlappingMonth,
  weekStartsOverlappingRange,
  workDateFor,
} from './lib/dates'
import {
  FRANCO_LOCATION_ID,
  VACATION_LOCATION_ID,
  francoLocation,
  isAbsenceLocation,
  vacationLocation,
  workLocations,
} from './lib/locations'
import { loadLunchHours, saveLunchHours } from './lib/prefs'
import { findContiguousAbsenceRange } from './lib/vacations'
import { appVersionLabel } from './lib/version'
import './styles/planificador.css'

function activeOnly(list) {
  return list.filter((x) => x.active !== false)
}

function App() {
  const { user, booting, can, logout, refreshMe } = useAuth()
  const canWriteShifts = can('shifts:write')
  const canWriteVacations = can('vacations:write')
  const canManageLunch = can('lunch:manage')
  const canManageUsers = can('users:manage') || can('roles:manage')
  const canReadAudit = can('audit:read')
  const canWriteTasks = can('tasks:write')
  const canManageTasks = can('tasks:manage')
  const canManageTaskRetention = can('tasks:retention')

  const [locations, setLocations] = useState([])
  const [staff, setStaff] = useState([])
  const [shifts, setShifts] = useState([])
  const [monthShifts, setMonthShifts] = useState([])
  const [vidrieras, setVidrieras] = useState([])
  const [monthVidrieras, setMonthVidrieras] = useState([])
  const [taskBoard, setTaskBoard] = useState([])
  const [taskAssignees, setTaskAssignees] = useState([])
  const [holidaysByDate, setHolidaysByDate] = useState({})
  const [view, setView] = useState('tasks')
  const [lunchHours, setLunchHours] = useState(() => loadLunchHours())
  const [weekOffset, setWeekOffset] = useState(0)
  const [monthOffset, setMonthOffset] = useState(0)
  const [monthFocusNonce, setMonthFocusNonce] = useState(0)
  const [monthFocusDateKey, setMonthFocusDateKey] = useState(null)
  const [monthLocFilter, setMonthLocFilter] = useState([])
  const [selectedDay, setSelectedDay] = useState(
    () => (new Date().getDay() + 6) % 7,
  )
  const [hourH, setHourH] = useState(44)
  const [loading, setLoading] = useState(true)
  const [taskLoading, setTaskLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [shiftModal, setShiftModal] = useState(null)
  const [usersOpen, setUsersOpen] = useState(false)
  const [auditOpen, setAuditOpen] = useState(false)
  const [lunchOpen, setLunchOpen] = useState(false)
  const [vacationOpen, setVacationOpen] = useState(false)
  const [vidrieraOpen, setVidrieraOpen] = useState(false)
  const [vidrieraModalDate, setVidrieraModalDate] = useState(null)
  const [taskAdminOpen, setTaskAdminOpen] = useState(false)
  const [taskRetentionOpen, setTaskRetentionOpen] = useState(false)
  const [absenceRemove, setAbsenceRemove] = useState(null)
  const [menuOpen, setMenuOpen] = useState(false)
  const [loginOpen, setLoginOpen] = useState(false)
  const [changePasswordOpen, setChangePasswordOpen] = useState(false)

  useEffect(() => {
    document.body.classList.toggle('menu-open', menuOpen)
    return () => document.body.classList.remove('menu-open')
  }, [menuOpen])

  const mustChangePassword = !!user?.mustChangePassword

  useEffect(() => {
    if (!mustChangePassword) return
    setChangePasswordOpen(true)
    setMenuOpen(false)
    setLoginOpen(false)
  }, [mustChangePassword])

  useEffect(() => {
    const ids = new Set(locations.map((l) => l.id))
    setMonthLocFilter((prev) => {
      const next = prev.filter((id) => ids.has(id))
      return next.length === prev.length ? prev : next
    })
  }, [locations])

  function goToday() {
    if (view === 'month') {
      setMonthOffset(0)
      setMonthFocusDateKey(toDateKey(new Date()))
      setMonthFocusNonce((n) => n + 1)
    } else {
      setWeekOffset(0)
      setSelectedDay((new Date().getDay() + 6) % 7)
    }
  }

  function focusMonthOnDate(dateKey) {
    if (!dateKey) return
    const d = parseDateKey(dateKey)
    const now = new Date()
    setMonthOffset(
      (d.getFullYear() - now.getFullYear()) * 12 +
        (d.getMonth() - now.getMonth()),
    )
    setMonthFocusDateKey(dateKey)
    setView('month')
    setMonthFocusNonce((n) => n + 1)
  }

  const weekStart = weekStartForOffset(weekOffset)
  const weekKey = toDateKey(weekStart)
  const monthDate = monthStartForOffset(monthOffset)

  const holidayYears = useMemo(() => {
    if (view === 'month') {
      return [monthStartForOffset(monthOffset).getFullYear()]
    }
    const start = weekStartForOffset(weekOffset)
    const end = addDays(start, 6)
    const y0 = start.getFullYear()
    const y1 = end.getFullYear()
    return y0 === y1 ? [y0] : [y0, y1]
  }, [view, weekOffset, monthOffset])

  const visibleMonthShifts = useMemo(() => {
    if (!monthLocFilter.length) return monthShifts
    const ids = new Set(monthLocFilter)
    return monthShifts.filter((s) => ids.has(s.locationId))
  }, [monthShifts, monthLocFilter])

  const visibleMonthStaff = useMemo(() => {
    if (!monthLocFilter.length) return staff
    const userIds = new Set(visibleMonthShifts.map((s) => s.userId))
    return staff.filter((p) => userIds.has(p.id))
  }, [staff, monthLocFilter, visibleMonthShifts])

  const visibleMonthVidrieras = useMemo(() => {
    if (!monthLocFilter.length) return monthVidrieras
    const ids = new Set(monthLocFilter)
    return monthVidrieras.filter((v) => ids.has(v.locationId))
  }, [monthVidrieras, monthLocFilter])

  const selectedDateKey = workDateFor(weekStart, selectedDay)
  const vidrieraLocationIds = useMemo(() => {
    const ids = new Set()
    for (const v of vidrieras) {
      if (String(v.workDate).slice(0, 10) === selectedDateKey) {
        ids.add(v.locationId)
      }
    }
    return ids
  }, [vidrieras, selectedDateKey])

  function toggleMonthLocFilter(locationId) {
    setMonthLocFilter((prev) =>
      prev.includes(locationId)
        ? prev.filter((id) => id !== locationId)
        : [...prev, locationId],
    )
  }

  const isTodayCol = (() => {
    const d = new Date(weekStart)
    d.setDate(d.getDate() + selectedDay)
    const t = new Date()
    return (
      d.getFullYear() === t.getFullYear() &&
      d.getMonth() === t.getMonth() &&
      d.getDate() === t.getDate()
    )
  })()

  // TODO: sincronizar vistas abiertas (polling o SSE) — ver TODO.md
  const reloadWeek = useCallback(async (key) => {
    const from = key
    const to = toDateKey(addDays(parseDateKey(key), 6))
    const [weekShifts, weekVidrieras] = await Promise.all([
      getShifts(key),
      getVidrieras(from, to),
    ])
    setShifts(weekShifts)
    setVidrieras(weekVidrieras)
  }, [])

  const reloadTaskBoard = useCallback(async () => {
    setTaskBoard(await getTaskBoard())
    if (canManageTasks) {
      try {
        setTaskAssignees(await getTaskAssignees())
      } catch {
        setTaskAssignees([])
      }
    }
  }, [canManageTasks])

  function handleLunchHoursChange(value) {
    setLunchHours(saveLunchHours(value))
  }

  const fetchShiftsByWeekKeys = useCallback(async (keys) => {
    if (!keys.length) return []
    const arrays = await Promise.all(keys.map((k) => getShifts(k)))
    const seen = new Set()
    const all = []
    for (const row of arrays.flat()) {
      if (seen.has(row.id)) continue
      seen.add(row.id)
      all.push(row)
    }
    return all
  }, [])

  const reloadMonth = useCallback(
    async (date) => {
      const { min, max } = monthBounds(date)
      const [all, monthV] = await Promise.all([
        fetchShiftsByWeekKeys(weekStartsOverlappingMonth(date)),
        getVidrieras(min, max),
      ])
      const y = date.getFullYear()
      const m = date.getMonth()
      setMonthShifts(
        all.filter((s) => {
          const d = parseDateKey(s.workDate)
          return d.getFullYear() === y && d.getMonth() === m
        }),
      )
      setMonthVidrieras(monthV)
    },
    [fetchShiftsByWeekKeys],
  )

  useEffect(() => {
    if (booting) return
    let cancelled = false
    ;(async () => {
      setLoading(true)
      setError(null)
      try {
        const [locs, members] = await Promise.all([getLocations(), getStaff()])
        if (cancelled) return
        setLocations(activeOnly(locs))
        setStaff(activeOnly(members))
      } catch (e) {
        if (!cancelled) setError(e.message)
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [booting])

  useEffect(() => {
    if (booting || view !== 'week') return
    let cancelled = false
    ;(async () => {
      try {
        await reloadWeek(weekKey)
      } catch (e) {
        if (!cancelled) setError(e.message)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [booting, view, weekKey, reloadWeek])

  useEffect(() => {
    if (booting || view !== 'month') return
    let cancelled = false
    const date = monthStartForOffset(monthOffset)
    ;(async () => {
      try {
        await reloadMonth(date)
      } catch (e) {
        if (!cancelled) setError(e.message)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [booting, view, monthOffset, reloadMonth])

  useEffect(() => {
    if (booting || view !== 'tasks') return
    let cancelled = false
    ;(async () => {
      setTaskLoading(true)
      try {
        await reloadTaskBoard()
      } catch (e) {
        if (!cancelled) setError(e.message)
      } finally {
        if (!cancelled) setTaskLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [booting, view, reloadTaskBoard])

  useEffect(() => {
    if (booting) return
    let cancelled = false
    ;(async () => {
      try {
        const lists = await Promise.all(holidayYears.map((y) => getHolidays(y)))
        if (cancelled) return
        setHolidaysByDate((prev) => {
          const next = { ...prev }
          for (const list of lists) {
            for (const h of list || []) {
              const key = String(h.date).slice(0, 10)
              if (!key) continue
              next[key] = { name: h.name, type: h.type }
            }
          }
          return next
        })
      } catch {
        // Feriados son solo visuales; no bloquear el planificador
      }
    })()
    return () => {
      cancelled = true
    }
  }, [booting, holidayYears])

  async function withBusy(fn) {
    setBusy(true)
    setError(null)
    try {
      await fn()
    } catch (e) {
      if (e.status === 401) {
        setLoginOpen(true)
        setError('Iniciá sesión para continuar')
      } else {
        setError(e.message)
      }
      throw e
    } finally {
      setBusy(false)
    }
  }

  async function refreshAfterShiftChange() {
    if (view === 'month') {
      await reloadMonth(monthDate)
    } else {
      await reloadWeek(weekKey)
    }
  }

  async function handleAssignTask(taskId, userId) {
    await withBusy(async () => {
      await assignTask(taskId, userId)
      await reloadTaskBoard()
    })
  }

  async function handleUnassignTask(taskId) {
    await withBusy(async () => {
      await unassignTask(taskId)
      await reloadTaskBoard()
    })
  }

  async function handleMoveTask(taskId, status, blockReason) {
    await withBusy(async () => {
      await moveTask(taskId, status, blockReason)
      await reloadTaskBoard()
    })
  }

  async function handleRetireTask(taskId) {
    await withBusy(async () => {
      await retireTask(taskId)
      await reloadTaskBoard()
    })
  }

  const reloadStaff = useCallback(async () => {
    setStaff(activeOnly(await getStaff()))
  }, [])

  const fetchShiftsForRange = useCallback(
    async (from, to) => fetchShiftsByWeekKeys(weekStartsOverlappingRange(from, to)),
    [fetchShiftsByWeekKeys],
  )

  async function handleSaveVacation(payload) {
    await withBusy(async () => {
      if (!vacationLocation(locations)) {
        throw new Error(
          'No está cargado el local Vacaciones. Ejecutá la migración MIGRA_VACACIONES_LOCATION.sql',
        )
      }
      const locationId = payload.locationId || VACATION_LOCATION_ID
      const startTime = normalizeTime(payload.startTime)
      const endTime = normalizeTime(payload.endTime)
      const replaceIds = new Set(payload.replaceConflictIds || [])
      const workDates = eachDateKey(payload.dateFrom, payload.dateTo)
      const pool = await fetchShiftsForRange(payload.dateFrom, payload.dateTo)

      for (const workDate of workDates) {
        for (const userId of payload.userIds) {
          const overlapping = pool.filter(
            (s) =>
              s.userId === userId &&
              String(s.workDate) === workDate &&
              timesOverlap(startTime, endTime, s.startTime, s.endTime),
          )
          for (const existing of overlapping) {
            if (replaceIds.has(existing.id)) await deleteShift(existing.id)
          }
        }
      }

      for (const workDate of workDates) {
        for (const userId of payload.userIds) {
          await createShift({
            userId,
            locationId,
            workDate,
            startTime,
            endTime,
          })
        }
      }

      await refreshAfterShiftChange()
      setVacationOpen(false)
    })
  }

  async function handleRemoveAbsenceShifts(shiftIds) {
    if (!shiftIds?.length) return
    await withBusy(async () => {
      for (const id of shiftIds) await deleteShift(id)
      await refreshAfterShiftChange()
    })
  }

  async function openAbsenceRemove(shift) {
    const day = String(shift.workDate).slice(0, 10)
    let pool = monthShifts
    try {
      const from = toDateKey(addDays(parseDateKey(day), -14))
      const to = toDateKey(addDays(parseDateKey(day), 14))
      pool = await fetchShiftsForRange(from, to)
    } catch {
      /* usa monthShifts */
    }
    const range = findContiguousAbsenceRange(
      pool,
      shift.userId,
      day,
      shift.locationId,
    )
    if (!range) return
    const member = staff.find((p) => p.id === shift.userId)
    setAbsenceRemove({
      kind: shift.locationId === FRANCO_LOCATION_ID ? 'franco' : 'vacation',
      staffName: member?.name || shift.userName || 'Persona',
      workDate: day,
      dateFrom: range.dateFrom,
      dateTo: range.dateTo,
      shiftIds: range.shiftIds,
      dayShiftId: shift.id,
    })
  }

  async function handleSaveShift(payload) {
    await withBusy(async () => {
      const francoWeekdays = payload.francoWeekdays || []
      const needsFranco =
        payload.locationId === FRANCO_LOCATION_ID || francoWeekdays.length > 0
      if (needsFranco && !francoLocation(locations)) {
        throw new Error(
          'No está cargado el local Franco. Ejecutá la migración MIGRA_FRANCO.sql',
        )
      }
      const startTime = normalizeTime(payload.startTime)
      const endTime = normalizeTime(payload.endTime)
      const dayBounds = absenceDayBounds()
      const francoStart = normalizeTime(
        payload.francoStartTime || dayBounds.startTime,
      )
      const francoEnd = normalizeTime(
        payload.francoEndTime || dayBounds.endTime,
      )
      const pool = view === 'month' ? monthShifts : shifts
      const replaceIds = new Set(payload.replaceConflictIds || [])
      const userIds =
        payload.mode === 'edit'
          ? [payload.userId]
          : payload.userIds || []

      let workDates
      let francoDates = []
      if (payload.rangeAssign) {
        ;({ workDates, francoDates } = splitRangeAssignDates(
          payload.dateFrom,
          payload.dateTo,
          {
            includeSundays: !!payload.includeSundays,
            francoWeekdays,
          },
        ))
      } else {
        workDates = [
          payload.workDate || workDateFor(weekStart, payload.dayIndex),
        ]
      }

      async function clearOverlaps(dates, fromTime, toTime) {
        for (const workDate of dates) {
          for (const userId of userIds) {
            const overlapping = pool.filter(
              (s) =>
                s.id !== payload.shiftId &&
                s.userId === userId &&
                String(s.workDate) === workDate &&
                timesOverlap(fromTime, toTime, s.startTime, s.endTime),
            )
            for (const existing of overlapping) {
              if (replaceIds.has(existing.id)) await deleteShift(existing.id)
            }
          }
        }
      }

      await clearOverlaps(workDates, startTime, endTime)
      if (francoDates.length) {
        await clearOverlaps(francoDates, francoStart, francoEnd)
      }

      if (payload.mode === 'edit') {
        await updateShift(payload.shiftId, {
          locationId: payload.locationId,
          workDate: workDates[0],
          startTime,
          endTime,
        })
      } else {
        for (const workDate of workDates) {
          for (const userId of userIds) {
            await createShift({
              userId,
              locationId: payload.locationId,
              workDate,
              startTime,
              endTime,
            })
          }
        }
        for (const workDate of francoDates) {
          for (const userId of userIds) {
            await createShift({
              userId,
              locationId: FRANCO_LOCATION_ID,
              workDate,
              startTime: francoStart,
              endTime: francoEnd,
            })
          }
        }
      }
      await refreshAfterShiftChange()
      setShiftModal(null)
      const landedFranco =
        payload.locationId === FRANCO_LOCATION_ID ||
        (payload.francoWeekdays && payload.francoWeekdays.length > 0)
      if (landedFranco && view !== 'month') {
        const focusKey = payload.rangeAssign
          ? payload.dateFrom
          : payload.workDate || workDates[0]
        focusMonthOnDate(focusKey)
      }
    })
  }

  async function handleDeleteShifts(shiftIds) {
    const ids = (shiftIds || []).filter(Boolean)
    if (!ids.length) return
    await withBusy(async () => {
      for (const id of ids) await deleteShift(id)
      await refreshAfterShiftChange()
      setShiftModal(null)
    })
  }

  function openAdd({ locationId, startTime, endTime }) {
    setShiftModal({
      mode: 'add',
      locationId,
      dayIndex: selectedDay,
      workDate: workDateFor(weekStart, selectedDay),
      startTime,
      endTime,
      preselect: [],
    })
  }

  function openVidrieraModal(dateKey) {
    setVidrieraModalDate(
      dateKey ||
        (view === 'week'
          ? workDateFor(weekStart, selectedDay)
          : toDateKey(new Date())),
    )
    setVidrieraOpen(true)
  }

  function closeVidrieraModal() {
    setVidrieraOpen(false)
    setVidrieraModalDate(null)
  }

  async function handleSaveVidriera({ workDate, locationIds }) {
    if (!canWriteShifts || !workDate) return
    const want = new Set(locationIds || [])
    await withBusy(async () => {
      const current = await getVidrieras(workDate, workDate)
      for (const row of current) {
        if (!want.has(row.locationId)) {
          await deleteVidriera(row.locationId, workDate)
        }
      }
      const have = new Set(current.map((v) => v.locationId))
      for (const id of want) {
        if (!have.has(id)) {
          await putVidriera({ locationId: id, workDate })
        }
      }
      const weekTo = toDateKey(addDays(weekStart, 6))
      const { min, max } = monthBounds(monthDate)
      const [weekV, monthV] = await Promise.all([
        getVidrieras(weekKey, weekTo),
        getVidrieras(min, max),
      ])
      setVidrieras(weekV)
      setMonthVidrieras(monthV)
      closeVidrieraModal()
    })
  }

  function openAddMonth({ userId, workDate }) {
    const work = parseDateKey(workDate)
    setShiftModal({
      mode: 'add',
      pickLocation: true,
      userLocked: !!userId,
      locationId: workLocations(locations)[0]?.id || '',
      workDate,
      dayIndex: (work.getDay() + 6) % 7,
      startTime: '09:00',
      endTime: '17:00',
      preselect: userId ? [userId] : [],
    })
  }

  function openRangeAssignMonth(member) {
    const bounds = monthBounds(monthDate)
    const today = toDateKey(new Date())
    const startKey =
      today >= bounds.min && today <= bounds.max ? today : bounds.min
    setShiftModal({
      mode: 'add',
      rangeAssign: true,
      userLocked: true,
      locationId: workLocations(locations)[0]?.id || '',
      workDate: startKey,
      dateFrom: startKey,
      dateTo: startKey,
      minDate: bounds.min,
      maxDate: bounds.max,
      startTime: '09:00',
      endTime: '17:00',
      preselect: member?.id ? [member.id] : [],
    })
  }

  function openEdit(s) {
    if (isAbsenceLocation(s.locationId)) return
    const work = parseDateKey(s.workDate)
    setWeekOffset(weekOffsetForDate(work))
    setSelectedDay(Number(s.dayIndex))
    setShiftModal({
      mode: 'edit',
      shiftId: s.id,
      userId: s.userId,
      locationId: s.locationId,
      dayIndex: Number(s.dayIndex),
      workDate: s.workDate,
      startTime: normalizeTime(s.startTime),
      endTime: normalizeTime(s.endTime),
    })
  }

  const navLabel =
    view === 'month' ? fmtMonthLabel(monthDate) : fmtWeekRange(weekStart)

  const modalShifts = view === 'month' ? monthShifts : shifts

  if (booting) {
    return (
      <div className="planner-root">
        <p style={{ color: 'var(--ink-soft)', fontSize: 13 }}>Cargando…</p>
      </div>
    )
  }

  return (
    <div className="planner-root">
      <div className="topbar">
        <button
          type="button"
          className="menu-toggle"
          aria-label="Abrir menú"
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen(true)}
        >
          <span />
          <span />
          <span />
        </button>
        {view === 'tasks' ? (
          <h1 className="task-view-title">Tablero de tareas</h1>
        ) : (
          <WeekNav
            label={navLabel}
            onPrev={() =>
              view === 'month'
                ? setMonthOffset((o) => o - 1)
                : setWeekOffset((o) => o - 1)
            }
            onNext={() =>
              view === 'month'
                ? setMonthOffset((o) => o + 1)
                : setWeekOffset((o) => o + 1)
            }
            onToday={goToday}
          />
        )}
      </div>

      {error && (
        <div className="m-warn" style={{ marginBottom: 12 }}>
          {error}
        </div>
      )}

      {view === 'week' && (
        <DayTabs
          weekStart={weekStart}
          selectedDay={selectedDay}
          onSelect={setSelectedDay}
          holidaysByDate={holidaysByDate}
        />
      )}

      {view === 'month' && (
        <div className="toolbar">
          <div className="legend" role="group" aria-label="Filtrar por local">
            {monthLocFilter.length > 0 && (
              <button
                type="button"
                className="chip chip-clear"
                onClick={() => setMonthLocFilter([])}
              >
                Todos
              </button>
            )}
            {locations.map((l) => {
              const on = monthLocFilter.includes(l.id)
              const dim = monthLocFilter.length > 0 && !on
              return (
                <button
                  type="button"
                  className={`chip${on ? ' is-on' : ''}${dim ? ' is-dim' : ''}`}
                  key={l.id}
                  aria-pressed={on}
                  title={
                    on
                      ? `Quitar filtro ${l.name}`
                      : `Mostrar solo ${l.name}`
                  }
                  onClick={() => toggleMonthLocFilter(l.id)}
                >
                  <span className="dot" style={{ background: l.color }} />
                  {l.name}
                </button>
              )
            })}
          </div>
        </div>
      )}

      {loading || (view === 'tasks' && taskLoading) ? (
        <p style={{ color: 'var(--ink-soft)', fontSize: 13 }}>Cargando…</p>
      ) : view === 'tasks' ? (
        <TaskBoard
          tasks={taskBoard}
          assignees={taskAssignees}
          currentUserId={user?.id || null}
          canWrite={canWriteTasks}
          canManage={canManageTasks}
          busy={busy}
          onAssign={handleAssignTask}
          onUnassign={handleUnassignTask}
          onMove={handleMoveTask}
          onRetire={handleRetireTask}
        />
      ) : view === 'month' ? (
        <MonthGantt
          monthDate={monthDate}
          staff={visibleMonthStaff}
          shifts={visibleMonthShifts}
          vidrieras={visibleMonthVidrieras}
          lunchHours={lunchHours}
          holidaysByDate={holidaysByDate}
          focusNonce={monthFocusNonce}
          focusDateKey={monthFocusDateKey}
          canAdd={canWriteShifts}
          canEdit={canWriteShifts}
          canEditVacations={canWriteVacations}
          onEdit={openEdit}
          onAdd={openAddMonth}
          onRangeAssign={openRangeAssignMonth}
          onAbsence={openAbsenceRemove}
        />
      ) : (
        <ScheduleGrid
          locations={workLocations(locations)}
          staff={staff}
          shifts={shifts}
          selectedDay={selectedDay}
          isToday={isTodayCol}
          hourH={hourH}
          onHourHChange={setHourH}
          canAdd={canWriteShifts}
          canEdit={canWriteShifts}
          vidrieraLocationIds={vidrieraLocationIds}
          canWriteVidriera={canWriteShifts}
          onVidrieraClick={() =>
            openVidrieraModal(workDateFor(weekStart, selectedDay))
          }
          onAdd={openAdd}
          onEdit={openEdit}
        />
      )}

      {view === 'week' && (
        <SummaryBar
          staff={staff}
          shifts={shifts}
          weekStart={weekStart}
          lunchHours={lunchHours}
        />
      )}

      {shiftModal && <ShiftModal
        open
        ctx={shiftModal}
        staff={staff}
        locations={locations}
        shifts={modalShifts}
        weekStart={weekStart}
        busy={busy}
        canWriteFrancos={canWriteVacations}
        showAddSame={view === 'week'}
        onClose={() => setShiftModal(null)}
        onSave={handleSaveShift}
        onDelete={handleDeleteShifts}
        onAddSame={(start, end) => {
          const m = shiftModal
          setShiftModal(null)
          openAdd({
            locationId: m?.locationId,
            startTime: start,
            endTime: end,
          })
        }}
      />}

      {usersOpen && <UsersModal
        open
        onClose={() => setUsersOpen(false)}
        onChanged={async () => {
          await reloadStaff()
          await refreshAfterShiftChange()
        }}
      />}

      {taskAdminOpen && <TaskAdminModal
        open
        locations={locations}
        onClose={() => setTaskAdminOpen(false)}
        onChanged={reloadTaskBoard}
      />}

      {taskRetentionOpen && <TaskRetentionModal
        open
        onClose={() => setTaskRetentionOpen(false)}
        onChanged={reloadTaskBoard}
      />}

      {auditOpen && <AuditModal open onClose={() => setAuditOpen(false)} />}

      {lunchOpen && <LunchModal
        open
        lunchHours={lunchHours}
        onChange={handleLunchHoursChange}
        onClose={() => setLunchOpen(false)}
      />}

      {vacationOpen && <VacationModal
        open
        staff={staff}
        locations={locations}
        busy={busy}
        canWrite={canWriteVacations}
        fetchShiftsForRange={fetchShiftsForRange}
        onClose={() => setVacationOpen(false)}
        onSave={handleSaveVacation}
      />}

      {vidrieraOpen && <VidrieraModal
        open
        locations={locations}
        busy={busy}
        initialDate={
          vidrieraModalDate ||
          (view === 'week'
            ? workDateFor(weekStart, selectedDay)
            : toDateKey(new Date()))
        }
        existing={view === 'month' ? monthVidrieras : vidrieras}
        existingLabel={view === 'month' ? 'este mes' : 'esta semana'}
        loadVidrieras={getVidrieras}
        onClose={closeVidrieraModal}
        onSave={handleSaveVidriera}
      />}

      {absenceRemove && <VacationRemoveModal
        open
        kind={absenceRemove?.kind || 'vacation'}
        staffName={absenceRemove?.staffName}
        workDate={absenceRemove?.workDate}
        dateFrom={absenceRemove?.dateFrom}
        dateTo={absenceRemove?.dateTo}
        busy={busy}
        onClose={() => setAbsenceRemove(null)}
        onRemoveDay={async () => {
          const id = absenceRemove?.dayShiftId
          setAbsenceRemove(null)
          if (id) await handleRemoveAbsenceShifts([id])
        }}
        onRemoveRange={async () => {
          const ids = absenceRemove?.shiftIds || []
          setAbsenceRemove(null)
          await handleRemoveAbsenceShifts(ids)
        }}
      />}

      <AppMenu
        open={menuOpen}
        onClose={() => setMenuOpen(false)}
        view={view}
        onViewChange={setView}
        periodLabel={navLabel}
        onPrevPeriod={() =>
          view === 'month'
            ? setMonthOffset((o) => o - 1)
            : setWeekOffset((o) => o - 1)
        }
        onNextPeriod={() =>
          view === 'month'
            ? setMonthOffset((o) => o + 1)
            : setWeekOffset((o) => o + 1)
        }
        onToday={goToday}
        showVacations={canWriteVacations}
        showUsers={canManageUsers}
        showLunch={canManageLunch}
        showVidriera={canWriteShifts}
        showTasksAdmin={canManageTasks}
        showTaskRetention={canManageTaskRetention}
        showAudit={canReadAudit}
        userLabel={user ? user.displayName || user.username : null}
        loggedIn={!!user}
        onVacations={() => setVacationOpen(true)}
        onUsers={() => setUsersOpen(true)}
        onLunch={() => setLunchOpen(true)}
        onVidriera={() => openVidrieraModal()}
        onTasksAdmin={() => setTaskAdminOpen(true)}
        onTaskRetention={() => setTaskRetentionOpen(true)}
        onAudit={() => setAuditOpen(true)}
        onLogin={() => setLoginOpen(true)}
        onLogout={logout}
        onChangePassword={() => setChangePasswordOpen(true)}
        version={appVersionLabel()}
      />

      {loginOpen && (
        <LoginScreen open onClose={() => setLoginOpen(false)} />
      )}
      {changePasswordOpen && <ChangePasswordModal
        open
        required={mustChangePassword}
        onChanged={refreshMe}
        onClose={() => setChangePasswordOpen(false)}
      />}
    </div>
  )
}

export default App
