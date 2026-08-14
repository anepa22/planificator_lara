import { useState } from 'react'
import { initials, paletteFor } from '../lib/palette'
import ConfirmModal from './ConfirmModal'

export default function PeopleModal({
  open,
  people,
  onClose,
  onAdd,
  onRemove,
  busy,
  canWrite = true,
}) {
  const [name, setName] = useState('')
  const [pendingRemove, setPendingRemove] = useState(null)

  if (!open) return null

  async function handleAdd() {
    const trimmed = name.trim()
    if (!trimmed || busy || !canWrite) return
    await onAdd(trimmed)
    setName('')
  }

  return (
    <>
      <div
        className="overlay open"
        onClick={(e) => {
          if (e.target === e.currentTarget && !pendingRemove) onClose()
        }}
      >
        <div className="modal people-modal">
          <h3>Personas</h3>
          <div className="m-sub">
            {canWrite ? 'Agregar o quitar del equipo' : 'Personal del equipo'}
          </div>
          <ul>
            {people.length === 0 && (
              <li style={{ color: 'var(--ink-soft)', fontSize: '12.5px' }}>
                Sin personas todavía.
              </li>
            )}
            {people.map((emp) => {
              const av = paletteFor(emp.id, people).c
              return (
                <li key={emp.id}>
                  <span className="av" style={{ background: av }}>
                    {initials(emp.name)}
                  </span>
                  <span className="pn">{emp.name}</span>
                  {canWrite && (
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => setPendingRemove(emp)}
                    >
                      Quitar
                    </button>
                  )}
                </li>
              )
            })}
          </ul>
          {canWrite && (
            <div className="new-person-row">
              <input
                type="text"
                value={name}
                placeholder="Nombre y apellido"
                disabled={busy}
                onChange={(e) => setName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleAdd()
                }}
                autoFocus
              />
              <button
                type="button"
                className="btn btn-primary"
                disabled={busy}
                onClick={handleAdd}
              >
                Agregar
              </button>
            </div>
          )}
          <div className="modal-actions">
            <button
              type="button"
              className="btn btn-ghost"
              style={{ flex: 1 }}
              onClick={onClose}
            >
              Cerrar
            </button>
          </div>
        </div>
      </div>

      <ConfirmModal
        open={!!pendingRemove}
        title="Quitar persona"
        message={
          pendingRemove
            ? `¿Quitar a ${pendingRemove.name} del equipo?`
            : ''
        }
        busy={busy}
        onClose={() => setPendingRemove(null)}
        onConfirm={async () => {
          const id = pendingRemove?.id
          setPendingRemove(null)
          if (id) await onRemove(id)
        }}
      />
    </>
  )
}
