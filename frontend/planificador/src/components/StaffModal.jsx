import { useState } from 'react'
import { initials, paletteFor } from '../lib/palette'
import ConfirmModal from './ConfirmModal'

export default function StaffModal({
  open,
  staff,
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
        <div className="modal staff-modal">
          <h3>Personal</h3>
          <div className="m-sub">
            {canWrite
              ? 'Se dan de alta sin acceso al sistema; el ingreso se habilita desde Usuarios'
              : 'Personal del planificador'}
          </div>
          <ul>
            {staff.length === 0 && (
              <li style={{ color: 'var(--ink-soft)', fontSize: '12.5px' }}>
                Sin personal todavía.
              </li>
            )}
            {staff.map((member) => {
              const av = paletteFor(member.id, staff).c
              return (
                <li key={member.id}>
                  <span className="av" style={{ background: av }}>
                    {initials(member.name)}
                  </span>
                  <span className="pn">{member.name}</span>
                  {canWrite && (
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => setPendingRemove(member)}
                    >
                      Quitar
                    </button>
                  )}
                </li>
              )
            })}
          </ul>
          {canWrite && (
            <div className="new-staff-row">
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
        title="Quitar del personal"
        message={
          pendingRemove
            ? `¿Quitar a ${pendingRemove.name} del planificador? Sus turnos dejan de verse.`
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
