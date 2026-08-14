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
