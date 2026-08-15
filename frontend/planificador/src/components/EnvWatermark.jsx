/** Marca de agua de ambiente (solo dev/test). pointer-events: none. */
export default function EnvWatermark() {
  const raw = String(import.meta.env.VITE_APP_ENV || '').trim().toLowerCase()
  const label =
    raw === 'dev' || raw === 'localenv' || raw === 'local'
      ? 'DEV'
      : raw === 'test'
        ? 'TEST'
        : null

  if (!label) return null

  return (
    <div className={`env-watermark env-watermark-${label.toLowerCase()}`} aria-hidden="true">
      <span>{label}</span>
    </div>
  )
}
