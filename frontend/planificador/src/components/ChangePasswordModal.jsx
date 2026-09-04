import { useEffect, useRef, useState } from 'react'
import { changePassword } from '../api/client'

export default function ChangePasswordModal({
  open,
  required = false,
  onClose,
  onChanged,
}) {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [ok, setOk] = useState(false)
  const closeTimer = useRef(null)

  useEffect(() => {
    return () => {
      if (closeTimer.current) clearTimeout(closeTimer.current)
    }
  }, [])

  useEffect(() => {
    if (!open) {
      if (closeTimer.current) clearTimeout(closeTimer.current)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setError('')
      setOk(false)
      setBusy(false)
    }
  }, [open])

  if (!open) return null

  function close() {
    if (required && !ok) return
    if (closeTimer.current) clearTimeout(closeTimer.current)
    onClose?.()
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (busy || ok) return
    setError('')
    if (newPassword.length < 6) {
      setError('La nueva contraseña debe tener al menos 6 caracteres')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('La confirmación no coincide')
      return
    }
    if (newPassword === currentPassword) {
      setError('La nueva contraseña debe ser distinta a la actual')
      return
    }
    setBusy(true)
    try {
      await changePassword(currentPassword, newPassword)
      await onChanged?.()
      setOk(true)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      closeTimer.current = setTimeout(() => {
        onClose?.()
      }, 1200)
    } catch (err) {
      setError(err.message || 'No se pudo cambiar la contraseña')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div
      className={`overlay open${required ? ' password-required' : ''}`}
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy && !ok && !required) close()
      }}
    >
      <form className="modal login-card" onSubmit={handleSubmit}>
        <h3>{required ? 'Elegí tu contraseña' : 'Cambiar contraseña'}</h3>
        <div className="m-sub">
          {required
            ? 'Es tu primer ingreso. Tenés que definir una contraseña propia para continuar.'
            : 'Ingresá la contraseña actual y la nueva'}
        </div>
        {error && <div className="m-warn">{error}</div>}
        {ok && (
          <div className="m-ok" role="status">
            Contraseña actualizada
          </div>
        )}
        {!ok && (
          <>
            <div className="field">
              <label htmlFor="pwd-current">Contraseña actual</label>
              <input
                id="pwd-current"
                type="password"
                autoComplete="current-password"
                value={currentPassword}
                disabled={busy}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
                autoFocus
              />
            </div>
            <div className="field">
              <label htmlFor="pwd-new">Nueva contraseña</label>
              <input
                id="pwd-new"
                type="password"
                autoComplete="new-password"
                value={newPassword}
                disabled={busy}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                minLength={6}
              />
            </div>
            <div className="field">
              <label htmlFor="pwd-confirm">Confirmar nueva</label>
              <input
                id="pwd-confirm"
                type="password"
                autoComplete="new-password"
                value={confirmPassword}
                disabled={busy}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                minLength={6}
              />
            </div>
          </>
        )}
        <div className="modal-actions">
          {!ok && !required && (
            <button
              type="button"
              className="btn btn-ghost"
              disabled={busy}
              onClick={close}
            >
              Cancelar
            </button>
          )}
          {!ok && (
            <button
              type="submit"
              className="btn btn-primary"
              disabled={
                busy ||
                !currentPassword ||
                !newPassword ||
                !confirmPassword
              }
            >
              {busy ? 'Guardando…' : 'Guardar'}
            </button>
          )}
        </div>
      </form>
    </div>
  )
}
