import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { initials, paletteFor } from '../lib/palette'
import TaskHistoryModal from './TaskHistoryModal'

const COLUMNS = [
  { id: 'PENDING', label: 'Pendientes' },
  { id: 'IN_PROGRESS', label: 'En proceso' },
  { id: 'BLOCKED', label: 'Bloqueada' },
  { id: 'DONE', label: 'Terminada' },
  { id: 'VERIFIED', label: 'Verificada' },
]

const PERSONAL_DESTINATIONS = new Set(['IN_PROGRESS', 'BLOCKED', 'DONE'])
const UNASSIGNED = '__none__'
const NO_LOCATION = '__none__'

function assigneeLabel(person) {
  return person.displayName || person.name || person.username || 'Sin nombre'
}

export default function TaskBoard({
  tasks = [],
  assignees = [],
  locations = [],
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
  const [assigneeFilter, setAssigneeFilter] = useState('all')
  const [locationFilter, setLocationFilter] = useState([])
  const [query, setQuery] = useState('')
  const skipClickRef = useRef(false)
  const menuRef = useRef(null)
  const filtersRef = useRef(null)
  const wrapRef = useRef(null)

  useLayoutEffect(() => {
    const filters = filtersRef.current
    const wrap = wrapRef.current
    if (!filters || !wrap) return
    function sync() {
      wrap.style.setProperty('--task-filters-h', `${filters.offsetHeight}px`)
    }
    sync()
    const observer = new ResizeObserver(sync)
    observer.observe(filters)
    return () => observer.disconnect()
  }, [])

  const people = useMemo(() => {
    const map = new Map()
    for (const person of assignees) {
      map.set(String(person.id), {
        id: person.id,
        name: assigneeLabel(person),
      })
    }
    for (const task of tasks) {
      if (!task.assigneeUserId) continue
      const id = String(task.assigneeUserId)
      if (!map.has(id)) {
        map.set(id, { id: task.assigneeUserId, name: task.assigneeName || 'Sin nombre' })
      }
    }
    return [...map.values()].sort((a, b) =>
      a.name.localeCompare(b.name, 'es', { sensitivity: 'base' }),
    )
  }, [assignees, tasks])

  const visibleTasks = useMemo(() => {
    const q = query.trim().toLowerCase()
    const locSet = locationFilter.length ? new Set(locationFilter) : null
    return tasks.filter((task) => {
      if (assigneeFilter === 'mine') {
        if (String(task.assigneeUserId || '') !== String(currentUserId || '')) {
          return false
        }
      } else if (assigneeFilter === UNASSIGNED) {
        if (task.assigneeUserId) return false
      } else if (assigneeFilter !== 'all') {
        if (String(task.assigneeUserId || '') !== String(assigneeFilter)) {
          return false
        }
      }
      if (locSet) {
        const loc = task.locationId || NO_LOCATION
        if (!locSet.has(loc)) return false
      }
      if (q && !String(task.title || '').toLowerCase().includes(q)) {
        return false
      }
      return true
    })
  }, [tasks, assigneeFilter, locationFilter, query, currentUserId])

  const byStatus = useMemo(() => {
    const map = new Map(COLUMNS.map((column) => [column.id, []]))
    for (const task of visibleTasks) map.get(task.status)?.push(task)
    return map
  }, [visibleTasks])

  function selectAssignee(next) {
    setAssigneeFilter((prev) => (prev === next ? 'all' : next))
  }

  function toggleLocation(id) {
    setLocationFilter((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id],
    )
  }

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
  const assignChoices = canSelfAssign
    ? assignees.filter((person) => String(person.id) !== String(currentUserId))
    : assignees

  const otherPeople = currentUserId
    ? people.filter((person) => String(person.id) !== String(currentUserId))
    : people
  const filtering =
    assigneeFilter !== 'all' || locationFilter.length > 0 || query.trim().length > 0

  return (
    <div className="task-board-wrap" ref={wrapRef}>
      <div className="task-board-filters" ref={filtersRef}>
        <div className="task-board-filter-row">
          <span className="task-board-filter-label">Asignado</span>
          <div className="legend" role="group" aria-label="Filtrar por asignado">
            <button
              type="button"
              className={`chip${assigneeFilter === 'all' ? ' is-on' : ''}${
                assigneeFilter !== 'all' ? ' is-dim' : ''
              }`}
              aria-pressed={assigneeFilter === 'all'}
              onClick={() => setAssigneeFilter('all')}
            >
              Todas
            </button>
            {currentUserId && (
              <button
                type="button"
                className={`chip${assigneeFilter === 'mine' ? ' is-on' : ''}${
                  assigneeFilter !== 'all' && assigneeFilter !== 'mine' ? ' is-dim' : ''
                }`}
                aria-pressed={assigneeFilter === 'mine'}
                onClick={() => selectAssignee('mine')}
              >
                Mías
              </button>
            )}
            <button
              type="button"
              className={`chip${assigneeFilter === UNASSIGNED ? ' is-on' : ''}${
                assigneeFilter !== 'all' && assigneeFilter !== UNASSIGNED ? ' is-dim' : ''
              }`}
              aria-pressed={assigneeFilter === UNASSIGNED}
              onClick={() => selectAssignee(UNASSIGNED)}
            >
              Sin asignar
            </button>
            {otherPeople.map((person) => {
              const on = String(assigneeFilter) === String(person.id)
              const dim = assigneeFilter !== 'all' && !on
              return (
                <button
                  type="button"
                  className={`chip${on ? ' is-on' : ''}${dim ? ' is-dim' : ''}`}
                  key={person.id}
                  aria-pressed={on}
                  title={on ? `Quitar filtro ${person.name}` : `Mostrar solo ${person.name}`}
                  onClick={() => selectAssignee(String(person.id))}
                >
                  <span
                    className="dot"
                    style={{ background: paletteFor(person.id, people).c }}
                  />
                  {person.name}
                </button>
              )
            })}
          </div>
        </div>

        <div className="task-board-filter-row">
          <span className="task-board-filter-label">Local</span>
          <div className="legend" role="group" aria-label="Filtrar por local">
            {locationFilter.length > 0 && (
              <button
                type="button"
                className="chip chip-clear"
                onClick={() => setLocationFilter([])}
              >
                Todos
              </button>
            )}
            {locations.map((location) => {
              const on = locationFilter.includes(location.id)
              const dim = locationFilter.length > 0 && !on
              return (
                <button
                  type="button"
                  className={`chip${on ? ' is-on' : ''}${dim ? ' is-dim' : ''}`}
                  key={location.id}
                  aria-pressed={on}
                  title={
                    on
                      ? `Quitar filtro ${location.name}`
                      : `Mostrar solo ${location.name}`
                  }
                  onClick={() => toggleLocation(location.id)}
                >
                  <span className="dot" style={{ background: location.color }} />
                  {location.name}
                </button>
              )
            })}
            <button
              type="button"
              className={`chip${locationFilter.includes(NO_LOCATION) ? ' is-on' : ''}${
                locationFilter.length > 0 && !locationFilter.includes(NO_LOCATION)
                  ? ' is-dim'
                  : ''
              }`}
              aria-pressed={locationFilter.includes(NO_LOCATION)}
              title="Tareas sin local"
              onClick={() => toggleLocation(NO_LOCATION)}
            >
              Sin local
            </button>
          </div>
          <input
            className="task-board-search"
            type="search"
            value={query}
            placeholder="Buscar título…"
            aria-label="Buscar por título"
            onChange={(event) => setQuery(event.target.value)}
          />
          {filtering && (
            <span className="task-filter-count">
              {visibleTasks.length} de {tasks.length}
            </span>
          )}
        </div>
      </div>

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
                  {assignChoices.map((assignee) => (
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
    </div>
  )
}
