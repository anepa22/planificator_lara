import { useEffect, useState } from 'react'
import {
  createTask,
  deleteTask,
  getTasks,
  publishTask,
  retireTask,
  updateTask,
} from '../api/client'
import ConfirmModal from './ConfirmModal'
import { workLocations } from '../lib/locations'

const STATUS_LABELS = {
  PENDING: 'Pendientes',
  IN_PROGRESS: 'En proceso',
  BLOCKED: 'Bloqueada',
  DONE: 'Terminada',
  VERIFIED: 'Verificada',
}

function isCompleted(task) {
  return !task.onBoard && task.status === 'VERIFIED'
}

export default function TaskAdminModal({ open, onClose, onChanged, locations = [] }) {
  const [tasks, setTasks] = useState([])
  const [editing, setEditing] = useState(null)
  const [pendingDelete, setPendingDelete] = useState(null)
  const [form, setForm] = useState({ title: '', description: '', locationId: '' })
  const [busy, setBusy] = useState(false)
  const [loaded, setLoaded] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!open) return
    resetForm()
    void reload()
  }, [open])

  async function reload() {
    setBusy(true)
    setError('')
    try {
      setTasks(await getTasks())
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
      setLoaded(true)
    }
  }

  function resetForm() {
    setEditing(null)
    setForm({ title: '', description: '', locationId: '' })
  }

  async function run(action) {
    setBusy(true)
    setError('')
    try {
      await action()
      await reload()
      await onChanged?.()
      return true
    } catch (err) {
      setError(err.message)
      return false
    } finally {
      setBusy(false)
    }
  }

  async function save(event) {
    event.preventDefault()
    const payload = {
      title: form.title.trim(),
      description: form.description.trim() || null,
      locationId: form.locationId || null,
    }
    if (!payload.title) return
    const ok = await run(() =>
      editing ? updateTask(editing.id, payload) : createTask(payload),
    )
    if (ok) resetForm()
  }

  if (!open) return null

  const locOptions = workLocations(locations)
  const completed = tasks.filter(isCompleted)
  const active = tasks.filter((task) => !isCompleted(task))

  function renderTask(task) {
    const completedTask = isCompleted(task)
    const state = completedTask
      ? 'COMPLETED'
      : task.onBoard
        ? task.status
        : 'OFF_BOARD'
    return (
      <li
        key={task.id}
        className={`task-admin-item state-${state}${
          editing?.id === task.id ? ' is-editing' : ''
        }`}
      >
        <div className="task-admin-rail" aria-hidden />
        <div className="task-admin-body">
          <span className="task-admin-title" title={task.title}>
            {task.title}
          </span>
          {task.description && (
            <span className="task-admin-desc" title={task.description}>
              {task.description}
            </span>
          )}
          {task.locationName && (
            <span className="task-admin-loc" title={task.locationName}>
              {task.locationName}
            </span>
          )}
          {task.assigneeName && (
            <span className="task-admin-who">{task.assigneeName}</span>
          )}
        </div>
        <span className="task-admin-state">
          {completedTask
            ? 'Completada'
            : task.onBoard
              ? STATUS_LABELS[task.status] || task.status
              : 'Fuera del tablero'}
        </span>
        <div className="task-admin-actions">
          <button
            type="button"
            disabled={busy}
            onClick={() => {
              setEditing(task)
              setForm({
                title: task.title,
                description: task.description || '',
                locationId: task.locationId || '',
              })
            }}
          >
            Editar
          </button>
          {!task.onBoard && !completedTask && (
            <button
              type="button"
              disabled={busy}
              title="Agregar a Pendientes"
              onClick={() => run(() => publishTask(task.id))}
            >
              Publicar
            </button>
          )}
          {task.onBoard && (
            <button
              type="button"
              disabled={busy}
              title="Sacar del tablero"
              onClick={() => run(() => retireTask(task.id))}
            >
              Quitar
            </button>
          )}
          <button
            type="button"
            className="danger"
            disabled={busy}
            onClick={() => setPendingDelete(task)}
          >
            Baja
          </button>
        </div>
      </li>
    )
  }

  return (
    <>
      <div
        className="overlay open panel-overlay"
        onClick={(event) => {
          if (event.target === event.currentTarget && !pendingDelete && !busy) {
            onClose()
          }
        }}
      >
        <div
          className="modal panel-modal task-admin-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="task-admin-title"
        >
          <div className="panel-head">
            <h3 id="task-admin-title">Administrar tareas</h3>
            <button
              type="button"
              className="panel-close"
              disabled={busy}
              onClick={onClose}
              aria-label="Cerrar"
            >
              ✕
            </button>
          </div>
          {error && <div className="m-warn">{error}</div>}

          <form className="panel-form task-admin-form" onSubmit={save}>
            <div className="task-admin-fields">
              <div className="field">
                <label htmlFor="task-title">
                  {editing ? 'Título (editando)' : 'Título'}
                </label>
                <input
                  id="task-title"
                  type="text"
                  value={form.title}
                  maxLength={160}
                  disabled={busy}
                  required
                  onChange={(event) =>
                    setForm((current) => ({ ...current, title: event.target.value }))
                  }
                />
              </div>
              <div className="field">
                <label htmlFor="task-location">Local</label>
                <select
                  id="task-location"
                  value={form.locationId}
                  disabled={busy}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      locationId: event.target.value,
                    }))
                  }
                >
                  <option value="">Sin local</option>
                  {locOptions.map((location) => (
                    <option key={location.id} value={location.id}>
                      {location.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="field">
              <label htmlFor="task-description">Descripción</label>
              <textarea
                id="task-description"
                rows={5}
                value={form.description}
                maxLength={4000}
                disabled={busy}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    description: event.target.value,
                  }))
                }
              />
            </div>
            <div className="panel-form-actions">
              <button type="submit" className="btn btn-primary" disabled={busy}>
                {editing ? 'Guardar cambios' : 'Crear tarea'}
              </button>
              {editing && (
                <button
                  type="button"
                  className="btn btn-ghost"
                  disabled={busy}
                  onClick={resetForm}
                >
                  Cancelar edición
                </button>
              )}
            </div>
          </form>

          <div className="panel-toolbar">
            <span className="panel-count">
              {!loaded || busy
                ? 'Cargando…'
                : tasks.length === 0
                  ? 'Sin tareas'
                  : `${active.length} tarea${active.length === 1 ? '' : 's'}${
                      completed.length
                        ? ` · ${completed.length} completada${
                            completed.length === 1 ? '' : 's'
                          }`
                        : ''
                    }`}
            </span>
          </div>

          <div className={`panel-list${busy ? ' is-busy' : ''}`}>
            {!busy && loaded && tasks.length === 0 ? (
              <div className="panel-empty">
                <div className="panel-empty-title">Todavía no hay tareas</div>
                <div>Creá la primera con el formulario de arriba.</div>
              </div>
            ) : (
              <>
                {active.length > 0 && <ul>{active.map(renderTask)}</ul>}
                {completed.length > 0 && (
                  <div className="task-admin-section">
                    <div className="task-admin-section-title">Completadas</div>
                    <ul>{completed.map(renderTask)}</ul>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>

      <ConfirmModal
        open={!!pendingDelete}
        title="Dar de baja la tarea"
        message={pendingDelete?.title}
        confirmLabel="Dar de baja"
        busy={busy}
        onClose={() => setPendingDelete(null)}
        onConfirm={async () => {
          const task = pendingDelete
          setPendingDelete(null)
          if (task) await run(() => deleteTask(task.id))
        }}
      />
    </>
  )
}
