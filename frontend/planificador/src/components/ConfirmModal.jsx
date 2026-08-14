export default function ConfirmModal({
  open,
  title,
  message,
  confirmLabel = 'Quitar',
  busy = false,
  onClose,
  onConfirm,
}) {
  if (!open) return null

  return (
    <div
      className="overlay open"
      style={{ zIndex: 60 }}
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onClose?.()
      }}
    >
      <div className="modal login-card" role="alertdialog" aria-modal="true">
        <h3>{title}</h3>
        {message && <div className="m-sub">{message}</div>}
        <div className="modal-actions">
          <button
            type="button"
            className="btn btn-ghost"
            disabled={busy}
            onClick={onClose}
          >
            Cancelar
          </button>
          <button
            type="button"
            className="btn btn-danger"
            disabled={busy}
            onClick={onConfirm}
          >
            {busy ? '…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
