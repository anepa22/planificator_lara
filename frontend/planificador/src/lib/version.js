import { version as pkgVersion } from '../../package.json'

/** Semver + SHA corto para el menú: "0.1.0 (a1b2c3d)" */
export function appVersionLabel() {
  const semver = String(import.meta.env.VITE_APP_VERSION || pkgVersion).trim()
  const sha = String(import.meta.env.VITE_APP_GIT_SHA || 'local').trim() || 'local'
  return `${semver} (${sha})`
}
