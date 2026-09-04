import { useEffect, useState } from 'react'
import { getTaskHistory } from '../api/client'
import { useAuth } from '../auth/AuthContext'

const STATUS_LABELS = {
  PENDING: 'Pendientes',
  IN_PROGRESS: 'En proceso',
  BLOCKED: 'Bloqueada',
  DONE: 'Terminada',
  VERIFIED: 'Verificada',
}

function fmtParts(iso) {
  if (!iso) return { day: '', time: '' }
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

function who(entry) {
  return entry.actorName || 'Alguien'
}

function statusLabel(status) {
  return STATUS_LABELS[status] || status || '—'
}

function eventText(entry) {
  const actor = who(entry)
  if (entry.action === 'PUBLISH') return `${actor} la pasó a Pendientes`
  if (entry.action === 'ASSIGN') {
    const assigned = entry.toAssigneeName || 'alguien'
    if (entry.actorUserId && entry.actorUserId === entry.toAssigneeUserId) {
      return `${assigned} se la asignó`
    }
    return `${actor} se la asignó a ${assigned}`
  }
  if (entry.action === 'UNASSIGN') {
    return `${actor} la desasignó y la volvió a Pendientes`
  }
  if (entry.action === 'MOVE') {
    const from = statusLabel(entry.fromStatus)
    const to = statusLabel(entry.toStatus)
    if (entry.toStatus === 'BLOCKED' && entry.blockReason) {
      return `${actor} la movió de ${from} a ${to}: ${entry.blockReason}`
    }
    return `${actor} la movió de ${from} a ${to}`
  }
  if (entry.action === 'RETIRE') {
    return `${actor} la sacó del tablero`
  }
  return `${actor} · ${entry.action}`
}

function Fact({ label, entry, tone }) {
  const parts = entry ? fmtParts(entry.occurredAt) : null
  return (
    <div className={`task-history-fact${tone ? ` is-${tone}` : ''}`}>
      <div className="task-history-fact-label">{label}</div>
      {entry ? (
        <>
          <div className="task-history-fact-when">
            {parts.day} · {parts.time}
          </div>
          <div className="task-history-fact-text">{eventText(entry)}</div>
        </>
      ) : (
        <div className="task-history-fact-empty">&nbsp;</div>
      )}
    </div>
  )
}

export default function TaskHistoryModal({ task, onClose }) {
  const { can } = useAuth()
  const canSeeAllMoves = can('tasks:history')
  const [data, setData] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!task) return
    let cancelled = false
    setBusy(true)
    setError('')
    getTaskHistory(task.id)
      .then((rows) => {
        if (!cancelled) setData(rows)
      })
      .catch((err) => {
        if (!cancelled) setError(err.message)
      })
      .finally(() => {
        if (!cancelled) setBusy(false)
      })
    return () => {
      cancelled = true
    }
  }, [task])

  if (!task) return null

  const movements = data?.movements || []

  return (
    <div
      className="overlay open panel-overlay"
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <div className="modal panel-modal task-history-modal">
        <div className="panel-head">
          <div>
            <h3>{task.title}</h3>
            <div className="m-sub">
              {statusLabel(task.status)}
              {task.assigneeName ? ` · ${task.assigneeName}` : ' · Sin asignar'}
            </div>
          </div>
          <button
            type="button"
            className="panel-close"
            onClick={onClose}
            aria-label="Cerrar"
          >
            ✕
          </button>
        </div>

        {task.description && (
          <p className="task-history-desc">{task.description}</p>
        )}
        {error && <div className="m-warn">{error}</div>}

        <div className="task-history-facts">
          <Fact label="Pendientes" entry={data?.pending} />
          <Fact label="Asignación" entry={data?.assigned} />
          <Fact label="Bloqueada" entry={data?.blocked} tone="blocked" />
          <Fact label="Terminada" entry={data?.done} />
          <Fact label="Verificada" entry={data?.verified} tone="verified" />
        </div>

        {canSeeAllMoves && (
          <>
            <div className="panel-toolbar">
              <div className="panel-count">
                {busy
                  ? 'Cargando…'
                  : `${movements.length} movimiento${movements.length === 1 ? '' : 's'}`}
              </div>
            </div>

            <div className="panel-list">
              {!busy && !movements.length && (
                <div className="panel-empty">
                  <div className="panel-empty-title">Sin movimientos</div>
                </div>
              )}
              {!!movements.length && (
                <ul>
                  {movements.map((entry) => {
                    const parts = fmtParts(entry.occurredAt)
                    return (
                      <li className="audit-item action-update" key={entry.id}>
                        <span className="audit-rail" aria-hidden />
                        <div className="audit-when-col">
                          <span className="audit-day">{parts.day}</span>
                          <span className="audit-time">{parts.time}</span>
                        </div>
                        <div className="audit-body">
                          <div className="audit-summary">{eventText(entry)}</div>
                        </div>
                      </li>
                    )
                  })}
                </ul>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  )
}
