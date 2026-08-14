export default function WeekNav({ label, onPrev, onNext, onToday }) {
  return (
    <div className="week-nav">
      <button type="button" onClick={onPrev} aria-label="Anterior">
        ‹
      </button>
      <div className="label">{label}</div>
      <button type="button" onClick={onNext} aria-label="Siguiente">
        ›
      </button>
      <button type="button" className="today-btn" onClick={onToday}>
        Hoy
      </button>
    </div>
  )
}
