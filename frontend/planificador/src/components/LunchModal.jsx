export default function LunchModal({
  open,
  lunchHours,
  onChange,
  onClose,
}) {
  if (!open) return null

  return (
    <div
      className="overlay open"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className="modal lunch-modal">
        <h3>Almuerzo</h3>
        <div className="m-sub">
          Horas a descontar por día trabajado. Se resta de los totales
          semanales (no aplica a vacaciones ni francos).
        </div>
        <div className="field">
          <label htmlFor="lunch-hours">Horas</label>
          <input
            id="lunch-hours"
            type="number"
            min="0"
            max="4"
            step="0.25"
            inputMode="decimal"
            value={lunchHours}
            autoFocus
            onChange={(e) => onChange?.(e.target.value)}
          />
        </div>
        <div className="modal-actions">
          <button
            type="button"
            className="btn btn-primary"
            style={{ flex: 1 }}
            onClick={onClose}
          >
            Listo
          </button>
        </div>
      </div>
    </div>
  )
}
