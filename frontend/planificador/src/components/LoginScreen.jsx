import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'

export default function LoginScreen({ open = true, onClose }) {
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  if (!open) return null

  async function handleSubmit(e) {
    e.preventDefault()
    if (busy) return
    setBusy(true)
    setError('')
    try {
      await login(username.trim(), password)
      setUsername('')
      setPassword('')
      onClose?.()
    } catch (err) {
      setError(err.message || 'No se pudo iniciar sesión')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div
      className="overlay open login-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget && onClose) onClose()
      }}
    >
      <form className="modal login-card" onSubmit={handleSubmit}>
        <h3>Iniciar sesión</h3>
        <div className="m-sub">Necesario para editar el planificador</div>
        {error && <div className="m-warn">{error}</div>}
        <div className="field">
          <label htmlFor="login-user">Usuario</label>
          <input
            id="login-user"
            autoComplete="username"
            value={username}
            disabled={busy}
            onChange={(e) => setUsername(e.target.value)}
            required
            autoFocus
          />
        </div>
        <div className="field">
          <label htmlFor="login-pass">Contraseña</label>
          <input
            id="login-pass"
            type="password"
            autoComplete="current-password"
            value={password}
            disabled={busy}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        <div className="modal-actions">
          {onClose && (
            <button
              type="button"
              className="btn btn-ghost"
              disabled={busy}
              onClick={onClose}
            >
              Cancelar
            </button>
          )}
          <button
            type="submit"
            className="btn btn-primary"
            disabled={busy || !username.trim() || !password}
          >
            {busy ? 'Ingresando…' : 'Ingresar'}
          </button>
        </div>
      </form>
    </div>
  )
}
