import { useEffect, useState } from 'react'
import {
  actionMessage,
  activateLocation,
  createLocation,
  deactivateLocation,
  getManageableLocations,
  updateLocation,
} from '../api/client'
import { isAbsenceLocation } from '../lib/locations'
import ConfirmModal from './ConfirmModal'

const EMPTY = {
  id: null,
  name: '',
  color: '#2A5B8C',
  supportsVidriera: false,
  sortOrder: '',
}

export default function LocationsModal({ open, onClose, onChanged }) {
  const [rows, setRows] = useState([])
  const [form, setForm] = useState(null)
  const [warn, setWarn] = useState('')
  const [busy, setBusy] = useState(false)
  const [pendingBaja, setPendingBaja] = useState(null)

  async function load() {
    const list = await getManageableLocations()
    setRows((list || []).filter((location) => !isAbsenceLocation(location.id)))
  }

  useEffect(() => {
    if (!open) return
    let cancelled = false
    setWarn('')
    setForm(null)
    ;(async () => {
      try {
        const list = await getManageableLocations()
        if (!cancelled) {
          setRows((list || []).filter((location) => !isAbsenceLocation(location.id)))
        }
      } catch (e) {
        if (!cancelled) setWarn(actionMessage(e, 'No se pudieron cargar los locales.'))
      }
    })()
    return () => {
      cancelled = true
    }
  }, [open])

  if (!open) return null

  function startCreate() {
    setWarn('')
    setForm({ ...EMPTY })
  }

  function startEdit(location) {
    setWarn('')
    setForm({
      id: location.id,
      name: location.name,
      color: location.color || '#2A5B8C',
      supportsVidriera: !!location.supportsVidriera,
      sortOrder: location.sortOrder ?? '',
    })
  }

  async function save(event) {
    event.preventDefault()
    if (!form || busy) return
    setBusy(true)
    setWarn('')
    try {
      if (form.id) {
        await updateLocation(form.id, {
          name: form.name,
          color: form.color,
          supportsVidriera: form.supportsVidriera,
          sortOrder: form.sortOrder === '' ? null : Number(form.sortOrder),
        })
      } else {
        await createLocation({
          name: form.name,
          color: form.color,
          supportsVidriera: form.supportsVidriera,
        })
      }
      setForm(null)
      await load()
      await onChanged?.()
    } catch (e) {
      setWarn(actionMessage(e, 'No se pudo guardar el local.'))
    } finally {
      setBusy(false)
    }
  }

  async function confirmBaja() {
    if (!pendingBaja || busy) return
    setBusy(true)
    setWarn('')
    try {
      await deactivateLocation(pendingBaja.id)
      setPendingBaja(null)
      await load()
      await onChanged?.()
    } catch (e) {
      setWarn(actionMessage(e, 'No se pudo dar de baja el local.'))
    } finally {
      setBusy(false)
    }
  }

  async function alta(location) {
    setBusy(true)
    setWarn('')
    try {
      await activateLocation(location.id)
      await load()
      await onChanged?.()
    } catch (e) {
      setWarn(actionMessage(e, 'No se pudo dar de alta el local.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div
      className="overlay panel-overlay open"
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onClose?.()
      }}
    >
      <div
        className="modal panel-modal locations-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="locations-title"
      >
        <h3 id="locations-title">Locales</h3>
        <p className="m-sub">
          Alta, edición y baja de los locales de venta. Franco y vacaciones no se administran acá.
        </p>

        {!form && (
          <>
            <div className="location-admin-list">
              {rows.length === 0 && <div className="m-sub">No hay locales de venta.</div>}
              {rows.map((location) => (
                <div className="location-admin-row" key={location.id}>
                  <span className="dot" style={{ background: location.color }} />
                  <span className="location-admin-name">{location.name}</span>
                  {!location.active && <span className="location-admin-badge">de baja</span>}
                  {location.supportsVidriera && <span className="location-admin-badge">vidriera</span>}
                  <span className="location-admin-actions">
                    <button type="button" className="btn btn-ghost" disabled={busy} onClick={() => startEdit(location)}>
                      Editar
                    </button>
                    {location.active ? (
                      <button
                        type="button"
                        className="btn btn-danger"
                        disabled={busy}
                        onClick={() => setPendingBaja(location)}
                      >
                        Dar de baja
                      </button>
                    ) : (
                      <button type="button" className="btn btn-primary" disabled={busy} onClick={() => alta(location)}>
                        Dar de alta
                      </button>
                    )}
                  </span>
                </div>
              ))}
            </div>
            <div className="modal-actions">
              <button type="button" className="btn btn-ghost" disabled={busy} onClick={onClose}>
                Cerrar
              </button>
              <button type="button" className="btn btn-primary" disabled={busy} onClick={startCreate}>
                Nuevo local
              </button>
            </div>
          </>
        )}

        {form && (
          <form className="panel-form" onSubmit={save}>
            <div className="field">
              <label htmlFor="location-name">Nombre</label>
              <input
                id="location-name"
                value={form.name}
                required
                disabled={busy}
                onChange={(e) => setForm((current) => ({ ...current, name: e.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="location-color">Color</label>
              <input
                id="location-color"
                type="color"
                value={form.color}
                disabled={busy}
                onChange={(e) => setForm((current) => ({ ...current, color: e.target.value }))}
              />
            </div>
            {form.id && (
              <div className="field">
                <label htmlFor="location-order">Orden</label>
                <input
                  id="location-order"
                  type="number"
                  min="0"
                  value={form.sortOrder}
                  disabled={busy}
                  onChange={(e) => setForm((current) => ({ ...current, sortOrder: e.target.value }))}
                />
              </div>
            )}
            <label className="field location-admin-check">
              <input
                type="checkbox"
                checked={form.supportsVidriera}
                disabled={busy}
                onChange={(e) =>
                  setForm((current) => ({ ...current, supportsVidriera: e.target.checked }))
                }
              />
              Hace vidriera
            </label>
            {warn && <div className="m-warn vacation-load-warn">{warn}</div>}
            <div className="modal-actions">
              <button type="button" className="btn btn-ghost" disabled={busy} onClick={() => setForm(null)}>
                Volver
              </button>
              <button type="submit" className="btn btn-primary" disabled={busy}>
                {busy ? '…' : 'Guardar'}
              </button>
            </div>
          </form>
        )}

        {!form && warn && <div className="m-warn vacation-load-warn">{warn}</div>}
      </div>

      <ConfirmModal
        open={!!pendingBaja}
        title="Dar de baja"
        message={
          pendingBaja
            ? `${pendingBaja.name} deja de ofrecerse para turnos, tareas y vidrieras nuevas. Lo ya cargado se conserva.`
            : ''
        }
        confirmLabel="Dar de baja"
        busy={busy}
        onClose={() => {
          if (!busy) setPendingBaja(null)
        }}
        onConfirm={confirmBaja}
      />
    </div>
  )
}
