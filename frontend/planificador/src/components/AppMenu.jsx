import { useEffect, useState } from 'react'

function MenuItem({ children, onClick, danger = false, nested = false }) {
  return (
    <button
      type="button"
      className={`app-menu-item${danger ? ' danger' : ''}${nested ? ' nested' : ''}`}
      onClick={onClick}
    >
      <span className="app-menu-item-label">{children}</span>
      <span className="app-menu-item-chevron" aria-hidden>
        ›
      </span>
    </button>
  )
}

export default function AppMenu({
  open,
  onClose,
  view,
  onViewChange,
  periodLabel,
  onPrevPeriod,
  onNextPeriod,
  onToday,
  showPeople = true,
  showVacations = true,
  showUsers = false,
  showLunch = false,
  showVidriera = false,
  showTasksAdmin = false,
  showAudit = false,
  userLabel,
  loggedIn = false,
  onPeople,
  onVacations,
  onUsers,
  onLunch,
  onVidriera,
  onTasksAdmin,
  onAudit,
  onLogin,
  onLogout,
  onChangePassword,
  version,
}) {
  const showPersonalGroup = showPeople || showVacations || showLunch || showVidriera
  const showOtherActions = showUsers || showTasksAdmin || showAudit
  const hasActions = showPersonalGroup || showOtherActions
  const [personalOpen, setPersonalOpen] = useState(false)

  useEffect(() => {
    if (!open) setPersonalOpen(false)
  }, [open])

  return (
    <div
      className={`app-menu-overlay${open ? ' open' : ''}`}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
      aria-hidden={!open}
    >
      <nav
        className={`app-menu-drawer${open ? ' open' : ''}`}
        aria-label="Menú principal"
        role="dialog"
        aria-modal={open}
      >
        <div className="app-menu-head">
          <div>
            <div className="app-menu-title">Planificador Gisela</div>
            {userLabel && <div className="app-menu-user">{userLabel}</div>}
            <div className="app-menu-version">v{version}</div>
          </div>
          <button
            type="button"
            className="app-menu-close"
            onClick={onClose}
            aria-label="Cerrar menú"
          >
            ✕
          </button>
        </div>

        <div className="app-menu-section">
          <div className="app-menu-label">Vista</div>
          <div className="app-menu-view-toggle" role="group" aria-label="Tipo de vista">
            <button
              type="button"
              className={`app-menu-view-btn${view === 'week' ? ' active' : ''}`}
              aria-pressed={view === 'week'}
              onClick={() => {
                onViewChange('week')
                onClose()
              }}
            >
              Semanal
            </button>
            <button
              type="button"
              className={`app-menu-view-btn${view === 'month' ? ' active' : ''}`}
              aria-pressed={view === 'month'}
              onClick={() => {
                onViewChange('month')
                onClose()
              }}
            >
              Mensual
            </button>
            <button
              type="button"
              className={`app-menu-view-btn${view === 'tasks' ? ' active' : ''}`}
              aria-pressed={view === 'tasks'}
              onClick={() => {
                onViewChange('tasks')
                onClose()
              }}
            >
              Tareas
            </button>
          </div>
        </div>

        {view !== 'tasks' && <div className="app-menu-section">
          <div className="app-menu-label">
            {view === 'month' ? 'Mes' : 'Semana'}
          </div>
          <div className="app-menu-week">{periodLabel}</div>
          <div className="app-menu-week-actions">
            <button type="button" className="app-menu-nav-btn" onClick={onPrevPeriod}>
              ‹ Anterior
            </button>
            <button
              type="button"
              className="app-menu-nav-btn accent"
              onClick={() => {
                onToday()
                onClose()
              }}
            >
              Hoy
            </button>
            <button type="button" className="app-menu-nav-btn" onClick={onNextPeriod}>
              Siguiente ›
            </button>
          </div>
        </div>}

        {hasActions && (
          <div className="app-menu-section">
            <div className="app-menu-label">Acciones</div>

            {showPersonalGroup && (
              <div className={`app-menu-list${personalOpen ? ' is-expanded' : ''}`}>
                <button
                  type="button"
                  className="app-menu-item app-menu-group-toggle"
                  aria-expanded={personalOpen}
                  onClick={() => setPersonalOpen((v) => !v)}
                >
                  <span className="app-menu-item-label">Personal</span>
                  <span
                    className={`app-menu-item-chevron toggle${personalOpen ? ' open' : ''}`}
                    aria-hidden
                  >
                    ›
                  </span>
                </button>
                {personalOpen && (
                  <div className="app-menu-sublist">
                    {showPeople && (
                      <MenuItem
                        nested
                        onClick={() => {
                          onPeople()
                          onClose()
                        }}
                      >
                        Equipo
                      </MenuItem>
                    )}
                    {showVacations && (
                      <MenuItem
                        nested
                        onClick={() => {
                          onVacations()
                          onClose()
                        }}
                      >
                        Vacaciones
                      </MenuItem>
                    )}
                    {showLunch && (
                      <MenuItem
                        nested
                        onClick={() => {
                          onLunch()
                          onClose()
                        }}
                      >
                        Almuerzo
                      </MenuItem>
                    )}
                    {showVidriera && (
                      <MenuItem
                        nested
                        onClick={() => {
                          onVidriera()
                          onClose()
                        }}
                      >
                        Vidrieras
                      </MenuItem>
                    )}
                  </div>
                )}
              </div>
            )}

            {showOtherActions && (
              <div className="app-menu-list">
                {showUsers && (
                  <MenuItem
                    onClick={() => {
                      onUsers()
                      onClose()
                    }}
                  >
                    Usuarios
                  </MenuItem>
                )}
                {showTasksAdmin && (
                  <MenuItem
                    onClick={() => {
                      onTasksAdmin()
                      onClose()
                    }}
                  >
                    Administrar tareas
                  </MenuItem>
                )}
                {showAudit && (
                  <MenuItem
                    onClick={() => {
                      onAudit()
                      onClose()
                    }}
                  >
                    Bitácora
                  </MenuItem>
                )}
              </div>
            )}
          </div>
        )}

        <div className="app-menu-section app-menu-section-session">
          <div className="app-menu-list">
            {loggedIn ? (
              <>
                <MenuItem
                  onClick={() => {
                    onChangePassword?.()
                    onClose()
                  }}
                >
                  Cambiar contraseña
                </MenuItem>
                <MenuItem
                  danger
                  onClick={() => {
                    onLogout?.()
                    onClose()
                  }}
                >
                  Cerrar sesión
                </MenuItem>
              </>
            ) : (
              <MenuItem
                onClick={() => {
                  onLogin?.()
                  onClose()
                }}
              >
                Iniciar sesión
              </MenuItem>
            )}
          </div>
        </div>
      </nav>
    </div>
  )
}
