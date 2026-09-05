import { useEffect, useMemo, useState } from 'react'
import { fmtVidrieraDay, toDateKey, workDateKey } from '../lib/dates'

export default function VidrieraModal({
  open,
  locations = [],
  busy = false,
  initialDate,
  existing = [],
  existingLabel = '',
  loadVidrieras,
  onClose,
  onSave,
}) {
  const [workDate, setWorkDate] = useState('')
  const [selectedIds, setSelectedIds] = useState([])
  const [loading, setLoading] = useState(false)
  const [warn, setWarn] = useState('')

  const vidrieraLocations = useMemo(
    () => (locations || []).filter((l) => l.supportsVidriera),
    [locations],
  )

  const locNameById = useMemo(() => {
    const map = new Map()
    for (const l of locations || []) map.set(l.id, l.name)
    return map
  }, [locations])

  const existingByDate = useMemo(() => {
    const map = new Map()
    for (const v of existing || []) {
      const date = workDateKey(v.workDate)
      if (!date) continue
      const name = v.locationName || locNameById.get(v.locationId) || v.locationId
      const names = map.get(date) || []
      if (!names.includes(name)) names.push(name)
      map.set(date, names)
    }
    return [...map.entries()].sort((a, b) => a[0].localeCompare(b[0]))
  }, [existing, locNameById])

  useEffect(() => {
    if (!open) return
    setWarn('')
    setWorkDate(initialDate || toDateKey(new Date()))
  }, [open, initialDate])

  useEffect(() => {
    if (!open || !workDate || !loadVidrieras) {
      setSelectedIds([])
      return
    }
    let cancelled = false
    ;(async () => {
      setLoading(true)
      try {
        const rows = await loadVidrieras(workDate, workDate)
        if (cancelled) return
        setSelectedIds((rows || []).map((v) => v.locationId))
      } catch (e) {
        if (!cancelled) {
          setWarn(e.message || 'No se pudieron cargar las vidrieras')
          setSelectedIds([])
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [open, workDate, loadVidrieras])

  function toggleLoc(id) {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    )
  }

  if (!open) return null

  const hasSelection = selectedIds.length > 0

  return (
    <div
      className="overlay open"
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onClose()
      }}
    >
      <div className="modal vacation-modal">
        <h3>Cargar o modificar vidriera</h3>
        <div className="m-sub">
          Elegí el día y tildá o destildá los locales. Guardar actualiza
          lo que ya estaba cargado: suma los nuevos y quita los destildados.
        </div>

        {warn && <div className="m-warn">{warn}</div>}

        <div className="field">
          <label htmlFor="vidriera-date">Día</label>
          <input
            id="vidriera-date"
            type="date"
            value={workDate}
            disabled={busy || loading}
            onChange={(e) => setWorkDate(e.target.value)}
          />
        </div>

        {existingByDate.length > 0 && (
          <div className="field">
            <label>
              Ya cargadas{existingLabel ? ` ${existingLabel}` : ''}
            </label>
            <div className="vidriera-existing">
              {existingByDate.map(([date, names]) => (
                <button
                  type="button"
                  key={date}
                  className={`vidriera-existing-row${
                    date === workDate ? ' is-current' : ''
                  }`}
                  disabled={busy || loading}
                  onClick={() => setWorkDate(date)}
                >
                  <span className="vidriera-existing-date">
                    {fmtVidrieraDay(date)}
                  </span>
                  <span className="vidriera-existing-locs">
                    {names.join(', ')}
                  </span>
                </button>
              ))}
            </div>
          </div>
        )}

        <div className="field">
          <label>Locales</label>
          <div className="pick-list">
            {!vidrieraLocations.length && (
              <div className="pp-empty">No hay locales habilitados para vidriera.</div>
            )}
            {vidrieraLocations.map((l) => (
              <label className="pp-item" key={l.id}>
                <input
                  type="checkbox"
                  checked={selectedIds.includes(l.id)}
                  disabled={busy || loading}
                  onChange={() => toggleLoc(l.id)}
                />
                <span className="ldot" style={{ background: l.color }} />
                <span className="pp-name">{l.name}</span>
              </label>
            ))}
          </div>
        </div>

        <div className="modal-actions">
          <button
            type="button"
            className="btn btn-ghost"
            disabled={busy}
            onClick={onClose}
          >
            Cerrar
          </button>
          <button
            type="button"
            className="btn btn-primary"
            disabled={busy || loading || !workDate || !vidrieraLocations.length}
            onClick={() => onSave?.({ workDate, locationIds: selectedIds })}
          >
            {busy ? '…' : hasSelection ? 'Guardar cambios' : 'Quitar'}
          </button>
        </div>
      </div>
    </div>
  )
}
