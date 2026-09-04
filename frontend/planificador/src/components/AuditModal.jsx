import { useEffect, useState } from 'react'
import { getAudit, getAuditUsers } from '../api/client'

const ENTITY_LABELS = {
  shift: 'Turno',
  vacation: 'Vacaciones',
  franco: 'Franco',
  vidriera: 'Vidriera',
  task: 'Tarea',
  staff: 'Asistentes',
  user: 'Usuario',
  role: 'Rol',
  session: 'Sesión',
}

const ACTION_LABELS = {
  CREATE: 'Alta',
  UPDATE: 'Edición',
  DELETE: 'Baja',
  LOGIN: 'Ingreso',
  LOGOUT: 'Salida',
  LOGIN_FAIL: 'Fallido',
}

const ENTITY_NOUNS = {
  shift: 'turno',
  vacation: 'vacaciones',
  franco: 'franco',
  vidriera: 'vidriera',
  task: 'tarea',
  staff: 'personal',
  user: 'usuario',
  role: 'rol',
}

/** Quita "Alta turno: " / etc. de resúmenes viejos (ya están en badges). */
function displaySummary(summary, action, entityType) {
  if (!summary) return ''
  const actionLabel = ACTION_LABELS[action]
  const noun = ENTITY_NOUNS[entityType]
  if (!actionLabel || !noun) return summary
  const prefix = `${actionLabel} ${noun}: `
  if (summary.toLocaleLowerCase('es').startsWith(prefix.toLocaleLowerCase('es'))) {
    return summary.slice(prefix.length)
  }
  return summary
}

const DEFAULT_RANGE_DAYS = 30

function toDateKey(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function todayKey() {
  return toDateKey(new Date())
}

function daysAgoKey(n) {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return toDateKey(d)
}

function defaultRange() {
  return { from: daysAgoKey(DEFAULT_RANGE_DAYS), to: todayKey() }
}

function fmtParts(iso) {
  if (!iso) return { day: '—', time: '' }
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return { day: iso, time: '' }
  return {
    day: d.toLocaleDateString('es-AR', {
      weekday: 'short',
      day: '2-digit',
      month: 'short',
    }),
    time: d.toLocaleTimeString('es-AR', {
      hour: '2-digit',
      minute: '2-digit',
    }),
  }
}

function actionClass(action) {
  if (action === 'CREATE' || action === 'LOGIN') return 'create'
  if (action === 'DELETE' || action === 'LOGIN_FAIL') return 'delete'
  return 'update'
}

function userOptionLabel(u) {
  if (u.displayName && u.displayName !== u.username) {
    return `${u.displayName} (${u.username})`
  }
  return u.username
}

export default function AuditModal({ open, onClose }) {
  const [from, setFrom] = useState(() => defaultRange().from)
  const [to, setTo] = useState(() => defaultRange().to)
  const [username, setUsername] = useState('')
  const [entityType, setEntityType] = useState('')
  const [users, setUsers] = useState([])
  const [entries, setEntries] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    if (!open) return
    let cancelled = false
    const range = defaultRange()

    setError('')
    setLoaded(false)
    setFrom(range.from)
    setTo(range.to)
    setUsername('')
    setEntityType('')

    ;(async () => {
      try {
        const opts = await getAuditUsers()
        if (!cancelled) setUsers(opts || [])
      } catch {
        if (!cancelled) setUsers([])
      }
      if (!cancelled) {
        await load(
          { from: range.from, to: range.to, username: '', entityType: '' },
          () => cancelled,
        )
      }
    })()

    return () => {
      cancelled = true
    }
  }, [open])

  async function load(filters, isCancelled = () => false) {
    setBusy(true)
    setError('')
    try {
      const rows = await getAudit({
        from: filters.from || undefined,
        to: filters.to || undefined,
        username: filters.username || undefined,
        entityType: filters.entityType || undefined,
        limit: 200,
      })
      if (isCancelled()) return
      setEntries(rows || [])
    } catch (e) {
      if (isCancelled()) return
      setError(e.message)
      setEntries([])
    } finally {
      if (!isCancelled()) {
        setBusy(false)
        setLoaded(true)
      }
    }
  }

  if (!open) return null

  return (
    <div
      className="overlay open panel-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div
        className="modal panel-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="audit-title"
      >
        <div className="panel-head">
          <h3 id="audit-title">Bitácora</h3>
          <button
            type="button"
            className="panel-close"
            onClick={onClose}
            aria-label="Cerrar"
          >
            ✕
          </button>
        </div>

        <form
          className="panel-form audit-filters"
          onSubmit={(e) => {
            e.preventDefault()
            load({ from, to, username, entityType })
          }}
        >
          <div className="field">
            <label htmlFor="audit-from">Desde</label>
            <input
              id="audit-from"
              type="date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="audit-to">Hasta</label>
            <input
              id="audit-to"
              type="date"
              value={to}
              onChange={(e) => setTo(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="audit-user">Usuario</label>
            <select
              id="audit-user"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            >
              <option value="">Todos</option>
              {users.map((u) => (
                <option key={u.username} value={u.username}>
                  {userOptionLabel(u)}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="audit-type">Tipo</label>
            <select
              id="audit-type"
              value={entityType}
              onChange={(e) => setEntityType(e.target.value)}
            >
              <option value="">Todos</option>
              {Object.entries(ENTITY_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>
          <div className="panel-form-actions">
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? 'Buscando…' : 'Buscar'}
            </button>
          </div>
        </form>

        {error && <div className="m-warn">{error}</div>}

        <div className="panel-toolbar">
          <span className="panel-count">
            {!loaded || busy
              ? 'Cargando…'
              : entries.length === 0
                ? 'Sin resultados'
                : `${entries.length} evento${entries.length === 1 ? '' : 's'}`}
          </span>
        </div>

        <div className={`panel-list${busy ? ' is-busy' : ''}`}>
          {!busy && loaded && entries.length === 0 ? (
            <div className="panel-empty">
              <div className="panel-empty-title">Nada en este rango</div>
              <div>Probá ampliar las fechas o quitar filtros.</div>
            </div>
          ) : (
            <ul>
              {entries.map((row) => {
                const when = fmtParts(row.occurredAt)
                const kind = actionClass(row.action)
                return (
                  <li key={row.id} className={`audit-item action-${kind}`}>
                    <div className="audit-rail" aria-hidden />
                    <div className="audit-when-col">
                      <span className="audit-day">{when.day}</span>
                      <span className="audit-time">{when.time}</span>
                    </div>
                    <div className="audit-body">
                      <div className="audit-meta">
                        <span className={`audit-badge action-${kind}`}>
                          {ACTION_LABELS[row.action] || row.action}
                        </span>
                        <span className="audit-badge soft">
                          {ENTITY_LABELS[row.entityType] || row.entityType}
                        </span>
                        <span className="audit-user">{row.username}</span>
                      </div>
                      <div className="audit-summary">
                        {displaySummary(row.summary, row.action, row.entityType)}
                      </div>
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}
