import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { initials } from '../lib/palette'
import TaskHistoryModal from './TaskHistoryModal'

const COLUMNS = [
  { id: 'PENDING', label: 'Pendientes' },
  { id: 'IN_PROGRESS', label: 'En proceso' },
  { id: 'BLOCKED', label: 'Bloqueada' },
  { id: 'DONE', label: 'Terminada' },
  { id: 'VERIFIED', label: 'Verificada' },
]

const PERSONAL_DESTINATIONS = new Set(['IN_PROGRESS', 'BLOCKED', 'DONE'])

export default function TaskBoard({
  tasks = [],
  assignees = [],
  currentUserId = null,
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
  const [menu, setMenu] = useState(null)
  const [historyTask, setHistoryTask] = useState(null)
  const skipClickRef = useRef(false)
  const menuRef = useRef(null)

  const byStatus = useMemo(() => {
    const map = new Map(COLUMNS.map((column) => [column.id, []]))
    for (const task of tasks) map.get(task.status)?.push(task)
    return map
  }, [tasks])

  function canMoveTask(task) {
    if (!task.assigneeUserId) return false
    return canManage || (canWrite && task.assigneeUserId === currentUserId)
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

  function hasContextMenu(task) {
    if (busy) return false
    if (canManage) return true
    return (
      canWrite &&
      !!currentUserId &&
      task.status === 'PENDING' &&
      !task.assigneeUserId
    )
  }

  function openMenu(event, task) {
    if (!hasContextMenu(task)) return
    event.preventDefault()
    event.stopPropagation()
    setDraggingId(null)
    setMenu({ x: event.clientX, y: event.clientY, taskId: task.id })
  }

  useLayoutEffect(() => {
    const el = menuRef.current
    if (!el || !menu) return
    const rect = el.getBoundingClientRect()
    const dx = Math.min(0, window.innerWidth - 8 - rect.right)
    const dy = Math.min(0, window.innerHeight - 8 - rect.bottom)
    if (dx || dy) {
      el.style.left = `${Math.max(8, rect.left + dx)}px`
      el.style.top = `${Math.max(8, rect.top + dy)}px`
    }
  }, [menu])

  useEffect(() => {
    if (!menu) return
    function close() {
      setMenu(null)
    }
    function onKey(event) {
      if (event.key === 'Escape') close()
    }
    function onPointer(event) {
      if (menuRef.current?.contains(event.target)) return
      close()
    }
    window.addEventListener('keydown', onKey)
    window.addEventListener('pointerdown', onPointer)
    window.addEventListener('scroll', close, true)
    window.addEventListener('resize', close)
    return () => {
      window.removeEventListener('keydown', onKey)
      window.removeEventListener('pointerdown', onPointer)
      window.removeEventListener('scroll', close, true)
      window.removeEventListener('resize', close)
    }
  }, [menu])

  const menuTask = menu ? tasks.find((task) => task.id === menu.taskId) : null
  const canSelfAssign =
    !!menuTask &&
    canWrite &&
    !!currentUserId &&
    menuTask.status === 'PENDING' &&
    !menuTask.assigneeUserId

  return (
    <>
      <div className="task-board" aria-label="Tareas de Asistentes">
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
                  const own = task.assigneeUserId === currentUserId
                  const movable = canMoveTask(task)
                  const contextual = hasContextMenu(task)
                  return (
                    <article
                      key={task.id}
                      className={`task-card${own ? ' is-own' : ''}${
                        movable ? ' is-movable' : ''
                      }${contextual ? ' has-menu' : ''}${
                        draggingId === task.id ? ' is-dragging' : ''
                      }`}
                      draggable={movable && !busy}
                      title={
                        contextual
                          ? canManage
                            ? 'Clic para ver el detalle. Clic derecho para asignar o sacar del tablero'
                            : 'Clic para ver el detalle. Clic derecho para asignar'
                          : 'Clic para ver el detalle'
                      }
                      onContextMenu={(event) => openMenu(event, task)}
                      onClick={() => {
                        if (skipClickRef.current) {
                          skipClickRef.current = false
                          return
                        }
                        setHistoryTask(task)
                      }}
                      onDragStart={(event) => {
                        skipClickRef.current = true
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
                        {task.assigneeUserId ? (
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
                        {task.locationName && (
                          <span
                            className="task-card-location"
                            title={task.locationName}
                            style={{
                              '--loc': task.locationColor || '#5B6675',
                            }}
                          >
                            {task.locationName}
                          </span>
                        )}
                      </div>

                      {column.id === 'PENDING' &&
                        !task.assigneeUserId &&
                        canWrite &&
                        currentUserId &&
                        !canManage && (
                          <button
                            type="button"
                            className="task-action"
                            disabled={busy}
                            onClick={(event) => {
                              event.stopPropagation()
                              void perform(() =>
                                onAssign?.(task.id, currentUserId),
                              )
                            }}
                          >
                            Asignarme
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

      {menuTask && (
        <div
          ref={menuRef}
          className="task-context-menu"
          role="menu"
          style={{ left: menu.x, top: menu.y }}
        >
          {(canManage || canSelfAssign) && (
            <div className="task-context-group">
              <div className="task-context-label">Asignar</div>
              {canSelfAssign && (
                <button
                  type="button"
                  role="menuitem"
                  disabled={busy}
                  onClick={() => {
                    setMenu(null)
                    void perform(() => onAssign?.(menuTask.id, currentUserId))
                  }}
                >
                  Asignarme
                </button>
              )}
              {canManage && (
                <>
                  <button
                    type="button"
                    role="menuitem"
                    className={!menuTask.assigneeUserId ? 'is-current' : ''}
                    disabled={busy || !menuTask.assigneeUserId}
                    onClick={() => {
                      setMenu(null)
                      void perform(() => onUnassign?.(menuTask.id))
                    }}
                  >
                    Sin asignar
                  </button>
                  {assignees.map((assignee) => (
                    <button
                      type="button"
                      role="menuitem"
                      key={assignee.id}
                      className={
                        menuTask.assigneeUserId === assignee.id ? 'is-current' : ''
                      }
                      disabled={busy || menuTask.assigneeUserId === assignee.id}
                      onClick={() => {
                        setMenu(null)
                        void perform(() => onAssign?.(menuTask.id, assignee.id))
                      }}
                    >
                      {assignee.displayName || assignee.username}
                    </button>
                  ))}
                </>
              )}
            </div>
          )}
          {canManage && (
            <div className="task-context-group">
              <button
                type="button"
                role="menuitem"
                className="is-danger"
                disabled={busy}
                onClick={() => {
                  setMenu(null)
                  void perform(() => onRetire?.(menuTask.id))
                }}
              >
                Sacar del tablero
              </button>
            </div>
          )}
        </div>
      )}

      {historyTask && (
        <TaskHistoryModal
          task={historyTask}
          onClose={() => setHistoryTask(null)}
        />
      )}

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
