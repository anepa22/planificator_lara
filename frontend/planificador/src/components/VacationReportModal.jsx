import { useCallback, useEffect, useState } from 'react'
import { getVacationReport } from '../api/client'
import { fmtVacationRangeLabel } from '../lib/vacations'

function yearRange(year) {
  return { from: `${year}-01-01`, to: `${year}-12-31` }
}

export default function VacationReportModal({ open, onClose }) {
  const currentYear = new Date().getFullYear()
  const [from, setFrom] = useState(yearRange(currentYear).from)
  const [to, setTo] = useState(yearRange(currentYear).to)
  const [report, setReport] = useState(null)
  const [busy, setBusy] = useState(false)
  const [loaded, setLoaded] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async (range) => {
    setBusy(true)
    setError('')
    try {
      setReport(await getVacationReport(range.from, range.to))
    } catch (e) {
      setReport(null)
      setError(e.message)
    } finally {
      setBusy(false)
      setLoaded(true)
    }
  }, [])

  useEffect(() => {
    if (!open) return
    const range = yearRange(new Date().getFullYear())
    setFrom(range.from)
    setTo(range.to)
    setLoaded(false)
    void load(range)
  }, [open, load])

  if (!open) return null

  const people = report?.people || []
  const years = [currentYear - 1, currentYear, currentYear + 1]

  return (
    <div
      className="overlay open panel-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onClose()
      }}
    >
      <div
        className="modal panel-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="vacation-report-title"
      >
        <div className="panel-head">
          <h3 id="vacation-report-title">Reporte de vacaciones</h3>
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

        <form
          className="panel-form audit-filters vacation-report-filters"
          onSubmit={(e) => {
            e.preventDefault()
            void load({ from, to })
          }}
        >
          <div className="field">
            <label htmlFor="vacation-report-year">Año</label>
            <select
              id="vacation-report-year"
              value={years.includes(Number(from.slice(0, 4))) && from.endsWith('-01-01') && to.endsWith('-12-31')
                ? from.slice(0, 4)
                : ''}
              disabled={busy}
              onChange={(e) => {
                if (!e.target.value) return
                const range = yearRange(Number(e.target.value))
                setFrom(range.from)
                setTo(range.to)
                void load(range)
              }}
            >
              <option value="">Personalizado</option>
              {years.map((year) => (
                <option key={year} value={year}>
                  {year}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="vacation-report-from">Desde</label>
            <input
              id="vacation-report-from"
              type="date"
              value={from}
              disabled={busy}
              required
              onChange={(e) => setFrom(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="vacation-report-to">Hasta</label>
            <input
              id="vacation-report-to"
              type="date"
              value={to}
              disabled={busy}
              required
              onChange={(e) => setTo(e.target.value)}
            />
          </div>
          <div className="panel-form-actions">
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? 'Buscando…' : 'Buscar'}
            </button>
          </div>
        </form>

        {error && <div className="m-warn">{error}</div>}

        <div className="panel-toolbar">
          <span className="panel-count">
            {!loaded || busy
              ? 'Cargando…'
              : people.length === 0
                ? 'Sin vacaciones en el período'
                : `${report.totalDays} día${report.totalDays === 1 ? '' : 's'} · ${people.length} persona${people.length === 1 ? '' : 's'}`}
          </span>
        </div>

        <div className={`panel-list${busy ? ' is-busy' : ''}`}>
          {loaded && !busy && people.length === 0 ? (
            <div className="panel-empty">
              <div className="panel-empty-title">No hay vacaciones cargadas</div>
              <div>Probá con otro período.</div>
            </div>
          ) : (
            <ul className="vacation-report-list">
              {people.map((person) => (
                <li key={person.userId} className="vacation-report-person">
                  <div className="vacation-report-person-head">
                    <span className="vacation-report-name">{person.userName}</span>
                    <span className="vacation-report-days">
                      {person.days} día{person.days === 1 ? '' : 's'}
                    </span>
                  </div>
                  <ul className="vacation-report-ranges">
                    {person.ranges.map((range) => (
                      <li key={`${range.dateFrom}-${range.dateTo}`}>
                        <span>{fmtVacationRangeLabel(range.dateFrom, range.dateTo)}</span>
                        <span className="vacation-report-range-days">
                          {range.days} día{range.days === 1 ? '' : 's'}
                        </span>
                      </li>
                    ))}
                  </ul>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}
