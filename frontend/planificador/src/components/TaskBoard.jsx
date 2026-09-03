import { useMemo, useState } from 'react'

const COLUMNS = [
  { id: 'PENDING', label: 'Pendientes' },
  { id: 'IN_PROGRESS', label: 'En proceso' },
  { id: 'BLOCKED', label: 'Bloqueada' },
  { id: 'DONE', label: 'Terminada' },
  { id: 'VERIFIED', label: 'Verificada' },
]

const PERSONAL_DESTINATIONS = new Set(['IN_PROGRESS', 'BLOCKED', 'DONE'])

function initials(name = '') {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase()
}

export default function TaskBoard({
  tasks = [],
  people = [],
  currentPersonId = null,
  canWrite = false,
  canManage = false,
  busy = false,
  onAssign,
  onUnassign,
  onMove,
  onRetire,
}) {
  const [draggingId, setDraggingId] = useState(null)
  const [blockRequest, setBlockRequest] = useState(null)
  const [blockReason, setBlockReason] = useState('')

  const byStatus = useMemo(() => {
    const map = new Map(COLUMNS.map((column) => [column.id, []]))
    for (const task of tasks) map.get(task.status)?.push(task)
    return map
  }, [tasks])

  function canMoveTask(task) {
    if (!task.assigneePersonId) return false
    return canManage || (canWrite && task.assigneePersonId === currentPersonId)
  }

  function canDrop(task, status) {
    if (!canMoveTask(task)) return false
    if (canManage) return true
    return PERSONAL_DESTINATIONS.has(status)
  }

  function requestMove(task, status) {
    if (!canDrop(task, status) || status === task.status) return
    if (status === 'BLOCKED') {
      setBlockReason('')
      setBlockRequest({ task, status })
      return
    }
    void perform(() => onMove?.(task.id, status, null))
  }

  async function perform(action) {
    try {
      await action()
      return true
    } catch {
      return false
    }
  }

  return (
    <>
      <div className="task-board" aria-label="Tablero de tareas">
        {COLUMNS.map((column) => {
          const columnTasks = byStatus.get(column.id) || []
          const dragged = tasks.find((task) => task.id === draggingId)
          const acceptsDrop = dragged && canDrop(dragged, column.id)
          return (
            <section
              key={column.id}
              className={`task-column task-column-${column.id.toLowerCase()}${
                acceptsDrop ? ' can-drop' : ''
              }`}
              onDragOver={(event) => {
                if (acceptsDrop) event.preventDefault()
              }}
              onDrop={(event) => {
                event.preventDefault()
                const id = event.dataTransfer.getData('text/task-id') || draggingId
                const task = tasks.find((item) => item.id === id)
                setDraggingId(null)
                if (task) requestMove(task, column.id)
              }}
            >
              <header className="task-column-head">
                <h2>{column.label}</h2>
                <span>{columnTasks.length}</span>
              </header>

              <div className="task-column-body">
                {!columnTasks.length && (
                  <div className="task-column-empty">Sin tareas</div>
                )}
                {columnTasks.map((task) => {
                  const own = task.assigneePersonId === currentPersonId
                  const movable = canMoveTask(task)
                  const moveOptions = COLUMNS.filter(
                    (target) =>
                      target.id !== task.status &&
                      (canManage || PERSONAL_DESTINATIONS.has(target.id)),
                  )
                  return (
                    <article
                      key={task.id}
                      className={`task-card${own ? ' is-own' : ''}${
                        movable ? ' is-movable' : ''
                      }`}
                      draggable={movable && !busy}
                      onDragStart={(event) => {
                        setDraggingId(task.id)
                        event.dataTransfer.effectAllowed = 'move'
                        event.dataTransfer.setData('text/task-id', task.id)
                      }}
                      onDragEnd={() => setDraggingId(null)}
                    >
                      <h3>{task.title}</h3>
                      {task.description && (
                        <p className="task-card-description">{task.description}</p>
                      )}
                      {task.status === 'BLOCKED' && task.blockReason && (
                        <div className="task-block-reason">
                          <strong>Motivo:</strong> {task.blockReason}
                        </div>
                      )}

                      <div className="task-card-assignee">
                        {task.assigneePersonId ? (
                          <>
                            <span
                              className="task-avatar"
                              style={{ background: task.assigneeColor || '#5B6675' }}
                            >
                              {initials(task.assigneeName)}
                            </span>
                            <span>{task.assigneeName}</span>
                          </>
                        ) : (
                          <span className="task-unassigned">Sin asignar</span>
                        )}
                      </div>

                      {column.id === 'PENDING' &&
                        !task.assigneePersonId &&
                        canWrite &&
                        currentPersonId &&
                        !canManage && (
                          <button
                            type="button"
                            className="task-action"
                            disabled={busy}
                            onClick={() =>
                              void perform(() =>
                                onAssign?.(task.id, currentPersonId),
                              )
                            }
                          >
                            Asignarme
                          </button>
                        )}

                      {canManage && (
                        <label className="task-inline-field">
                          <span>Asignada a</span>
                          <select
                            value={task.assigneePersonId || ''}
                            disabled={busy}
                            onChange={(event) => {
                              const personId = event.target.value
                              if (personId) {
                                void perform(() => onAssign?.(task.id, personId))
                              } else {
                                void perform(() => onUnassign?.(task.id))
                              }
                            }}
                          >
                            <option value="">Sin asignar / Pendientes</option>
                            {people.map((person) => (
                              <option key={person.id} value={person.id}>
                                {person.name}
                              </option>
                            ))}
                          </select>
                        </label>
                      )}

                      {movable && (
                        <label className="task-inline-field">
                          <span>Mover a</span>
                          <select
                            value=""
                            disabled={busy}
                            onChange={(event) => {
                              const status = event.target.value
                              if (status) requestMove(task, status)
                            }}
                          >
                            <option value="">Elegir estado…</option>
                            {moveOptions.map((target) => (
                              <option key={target.id} value={target.id}>
                                {target.label}
                              </option>
                            ))}
                          </select>
                        </label>
                      )}

                      {canManage && (
                        <button
                          type="button"
                          className="task-retire"
                          disabled={busy}
                          onClick={() =>
                            void perform(() => onRetire?.(task.id))
                          }
                        >
                          Sacar del tablero
                        </button>
                      )}
                    </article>
                  )
                })}
              </div>
            </section>
          )
        })}
      </div>

      {blockRequest && (
        <div
          className="overlay open"
          onClick={(event) => {
            if (event.target === event.currentTarget && !busy) {
              setBlockRequest(null)
            }
          }}
        >
          <div className="modal task-block-modal">
            <h3>Bloquear tarea</h3>
            <div className="m-sub">{blockRequest.task.title}</div>
            <div className="field">
              <label htmlFor="task-block-reason">Motivo</label>
              <textarea
                id="task-block-reason"
                value={blockReason}
                maxLength={2000}
                disabled={busy}
                autoFocus
                onChange={(event) => setBlockReason(event.target.value)}
              />
            </div>
            <div className="modal-actions">
              <button
                type="button"
                className="btn btn-ghost"
                disabled={busy}
                onClick={() => setBlockRequest(null)}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="btn btn-primary"
                disabled={busy || !blockReason.trim()}
                onClick={async () => {
                  const ok = await perform(() =>
                    onMove?.(
                      blockRequest.task.id,
                      blockRequest.status,
                      blockReason.trim(),
                    ),
                  )
                  if (ok) setBlockRequest(null)
                }}
              >
                Bloquear
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
