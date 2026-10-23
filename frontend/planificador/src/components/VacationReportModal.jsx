import { useCallback, useEffect, useRef, useState } from 'react'
import { downloadVacationReportPdf, getVacationReport } from '../api/client'
import { enjoymentLabel, enjoymentRange, enjoymentYear, fmtVacationRangeLabel } from '../lib/vacations'

function dayCount(days) {
  return `${days} día${days === 1 ? '' : 's'}`
}

function selectedEnjoymentYear(from, to, validYears) {
  const match = validYears.find((year) => {
    const range = enjoymentRange(year)
    return from === range.from && to === range.to
  })
  return match == null ? '' : String(match)
}

export default function VacationReportModal({ open, canPrint = false, onClose }) {
  const currentYear = enjoymentYear(new Date())
  const opening = enjoymentRange(currentYear)
  const [from, setFrom] = useState(opening.from)
  const [to, setTo] = useState(opening.to)
  const [report, setReport] = useState(null)
  const [busy, setBusy] = useState(false)
  const [printing, setPrinting] = useState(false)
  const [loaded, setLoaded] = useState(false)
  const [error, setError] = useState('')
  const requestId = useRef(0)
  const reportMatchesFilters = report?.from === from && report?.to === to

  async function printPdf() {
    if (!reportMatchesFilters) return
    setPrinting(true)
    setError('')
    try {
      await downloadVacationReportPdf(report.from, report.to)
    } catch (e) {
      setError(e.message)
    } finally {
      setPrinting(false)
    }
  }

  const load = useCallback(async (range) => {
    const currentRequest = ++requestId.current
    setBusy(true)
    setError('')
    try {
      const nextReport = await getVacationReport(range.from, range.to)
      if (currentRequest === requestId.current) setReport(nextReport)
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
    const range = enjoymentRange(enjoymentYear(new Date()))
    setFrom(range.from)
    setTo(range.to)
    setLoaded(false)
    void load(range)
    return () => {
      requestId.current += 1
    }
  }, [open, load])

  if (!open) return null

  const locked = busy || printing
  const people = report?.people || []
  const years = [currentYear - 1, currentYear, currentYear + 1]
  const selectedYear = selectedEnjoymentYear(from, to, years)

  return (
    <div
      className="overlay open panel-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget && !locked) onClose()
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
            disabled={locked}
            onClick={onClose}
            aria-label="Cerrar"
          >
            ✕
          </button>
        </div>

        <form
          className="panel-form vacation-report-filters"
          onSubmit={(e) => {
            e.preventDefault()
            void load({ from, to })
          }}
        >
          <div className="field">
            <label htmlFor="vacation-report-year">Período</label>
            <select
              id="vacation-report-year"
              value={selectedYear}
              disabled={locked}
              onChange={(e) => {
                if (!e.target.value) return
                const range = enjoymentRange(Number(e.target.value))
                setFrom(range.from)
                setTo(range.to)
                void load(range)
              }}
            >
              <option value="">Personalizado</option>
              {years.map((year) => (
                <option key={year} value={year}>
                  {enjoymentLabel(year)}
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
              disabled={locked}
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
              disabled={locked}
              required
              onChange={(e) => setTo(e.target.value)}
            />
          </div>
          <div className="panel-form-actions">
            <button type="submit" className="btn btn-primary" disabled={locked}>
              {busy ? 'Buscando…' : 'Buscar'}
            </button>
          </div>
        </form>

        {error && <div className="m-warn">{error}</div>}

        <div className="panel-toolbar">
          {canPrint && (
            <button
              type="button"
              className="btn btn-primary"
              disabled={locked || !reportMatchesFilters}
              onClick={() => void printPdf()}
            >
              {printing ? 'Generando…' : 'Imprimir PDF'}
            </button>
          )}
          <span className="panel-count">
            {!loaded || busy
              ? 'Cargando…'
              : people.length === 0
                ? 'Sin vacaciones en el período'
                : `${dayCount(report.totalDays)} · ${people.length} persona${people.length === 1 ? '' : 's'}`}
          </span>
        </div>

        <div className={`panel-list${busy ? ' is-busy' : ''}`}>
          {loaded && !busy && !error && people.length === 0 ? (
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
                    <span className="vacation-report-days">{dayCount(person.days)}</span>
                  </div>
                  {person.thisPeriod != null && (
                    <div className="vacation-report-quota">
                      <span>De este período {person.thisPeriod}</span>
                      <span>Del período anterior {person.carried}</span>
                      <span>Gozados {person.enjoyed}</span>
                      {person.extra > 0 && <span>Días extra {person.extra}</span>}
                      <span className="is-remaining">Quedan {person.remaining}</span>
                    </div>
                  )}
                  <ul className="vacation-report-ranges">
                    {person.ranges.map((range) => (
                      <li key={`${range.dateFrom}-${range.dateTo}`}>
                        <span>{fmtVacationRangeLabel(range.dateFrom, range.dateTo)}</span>
                        <span className="vacation-report-range-days">
                          {dayCount(range.days)}
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
