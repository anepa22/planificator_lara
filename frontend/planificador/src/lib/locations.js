export const VACATION_LOCATION_ID = 'vacaciones'
export const FRANCO_LOCATION_ID = 'franco'

function isVacationLocation(id) {
  return id === VACATION_LOCATION_ID
}

export function isFrancoLocation(id) {
  return id === FRANCO_LOCATION_ID
}

/** Vacaciones o franco: no cuentan como trabajo ni aparecen en grilla semanal. */
export function isAbsenceLocation(id) {
  return isVacationLocation(id) || isFrancoLocation(id)
}

export function workLocations(locations) {
  return (locations || []).filter((l) => !isAbsenceLocation(l.id))
}

/** Activos de venta, más un inactivo solo si el período tiene turnos suyos. */
export function gridLocations(locations, shifts) {
  const columns = workLocations(locations).filter((location) => location.active !== false)
  const known = new Set(columns.map((location) => location.id))
  for (const shift of shifts || []) {
    const id = shift?.locationId
    if (!id || known.has(id) || isAbsenceLocation(id)) continue
    known.add(id)
    columns.push({
      id,
      name: shift.locationName || id,
      color: shift.locationColor || '#5B6675',
      colorSoft: shift.locationColorSoft || '#E8EDF2',
      active: false,
    })
  }
  return columns
}

export function vacationLocation(locations) {
  return (locations || []).find((l) => isVacationLocation(l.id)) || null
}

export function francoLocation(locations) {
  return (locations || []).find((l) => isFrancoLocation(l.id)) || null
}

export function absenceLabel(locationId) {
  if (isFrancoLocation(locationId)) return 'Franco'
  if (isVacationLocation(locationId)) return 'Vacaciones'
  return 'Ausencia'
}
