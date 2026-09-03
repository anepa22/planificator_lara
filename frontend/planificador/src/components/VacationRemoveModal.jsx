import { fmtVacationDay, fmtVacationRangeLabel } from '../lib/vacations'

export default function VacationRemoveModal({
  open,
  kind = 'vacation',
  staffName,
  workDate,
  dateFrom,
  dateTo,
  busy,
  onClose,
  onRemoveDay,
  onRemoveRange,
}) {
  if (!open) return null

  const multiDay = dateFrom && dateTo && dateFrom !== dateTo
  const title =
    kind === 'franco'
      ? 'Quitar franco'
      : kind === 'shift'
        ? 'Quitar turno'
        : 'Quitar vacaciones'

  return (
    <div
      className="overlay open"
      style={{ zIndex: 60 }}
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onClose()
      }}
    >
      <div className="modal">
        <h3>{title}</h3>
        <div className="m-sub">
          {staffName || 'Persona'}
          {multiDay
            ? ` · tramo ${fmtVacationRangeLabel(dateFrom, dateTo)}`
            : ` · ${fmtVacationDay(workDate || dateFrom)}`}
        </div>

        <div className="modal-actions vac-remove-actions">
          <button
            type="button"
            className="btn btn-ghost"
            disabled={busy}
            onClick={onClose}
          >
            Cancelar
          </button>
          {multiDay && (
            <button
              type="button"
              className="btn btn-ghost"
              disabled={busy}
              onClick={onRemoveDay}
            >
              Solo este día
            </button>
          )}
          <button
            type="button"
            className="btn btn-danger"
            disabled={busy}
            onClick={multiDay ? onRemoveRange : onRemoveDay}
          >
            {multiDay ? 'Quitar tramo' : 'Quitar'}
          </button>
        </div>
      </div>
    </div>
  )
}
