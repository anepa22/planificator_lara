const STAFF_PALETTE = [
  { c: '#0D9488', soft: '#CCFBF1', ink: '#115E59' },
  { c: '#2563EB', soft: '#DBEAFE', ink: '#1E40AF' },
  { c: '#C026D3', soft: '#FAE8FF', ink: '#86198F' },
  { c: '#D97706', soft: '#FEF3C7', ink: '#92400E' },
  { c: '#DC2626', soft: '#FEE2E2', ink: '#991B1B' },
  { c: '#059669', soft: '#D1FAE5', ink: '#065F46' },
  { c: '#7C3AED', soft: '#EDE9FE', ink: '#5B21B6' },
  { c: '#0891B2', soft: '#CFFAFE', ink: '#155E75' },
  { c: '#DB2777', soft: '#FCE7F3', ink: '#9D174D' },
  { c: '#4F46E5', soft: '#E0E7FF', ink: '#3730A3' },
  { c: '#CA8A04', soft: '#FEF9C3', ink: '#854D0E' },
  { c: '#0F766E', soft: '#CCFBF1', ink: '#134E4A' },
]

export function initials(name) {
  const parts = String(name || '').trim().split(/\s+/)
  return ((parts[0]?.[0] || '') + (parts[1]?.[0] || '')).toUpperCase()
}

export function paletteFor(userId, staff) {
  const idx = (staff || []).findIndex((s) => s.id === userId)
  return STAFF_PALETTE[(idx >= 0 ? idx : 0) % STAFF_PALETTE.length]
}
