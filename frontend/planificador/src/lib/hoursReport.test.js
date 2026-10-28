import assert from 'node:assert/strict'
import test from 'node:test'
import {
  currentMonthRange,
  fmtReportHours,
  reportMatchesSearch,
  salesLocationOptions,
} from './hoursReport.js'

test('el 27 de octubre de 2026 abre octubre completo', () => {
  const range = currentMonthRange(new Date('2026-10-27T15:00:00-03:00'))
  assert.deepEqual(range, { from: '2026-10-01', to: '2026-10-31' })
})

test('el PDF queda deshabilitado si cambia el almuerzo o un filtro', () => {
  const report = {
    from: '2026-10-01',
    to: '2026-10-31',
    personId: '',
    locationId: '1',
    lunchHours: 1,
  }
  assert.equal(
    reportMatchesSearch(report, {
      from: '2026-10-01',
      to: '2026-10-31',
      personId: '',
      locationId: '1',
      lunchHours: 1,
    }),
    true,
  )
  assert.equal(
    reportMatchesSearch(report, { ...report, personId: '', lunchHours: 1.5 }),
    false,
  )
  assert.equal(
    reportMatchesSearch(report, { ...report, locationId: '2', lunchHours: 1 }),
    false,
  )
})

test('franco y vacaciones no están en el filtro de local', () => {
  assert.deepEqual(
    salesLocationOptions([
      { id: '1', name: 'Lara 1' },
      { id: 'franco', name: 'Franco' },
      { id: 'vacaciones', name: 'Vacaciones' },
    ]).map((location) => location.id),
    ['1'],
  )
})

test('las horas se muestran con coma', () => {
  assert.equal(fmtReportHours(9.5), '9,5')
  assert.equal(fmtReportHours(15), '15')
})
