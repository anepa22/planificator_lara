import { useCallback, useEffect, useMemo, useState } from 'react'
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
import { initials } from '../lib/palette'
import ConfirmModal from './ConfirmModal'

const ROLE_LABELS = {
  admin: 'Administrador',
  editor: 'Supervisor',
  personal: 'Asistente',
}

/** Los permisos se agrupan por área para que la grilla de roles se lea de un saque. */
const PERMISSION_GROUPS = [
  { id: 'tasks', title: 'Tareas', prefixes: ['tasks:'] },
  {
    id: 'schedule',
    title: 'Horarios y ausencias',
    prefixes: ['shifts:', 'vacations:', 'schedule:', 'lunch:'],
  },
  { id: 'team', title: 'Equipo y accesos', prefixes: ['staff:', 'users:', 'roles:'] },
  { id: 'other', title: 'Otros', prefixes: [] },
]

const EMPTY_FORM = {
  username: '',
  password: '',
  displayName: '',
  telegramChatId: '',
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

function groupPermissions(permissions) {
  const groups = PERMISSION_GROUPS.map((group) => ({ ...group, items: [] }))
  const fallback = groups[groups.length - 1]
  for (const permission of permissions) {
    const target = groups.find((group) =>
      group.prefixes.some((prefix) => permission.code.startsWith(prefix)),
    )
    ;(target || fallback).items.push(permission)
  }
  return groups.filter((group) => group.items.length > 0)
}

function roleLabel(role) {
  return ROLE_LABELS[role.code] || role.name
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
  const [loaded, setLoaded] = useState(false)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [pendingDelete, setPendingDelete] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [formOpen, setFormOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [usernameTouched, setUsernameTouched] = useState(false)

  const reload = useCallback(async () => {
    setBusy(true)
    try {
      const pending = []
      if (canManageUsers || canManageRoles) pending.push(getRoles().then(setRoles))
      if (canManageUsers) pending.push(getUsers().then(setUsers))
      if (canManageRoles) pending.push(getPermissions().then(setPermissions))
      await Promise.all(pending)
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
      setLoaded(true)
    }
  }, [canManageRoles, canManageUsers])

  useEffect(() => {
    if (!open) return
    setError('')
    setTab(canManageUsers ? 'users' : 'roles')
    setQuery('')
    setLoaded(false)
    closeForm()
    void reload()
  }, [open, reload, canManageUsers])

  const groupedPermissions = useMemo(
    () => groupPermissions(permissions),
    [permissions],
  )

  const visibleUsers = useMemo(() => {
    const needle = query.trim().toLocaleLowerCase('es')
    if (!needle) return users
    return users.filter((user) =>
      `${user.displayName || ''} ${user.username || ''}`
        .toLocaleLowerCase('es')
        .includes(needle),
    )
  }, [users, query])

  if (!open) return null

  function closeForm() {
    setEditingId(null)
    setForm(EMPTY_FORM)
    setUsernameTouched(false)
    setFormOpen(false)
  }

  function startCreate() {
    setEditingId(null)
    setForm(EMPTY_FORM)
    setUsernameTouched(false)
    setFormOpen(true)
  }

  function startEdit(user) {
    setEditingId(user.id)
    setUsernameTouched(true)
    setFormOpen(true)
    setForm({
      username: user.username,
      password: '',
      displayName: user.displayName,
      telegramChatId: user.telegramChatId || '',
      canLogin: user.canLogin !== false,
      active: user.active !== false,
      roleIds: user.roles?.length ? [...user.roles] : [],
    })
  }

  async function handleSaveUser(e) {
    e.preventDefault()
    if (busy) return
    setBusy(true)
    setError('')
    try {
      if (editingId) {
        await updateUser(editingId, {
          displayName: form.displayName.trim(),
          telegramChatId: form.telegramChatId.trim() || null,
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
          telegramChatId: form.telegramChatId.trim() || null,
          canLogin: form.canLogin,
          roleIds: form.roleIds,
        })
      }
      closeForm()
      await reload()
      onChanged?.()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function confirmDelete() {
    const user = pendingDelete
    if (!user) return
    setPendingDelete(null)
    setBusy(true)
    setError('')
    try {
      await deleteUser(user.id)
      if (editingId === user.id) closeForm()
      await reload()
      onChanged?.()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  function toggleRole(roleId) {
    setForm((prev) => {
      const roleIds = prev.roleIds.includes(roleId)
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
  const editingUser = editingId
    ? users.find((user) => user.id === editingId)
    : null

  return (
    <>
      <div
        className="overlay open panel-overlay"
        onClick={(e) => {
          if (e.target === e.currentTarget && !pendingDelete && !busy) onClose()
        }}
      >
        <div
          className="modal panel-modal users-panel"
          role="dialog"
          aria-modal="true"
          aria-labelledby="users-title"
        >
          <div className="panel-head">
            <h3 id="users-title">Usuarios y roles</h3>
            <div className="panel-head-right">
              <div className="users-tabs" role="tablist">
                {canManageUsers && (
                  <button
                    type="button"
                    role="tab"
                    aria-selected={tab === 'users'}
                    className={`users-tab${tab === 'users' ? ' active' : ''}`}
                    onClick={() => setTab('users')}
                  >
                    Usuarios
                  </button>
                )}
                {canManageRoles && (
                  <button
                    type="button"
                    role="tab"
                    aria-selected={tab === 'roles'}
                    className={`users-tab${tab === 'roles' ? ' active' : ''}`}
                    onClick={() => setTab('roles')}
                  >
                    Roles
                  </button>
                )}
              </div>
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
          </div>

          {error && <div className="m-warn">{error}</div>}

          {tab === 'users' && canManageUsers && (
            <>
              <div className="panel-toolbar users-toolbar">
                <span className="panel-count">
                  {!loaded || busy
                    ? 'Cargando…'
                    : users.length === 0
                      ? 'Sin usuarios'
                      : query.trim()
                        ? `${visibleUsers.length} de ${users.length}`
                        : `${users.length} usuario${users.length === 1 ? '' : 's'}`}
                </span>
                <div className="users-toolbar-right">
                  <input
                    className="users-search"
                    type="search"
                    value={query}
                    placeholder="Buscar por nombre o usuario"
                    aria-label="Buscar usuario"
                    onChange={(e) => setQuery(e.target.value)}
                  />
                  <button
                    type="button"
                    className="btn btn-primary users-new"
                    disabled={busy}
                    onClick={startCreate}
                  >
                    Nuevo usuario
                  </button>
                </div>
              </div>

              {formOpen && (
                <form className="panel-form users-form" onSubmit={handleSaveUser}>
                  <div className="users-form-head">
                    <span className="users-form-title">
                      {editingId
                        ? `Editando ${editingUser?.displayName || form.displayName}`
                        : 'Nuevo usuario'}
                    </span>
                    <button
                      type="button"
                      className="users-form-close"
                      disabled={busy}
                      onClick={closeForm}
                    >
                      Cancelar
                    </button>
                  </div>
                  <div className="field">
                    <label htmlFor="user-name">Nombre</label>
                    <input
                      id="user-name"
                      type="text"
                      value={form.displayName}
                      disabled={busy}
                      required
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
                    />
                  </div>
                  <div className="field">
                    <label htmlFor="user-username">Nombre de usuario</label>
                    <input
                      id="user-username"
                      type="text"
                      value={form.username}
                      disabled={busy || !!editingId}
                      autoComplete="username"
                      placeholder="ej. brenda.cappa"
                      required={!editingId}
                      minLength={2}
                      onChange={(e) => {
                        setUsernameTouched(true)
                        setForm((f) => ({ ...f, username: e.target.value }))
                      }}
                    />
                  </div>
                  {form.canLogin && (
                    <div className="field">
                      <label htmlFor="user-password">
                        {editingId ? 'Nueva contraseña (opcional)' : 'Contraseña'}
                      </label>
                      <input
                        id="user-password"
                        type="password"
                        value={form.password}
                        disabled={busy}
                        required={needsPassword}
                        minLength={6}
                        onChange={(e) =>
                          setForm((f) => ({ ...f, password: e.target.value }))
                        }
                      />
                    </div>
                  )}
                  <div className="field">
                    <label htmlFor="user-telegram">Chat ID de Telegram</label>
                    <input
                      id="user-telegram"
                      type="text"
                      inputMode="numeric"
                      value={form.telegramChatId}
                      disabled={busy}
                      placeholder="opcional · ej. 123456789"
                      maxLength={32}
                      pattern="-?[0-9]+"
                      onChange={(e) =>
                        setForm((f) => ({ ...f, telegramChatId: e.target.value }))
                      }
                    />
                    <span className="field-help">
                      Recibe avisos si su rol tiene «Recibir notificaciones de
                      tareas» y ya inició el bot.
                    </span>
                  </div>
                  <div className="field field-wide">
                    <label>Roles</label>
                    <div className="role-pills">
                      {roles.map((role) => {
                        const on = form.roleIds.includes(role.id)
                        return (
                          <label
                            key={role.id}
                            className={`role-pill${on ? ' is-on' : ''}`}
                          >
                            <input
                              type="checkbox"
                              checked={on}
                              disabled={busy}
                              onChange={() => toggleRole(role.id)}
                            />
                            {roleLabel(role)}
                          </label>
                        )
                      })}
                    </div>
                  </div>
                  <div className="users-form-checks field-wide">
                    <label
                      className={`check-card check-card-compact${
                        form.canLogin ? ' is-on' : ''
                      }`}
                    >
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
                        <span className="check-card-title">Puede ingresar</span>
                        <span className="check-card-sub">
                          Si está apagado solo figura en las grillas
                        </span>
                      </span>
                    </label>
                    {editingId && (
                      <label
                        className={`check-card check-card-compact${
                          form.active ? ' is-on' : ''
                        }`}
                      >
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
                            Al desactivarlo sale de la grilla y del resumen
                          </span>
                        </span>
                      </label>
                    )}
                  </div>
                  <div className="panel-form-actions">
                    <button type="submit" className="btn btn-primary" disabled={busy}>
                      {editingId ? 'Guardar cambios' : 'Crear usuario'}
                    </button>
                  </div>
                </form>
              )}

              <div className={`panel-list${busy ? ' is-busy' : ''}`}>
                {!busy && loaded && visibleUsers.length === 0 ? (
                  <div className="panel-empty">
                    <div className="panel-empty-title">
                      {users.length === 0
                        ? 'Todavía no hay usuarios'
                        : 'Nadie coincide con la búsqueda'}
                    </div>
                    <div>
                      {users.length === 0
                        ? 'Creá el primero con «Nuevo usuario».'
                        : 'Probá con otro nombre.'}
                    </div>
                  </div>
                ) : (
                  <ul>
                    {visibleUsers.map((user) => (
                      <li
                        key={user.id}
                        className={`user-item${
                          user.active === false ? ' is-off' : ''
                        }${editingId === user.id ? ' is-editing' : ''}`}
                      >
                        <span
                          className="user-av"
                          style={{ background: user.color || '#5B6675' }}
                          aria-hidden
                        >
                          {initials(user.displayName)}
                        </span>
                        <div className="user-body">
                          <span className="user-name" title={user.displayName}>
                            {user.displayName}
                          </span>
                          <span className="user-tags">
                            <span className="user-username">{user.username}</span>
                            {(user.roles || []).map((role) => (
                              <span className="user-badge" key={role}>
                                {ROLE_LABELS[role] || role}
                              </span>
                            ))}
                            {user.canLogin === false && (
                              <span className="user-badge soft">Sin acceso</span>
                            )}
                            {user.active === false && (
                              <span className="user-badge off">Inactivo</span>
                            )}
                            {user.telegramChatId && (
                              <span className="user-badge ok">Telegram</span>
                            )}
                          </span>
                        </div>
                        <div className="row-actions">
                          <button
                            type="button"
                            disabled={busy}
                            onClick={() => startEdit(user)}
                          >
                            Editar
                          </button>
                          {user.username !== 'admin' && (
                            <button
                              type="button"
                              className="danger"
                              disabled={busy}
                              onClick={() => setPendingDelete(user)}
                            >
                              Eliminar
                            </button>
                          )}
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </>
          )}

          {tab === 'roles' && canManageRoles && (
            <>
              <div className="panel-toolbar">
                <span className="panel-count">
                  {busy ? 'Guardando…' : `${roles.length} roles`}
                </span>
                <span className="panel-hint">
                  El rol solo agrupa permisos. Un usuario puede tener más de uno.
                </span>
              </div>
              <div className={`panel-list roles-list${busy ? ' is-busy' : ''}`}>
                {roles.map((role) => (
                  <section className="role-card" key={role.id}>
                    <header className="role-card-head">
                      <span className="role-card-name">{roleLabel(role)}</span>
                      <span className="role-card-count">
                        {role.permissionCodes.length} de {permissions.length} permisos
                      </span>
                    </header>
                    {groupedPermissions.map((group) => (
                      <div className="role-group" key={group.id}>
                        <div className="role-group-title">{group.title}</div>
                        <div className="role-perms">
                          {group.items.map((permission) => {
                            const on = role.permissionCodes.includes(permission.code)
                            return (
                              <label
                                key={permission.id}
                                className={`perm-toggle${on ? ' is-on' : ''}`}
                                title={permission.code}
                              >
                                <input
                                  type="checkbox"
                                  checked={on}
                                  disabled={busy}
                                  onChange={() =>
                                    toggleRolePermission(role, permission.code)
                                  }
                                />
                                <span className="perm-name">{permission.name}</span>
                              </label>
                            )
                          })}
                        </div>
                      </div>
                    ))}
                  </section>
                ))}
              </div>
            </>
          )}
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
