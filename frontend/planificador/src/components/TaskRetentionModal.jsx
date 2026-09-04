import { useEffect, useState } from 'react'
import {
  getTaskRetentionSettings,
  updateTaskRetentionSettings,
} from '../api/client'

export default function TaskRetentionModal({ open, onClose, onChanged }) {
  const [days, setDays] = useState(7)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!open) return
    setBusy(true)
    setError('')
    getTaskRetentionSettings()
      .then((settings) => setDays(settings.verifiedRetentionDays))
      .catch((err) => setError(err.message))
      .finally(() => setBusy(false))
  }, [open])

  if (!open) return null

  async function save(event) {
    event.preventDefault()
    const value = Number(days)
    if (!Number.isInteger(value) || value < 1 || value > 3650) {
      setError('Ingresá una cantidad entre 1 y 3650 días')
      return
    }
    setBusy(true)
    setError('')
    try {
      await updateTaskRetentionSettings(value)
      await onChanged?.()
      onClose()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div
      className="overlay open"
      onClick={(event) => {
        if (event.target === event.currentTarget && !busy) onClose()
      }}
    >
      <form className="modal task-retention-modal" onSubmit={save}>
        <h3>Retiro de tareas verificadas</h3>
        <div className="m-sub">
          Las tareas salen automáticamente del tablero al superar esta cantidad
          de días corridos en Verificada. Permanecen en Completadas.
        </div>
        {error && <div className="m-warn">{error}</div>}
        <div className="field">
          <label htmlFor="verified-retention-days">Días en Verificada</label>
          <input
            id="verified-retention-days"
            type="number"
            min="1"
            max="3650"
            step="1"
            value={days}
            disabled={busy}
            autoFocus
            required
            onChange={(event) => setDays(event.target.value)}
          />
        </div>
        <div className="modal-actions">
          <button
            type="button"
            className="btn btn-ghost"
            disabled={busy}
            onClick={onClose}
          >
            Cancelar
          </button>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            Guardar
          </button>
        </div>
      </form>
    </div>
  )
}
