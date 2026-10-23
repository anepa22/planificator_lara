import { useCallback, useEffect, useRef, useState } from 'react'
import { downloadVacationReportPdf, getVacationReport } from '../api/client'
import { addDays, parseDateKey } from '../lib/dates'
import {
  FIRST_ENJOYMENT_YEAR,
  enjoymentLabel,
  enjoymentRange,
  enjoymentYear,
  fmtVacationRangeLabel,
  selectableEnjoymentYears,
} from '../lib/vacations'

const MONTHS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

function monthsInRange(from, to) {
  const start = parseDateKey(from)
  const end = parseDateKey(to)
  const months = []
  const cursor = new Date(start.getFullYear(), start.getMonth(), 1)
  const last = new Date(end.getFullYear(), end.getMonth(), 1)
  while (cursor <= last && months.length < 27) {
    months.push({ year: cursor.getFullYear(), month: cursor.getMonth() })
    cursor.setMonth(cursor.getMonth() + 1)
  }
  return months
}

function vacationDaysByMonth(ranges) {
  const counts = new Map()
  for (const range of ranges || []) {
    let cursor = parseDateKey(range.dateFrom)
    const end = parseDateKey(range.dateTo)
    while (cursor <= end) {
      const key = `${cursor.getFullYear()}-${cursor.getMonth()}`
      counts.set(key, (counts.get(key) || 0) + 1)
      cursor = addDays(cursor, 1)
    }
  }
  return counts
}

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
  const years = selectableEnjoymentYears()
  const firstDay = `${FIRST_ENJOYMENT_YEAR}-10-01`
  const selectedYear = selectedEnjoymentYear(from, to, years)
  const months = report ? monthsInRange(report.from, report.to) : []

  return (
    <div
      className="overlay open panel-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget && !locked) onClose()
      }}
    >
      <div
        className="modal panel-modal vacation-report-modal"
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
              min={firstDay}
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
              min={firstDay}
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
            <div className="vacation-grid">
              {people.map((person) => {
                const byMonth = vacationDaysByMonth(person.ranges)
                const remainingClass =
                  person.remaining < 0 ? 'is-over' : 'is-remaining'
                return (
                  <article key={person.userId} className="vacation-card">
                    <header className="vacation-card-head">
                      <h4 className="vacation-report-name">{person.userName}</h4>
                      <span className="vacation-report-days">{dayCount(person.days)} tomados</span>
                    </header>
                    {person.thisPeriod != null && (
                      <div className="vacation-stats">
                        <div className="vacation-stat">
                          <span>De este período</span>
                          <strong>{person.thisPeriod}</strong>
                        </div>
                        <div className="vacation-stat">
                          <span>Del período anterior</span>
                          <strong>{person.carried}</strong>
                        </div>
                        {person.extra > 0 && (
                          <div className="vacation-stat">
                            <span>Días extra</span>
                            <strong>{person.extra}</strong>
                          </div>
                        )}
                        <div className="vacation-stat">
                          <span>Gozados</span>
                          <strong>{person.enjoyed}</strong>
                        </div>
                        <div className={`vacation-stat ${remainingClass}`}>
                          <span>Quedan</span>
                          <strong>{person.remaining}</strong>
                        </div>
                      </div>
                    )}
                    <div className="vacation-months-scroll">
                      <div
                        className="vacation-months"
                        style={{ gridTemplateColumns: `repeat(${months.length}, minmax(44px, 1fr))` }}
                      >
                        {months.map(({ year, month }, index) => {
                          const count = byMonth.get(`${year}-${month}`) || 0
                          const name = MONTHS[month]
                          const showYear = index === 0 || months[index - 1].year !== year
                          return (
                            <div
                              key={`${year}-${month}`}
                              className={`vacation-month${count ? ' is-taken' : ''}`}
                              title={`${name} ${year}: ${dayCount(count)}`}
                            >
                              <span className="label">
                                {name}
                                {showYear ? <i>{year}</i> : null}
                              </span>
                              <span className="count">{count || '–'}</span>
                            </div>
                          )
                        })}
                      </div>
                    </div>
                    <div className="vacation-ranges">
                      {person.ranges.map((range) => (
                        <span key={`${range.dateFrom}-${range.dateTo}`} className="vacation-range-chip">
                          {fmtVacationRangeLabel(range.dateFrom, range.dateTo)}
                          <em>{dayCount(range.days)}</em>
                        </span>
                      ))}
                    </div>
                  </article>
                )
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
