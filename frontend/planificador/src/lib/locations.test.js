import assert from 'node:assert/strict'
import test from 'node:test'
import { gridLocations } from './locations.js'

test('una semana sin turnos de un local inactivo no muestra su columna', () => {
  const columns = gridLocations(
    [
      { id: 'lara1', name: 'Lara 1', active: true, color: '#111111' },
      { id: 'lara2', name: 'Lara 2', active: false, color: '#222222' },
    ],
    [{ locationId: 'lara1', locationName: 'Lara 1' }],
  )
  assert.deepEqual(
    columns.map((location) => location.id),
    ['lara1'],
  )
})

test('una semana con turnos muestra el local inactivo sin ofrecerlo para cargar', () => {
  const columns = gridLocations(
    [{ id: 'lara1', name: 'Lara 1', active: true, color: '#111111' }],
    [
      { locationId: 'lara2', locationName: 'Lara 2', locationColor: '#222222' },
      { locationId: 'franco', locationName: 'Franco' },
    ],
  )
  assert.deepEqual(
    columns.map((location) => [location.id, location.active !== false, location.name]),
    [
      ['lara1', true, 'Lara 1'],
      ['lara2', false, 'Lara 2'],
    ],
  )
})
