import { toMinutes } from './dates'

/** Asigna carriles mínimos para turnos solapados */
export function assignLanes(shifts) {
  const lanesEnd = []
  const placed = []
  shifts.forEach((item) => {
    const start = toMinutes(item.startTime)
    const end = toMinutes(item.endTime)
    let lane = -1
    for (let i = 0; i < lanesEnd.length; i++) {
      if (lanesEnd[i] <= start) {
        lane = i
        break
      }
    }
    if (lane === -1) {
      lane = lanesEnd.length
      lanesEnd.push(end)
    } else {
      lanesEnd[lane] = end
    }
    placed.push({ ...item, lane })
  })
  return { placed, laneCount: Math.max(1, lanesEnd.length) }
}
