import { useCallback, useEffect, useState } from 'react'
import {
  createUser,
  deleteUser,
  getRoles,
  getUsers,
  updateRolePermissions,
  updateUser,
  getPermissions,
} from '../api/client'
import { useAuth } from '../auth/useAuth'
import ConfirmModal from './ConfirmModal'

const ROLE_LABELS = {
  admin: 'Administrador',
  editor: 'Supervisor',
  personal: 'Personal',
}

const EMPTY_FORM = {
  username: '',
  password: '',
  displayName: '',
  canLogin: true,
  active: true,
  roleIds: [],
}

function usernameFromName(name) {
  return name
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '.')
    .replace(/^\.+|\.+$/g, '')
    .slice(0, 80)
}

export default function UsersModal({ open, onClose, onChanged }) {
  const { can } = useAuth()
  const canManageUsers = can('users:manage')
  const canManageRoles = can('roles:manage')

  const [tab, setTab] = useState('users')
  const [users, setUsers] = useState([])
  const [roles, setRoles] = useState([])
  const [permissions, setPermissions] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [pendingDelete, setPendingDelete] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [editingId, setEditingId] = useState(null)
  const [usernameTouched, setUsernameTouched] = useState(false)

  const reload = useCallback(async () => {
    setBusy(true)
    try {
      const tasks = []
      if (canManageUsers || canManageRoles) tasks.push(getRoles().then(setRoles))
      if (canManageUsers) tasks.push(getUsers().then(setUsers))
      if (canManageRoles) tasks.push(getPermissions().then(setPermissions))
      await Promise.all(tasks)
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }, [canManageRoles, canManageUsers])

  useEffect(() => {
    if (!open) return
    setError('')
    setTab('users')
    setEditingId(null)
    setForm(EMPTY_FORM)
    setUsernameTouched(false)
    void reload()
  }, [open, reload])

  if (!open) return null

  async function handleSaveUser(e) {
    e.preventDefault()
    if (busy) return
    setBusy(true)
    setError('')
    try {
      if (editingId) {
        await updateUser(editingId, {
          displayName: form.displayName.trim(),
          active: form.active,
          password: form.password.trim() || null,
          canLogin: form.canLogin,
          roleIds: form.roleIds,
        })
      } else {
        const username = form.username.trim() || usernameFromName(form.displayName)
        if (!username) {
          setError('El nombre de usuario es obligatorio')
          return
        }
        await createUser({
          username,
          password: form.canLogin ? form.password : null,
          displayName: form.displayName.trim(),
          canLogin: form.canLogin,
          roleIds: form.roleIds,
        })
      }
      setEditingId(null)
      setForm(EMPTY_FORM)
      setUsernameTouched(false)
      await reload()
      onChanged?.()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleDelete(user) {
    if (user.username === 'admin') return
    setPendingDelete(user)
  }

  async function confirmDelete() {
    const user = pendingDelete
    if (!user) return
    setPendingDelete(null)
    setBusy(true)
    setError('')
    try {
      await deleteUser(user.id)
      await reload()
      onChanged?.()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  function startEdit(user) {
    setEditingId(user.id)
    setUsernameTouched(true)
    setForm({
      username: user.username,
      password: '',
      displayName: user.displayName,
      canLogin: user.canLogin !== false,
      active: user.active !== false,
      roleIds: user.roles?.length ? [...user.roles] : [],
    })
  }

  function toggleRole(roleId) {
    setForm((prev) => {
      const has = prev.roleIds.includes(roleId)
      const roleIds = has
        ? prev.roleIds.filter((r) => r !== roleId)
        : [...prev.roleIds, roleId]
      return { ...prev, roleIds }
    })
  }

  async function toggleRolePermission(role, code) {
    if (!canManageRoles || busy) return
    const next = role.permissionCodes.includes(code)
      ? role.permissionCodes.filter((c) => c !== code)
      : [...role.permissionCodes, code]
    setBusy(true)
    setError('')
    try {
      await updateRolePermissions(role.id, next)
      await reload()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const needsPassword = form.canLogin && !editingId

  return (
    <>
    <div
      className="overlay open"
      onClick={(e) => {
        if (e.target === e.currentTarget && !pendingDelete) onClose()
      }}
    >
      <div className="modal users-modal">
        <h3>Usuarios y roles</h3>
        <div className="m-sub">
          Todo el personal es un usuario. Sin acceso habilitado igual aparece en
          el planificador.
        </div>
        {error && <div className="m-warn">{error}</div>}

        <div className="users-tabs">
          {canManageUsers && (
            <button
              type="button"
              className={`users-tab${tab === 'users' ? ' active' : ''}`}
              onClick={() => setTab('users')}
            >
              Usuarios
            </button>
          )}
          {canManageRoles && (
            <button
              type="button"
              className={`users-tab${tab === 'roles' ? ' active' : ''}`}
              onClick={() => setTab('roles')}
            >
              Roles
            </button>
          )}
        </div>

        {tab === 'users' && canManageUsers && (
          <>
            <form className="users-form" onSubmit={handleSaveUser}>
              <div className="field">
                <label>Nombre</label>
                <input
                  value={form.displayName}
                  disabled={busy}
                  onChange={(e) => {
                    const displayName = e.target.value
                    setForm((f) => ({
                      ...f,
                      displayName,
                      username:
                        editingId || usernameTouched
                          ? f.username
                          : usernameFromName(displayName),
                    }))
                  }}
                  required
                />
              </div>
              <div className="field">
                <label>Nombre de usuario</label>
                <input
                  value={form.username}
                  disabled={busy || !!editingId}
                  autoComplete="username"
                  placeholder="ej. brenda.cappa"
                  onChange={(e) => {
                    setUsernameTouched(true)
                    setForm((f) => ({ ...f, username: e.target.value }))
                  }}
                  required={!editingId}
                  minLength={2}
                />
              </div>
              <label className={`check-card check-card-compact${form.canLogin ? ' is-on' : ''}`}>
                <input
                  type="checkbox"
                  checked={form.canLogin}
                  disabled={busy}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, canLogin: e.target.checked }))
                  }
                />
                <span className="check-card-box" aria-hidden />
                <span className="check-card-text">
                  <span className="check-card-title">Puede ingresar al sistema</span>
                  <span className="check-card-sub">
                    Si está apagado solo figura en el planificador
                  </span>
                </span>
              </label>
              {form.canLogin && (
                <div className="field">
                  <label>
                    {editingId ? 'Nueva contraseña (opcional)' : 'Contraseña'}
                  </label>
                  <input
                    type="password"
                    value={form.password}
                    disabled={busy}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, password: e.target.value }))
                    }
                    required={needsPassword}
                    minLength={6}
                  />
                </div>
              )}
              {editingId && (
                <label className={`check-card check-card-compact${form.active ? ' is-on' : ''}`}>
                  <input
                    type="checkbox"
                    checked={form.active}
                    disabled={busy}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, active: e.target.checked }))
                    }
                  />
                  <span className="check-card-box" aria-hidden />
                  <span className="check-card-text">
                    <span className="check-card-title">Activo</span>
                    <span className="check-card-sub">
                      Al desactivarlo desaparece de la grilla y del resumen
                    </span>
                  </span>
                </label>
              )}
              <div className="field">
                <label>Roles</label>
                <div className="pick-list">
                  {roles.map((r) => (
                    <label className="pp-item" key={r.id}>
                      <input
                        type="checkbox"
                        checked={form.roleIds.includes(r.id)}
                        disabled={busy}
                        onChange={() => toggleRole(r.id)}
                      />
                      <span className="pp-name">
                        {ROLE_LABELS[r.code] || r.name}
                      </span>
                    </label>
                  ))}
                </div>
              </div>
              <div className="modal-actions">
                {editingId && (
                  <button
                    type="button"
                    className="btn btn-ghost"
                    disabled={busy}
                    onClick={() => {
                      setEditingId(null)
                      setForm(EMPTY_FORM)
                      setUsernameTouched(false)
                    }}
                  >
                    Cancelar edición
                  </button>
                )}
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={busy}
                >
                  {editingId ? 'Guardar usuario' : 'Crear usuario'}
                </button>
              </div>
            </form>

            <ul className="vac-list">
              {users.map((u) => (
                <li key={u.id}>
                  <div className="vac-list-row">
                    <span className="vac-list-text">
                      <span className="pn">{u.displayName}</span>
                      <span className="vac-list-range">
                        {u.username}
                        {!u.active ? ' · inactivo' : ''}
                        {u.canLogin === false ? ' · sin acceso' : ''}
                        {(u.roles || []).length
                          ? ` · ${(u.roles || [])
                              .map((r) => ROLE_LABELS[r] || r)
                              .join(', ')}`
                          : ''}
                      </span>
                    </span>
                    <button
                      type="button"
                      className="vac-list-remove"
                      disabled={busy}
                      onClick={() => startEdit(u)}
                    >
                      Editar
                    </button>
                    {u.username !== 'admin' && (
                      <button
                        type="button"
                        className="vac-list-remove"
                        disabled={busy}
                        onClick={() => handleDelete(u)}
                      >
                        Eliminar
                      </button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          </>
        )}

        {tab === 'roles' && canManageRoles && (
          <div className="roles-panel">
            {roles.map((role) => (
              <div className="role-block" key={role.id}>
                <div className="role-title">
                  {ROLE_LABELS[role.code] || role.name}
                </div>
                <div className="pick-list">
                  {permissions.map((p) => (
                    <label className="pp-item" key={p.id}>
                      <input
                        type="checkbox"
                        checked={role.permissionCodes.includes(p.code)}
                        disabled={busy}
                        onChange={() => toggleRolePermission(role, p.code)}
                      />
                      <span className="pp-name">
                        {p.name}
                        <span className="vac-list-range"> · {p.code}</span>
                      </span>
                    </label>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="modal-actions">
          <button
            type="button"
            className="btn btn-ghost"
            disabled={busy}
            onClick={onClose}
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>

    <ConfirmModal
      open={!!pendingDelete}
      title="Eliminar usuario"
      message={
        pendingDelete
          ? `¿Eliminar el usuario ${pendingDelete.username}? Se borran también sus turnos.`
          : ''
      }
      confirmLabel="Eliminar"
      busy={busy}
      onClose={() => setPendingDelete(null)}
      onConfirm={confirmDelete}
    />
    </>
  )
}
