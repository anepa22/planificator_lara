import { useCallback, useEffect, useRef, useState } from 'react'
import { downloadHoursReportPdf, getHoursReport } from '../api/client'
import {
  currentMonthRange,
  fmtReportHours,
  reportMatchesSearch,
  salesLocationOptions,
} from '../lib/hoursReport'

export default function HoursReportModal({ open, lunchHours = 1, onClose }) {
  const opening = currentMonthRange()
  const [from, setFrom] = useState(opening.from)
  const [to, setTo] = useState(opening.to)
  const [personId, setPersonId] = useState('')
  const [locationId, setLocationId] = useState('')
  const [report, setReport] = useState(null)
  const [busy, setBusy] = useState(false)
  const [printing, setPrinting] = useState(false)
  const [loaded, setLoaded] = useState(false)
  const [error, setError] = useState('')
  const requestId = useRef(0)
  const lunchRef = useRef(lunchHours)
  const previousLunch = useRef(lunchHours)
  lunchRef.current = lunchHours

  const search = { from, to, personId, locationId, lunchHours }
  const matches = reportMatchesSearch(report, search)

  const load = useCallback(async (next) => {
    const currentRequest = ++requestId.current
    setBusy(true)
    setError('')
    try {
      const result = await getHoursReport(next)
      if (currentRequest === requestId.current) setReport(result)
    } catch (e) {
      if (currentRequest === requestId.current) {
        setReport(null)
        setError(e.message)
      }
    } finally {
      if (currentRequest === requestId.current) {
        setBusy(false)
        setLoaded(true)
      }
    }
  }, [])

  useEffect(() => {
    if (!open) return
    const range = currentMonthRange()
    setFrom(range.from)
    setTo(range.to)
    setPersonId('')
    setLocationId('')
    setLoaded(false)
    void load({ ...range, personId: '', locationId: '', lunchHours: lunchRef.current })
    return () => {
      requestId.current += 1
    }
  }, [open, load])

  useEffect(() => {
    if (!open) {
      previousLunch.current = lunchHours
      return
    }
    if (Number(previousLunch.current) === Number(lunchHours)) return
    previousLunch.current = lunchHours
    if (!report) return
    if (report.from !== from || report.to !== to) return
    if ((report.personId || '') !== personId || (report.locationId || '') !== locationId) return
    void load({ from, to, personId, locationId, lunchHours })
  }, [open, report, from, to, personId, locationId, lunchHours, load])

  async function printPdf() {
    if (!matches || !report) return
    setPrinting(true)
    setError('')
    try {
      await downloadHoursReportPdf({
        from: report.from,
        to: report.to,
        personId: report.personId || '',
        locationId: report.locationId || '',
        lunchHours: report.lunchHours,
      })
    } catch (e) {
      setError(e.message)
    } finally {
      setPrinting(false)
    }
  }

  if (!open) return null

  const locked = busy || printing
  const people = report?.people || []
  const personOptions = report?.personOptions || []
  const locationOptions = salesLocationOptions(report?.locationOptions)
  const lunchLabel = fmtReportHours(lunchHours)

  return (
    <div
      className="overlay open panel-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget && !locked) onClose()
      }}
    >
      <div
        className="modal panel-modal hours-report-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="hours-report-title"
      >
        <div className="panel-head">
          <h3 id="hours-report-title">Reporte de horas</h3>
          <button
            type="button"
            className="panel-close"
            disabled={locked}
            onClick={onClose}
            aria-label="Cerrar"
          >
            ✕
          </button>
        </div>

        <form
          className="panel-form hours-report-filters"
          onSubmit={(e) => {
            e.preventDefault()
            void load(search)
          }}
        >
          <div className="field">
            <label htmlFor="hours-report-from">Desde</label>
            <input
              id="hours-report-from"
              type="date"
              value={from}
              disabled={locked}
              required
              onChange={(e) => setFrom(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="hours-report-to">Hasta</label>
            <input
              id="hours-report-to"
              type="date"
              value={to}
              disabled={locked}
              required
              onChange={(e) => setTo(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="hours-report-person">Persona</label>
            <select
              id="hours-report-person"
              value={personId}
              disabled={locked}
              onChange={(e) => setPersonId(e.target.value)}
            >
              <option value="">Todas</option>
              {personOptions.map((person) => (
                <option key={person.id} value={person.id}>
                  {person.name}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="hours-report-location">Local</label>
            <select
              id="hours-report-location"
              value={locationId}
              disabled={locked}
              onChange={(e) => setLocationId(e.target.value)}
            >
              <option value="">Todos</option>
              {locationOptions.map((location) => (
                <option key={location.id} value={location.id}>
                  {location.name}
                </option>
              ))}
            </select>
          </div>
          <div className="panel-form-actions">
            <button type="submit" className="btn btn-primary" disabled={locked}>
              {busy ? 'Buscando…' : 'Buscar'}
            </button>
          </div>
        </form>

        {error && <div className="m-warn">{error}</div>}

        <div className="panel-toolbar">
          <button
            type="button"
            className="btn btn-primary"
            disabled={locked || !matches}
            onClick={() => void printPdf()}
          >
            {printing ? 'Generando…' : 'Imprimir PDF'}
          </button>
          <span className="panel-count">
            {!loaded || busy
              ? 'Cargando…'
              : `Total ${fmtReportHours(report?.totalHours)} h · menos ${lunchLabel} h de almuerzo por día trabajado`}
          </span>
        </div>

        <div className={`panel-list${busy ? ' is-busy' : ''}`}>
          {loaded && !busy && !error && people.length === 0 ? (
            <div className="panel-empty">
              <div className="panel-empty-title">No hay horas en el período</div>
              <div>Probá con otro rango.</div>
            </div>
          ) : (
            <div className="vacation-grid">
              {people.map((person) => (
                <article key={person.userId} className="vacation-card">
                  <header className="vacation-card-head">
                    <h4 className="vacation-report-name">{person.userName}</h4>
                    <span className="vacation-report-days">{fmtReportHours(person.hours)} h</span>
                  </header>
                </article>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
