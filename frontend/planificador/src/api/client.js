import { clearToken, getToken } from '../lib/authStorage'

const BASE = import.meta.env.VITE_API_URL ?? ''

export function getSyncWebSocketUrl() {
  const base = BASE.replace(/\/$/, '')
  const url = new URL(`${base}/api/ws/sync`, window.location.href)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  return url.toString()
}

class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export function actionMessage(error, fallback = 'No se pudo completar.') {
  const text = String(error?.message || fallback)
  return text.replace(/^Bad Request:\s*/i, '')
}

function authHeaders(extra = {}) {
  const headers = { ...extra }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  return headers
}

export const SESSION_ENDED_EVENT = 'planificator:session-ended'

export function notifySessionEnded(promptLogin) {
  window.dispatchEvent(
    new CustomEvent(SESSION_ENDED_EVENT, {
      detail: { promptLogin: !!promptLogin },
    }),
  )
}

function rejectIfUnauthorized(res) {
  if (res.status !== 401) return
  clearToken()
  window.dispatchEvent(new CustomEvent('planificator:unauthorized'))
}

async function errorMessage(res) {
  const err = await res.json().catch(() => ({}))
  return [err.message, err.detail].filter(Boolean).join(': ') || `Error ${res.status}`
}

async function api(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: authHeaders({
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    }),
  })
  rejectIfUnauthorized(res)
  if (!res.ok) throw new ApiError(await errorMessage(res), res.status)
  if (res.status === 204) return null
  return res.json()
}

export const login = (username, password) =>
  api('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })

export const getMe = () => api('/api/auth/me')

export function logout(token = getToken()) {
  const headers = {}
  if (token) headers.Authorization = `Bearer ${token}`
  return fetch(`${BASE}/api/auth/logout`, { method: 'POST', headers }).catch(
    () => null,
  )
}

export const changePassword = (currentPassword, newPassword) =>
  api('/api/auth/change-password', {
    method: 'POST',
    body: JSON.stringify({ currentPassword, newPassword }),
  })

export const getLocations = () => api('/api/locations')
export const getManageableLocations = () => api('/api/locations/admin')
export const createLocation = (body) =>
  api('/api/locations', { method: 'POST', body: JSON.stringify(body) })
export const updateLocation = (id, body) =>
  api(`/api/locations/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
export const deactivateLocation = (id) =>
  api(`/api/locations/${encodeURIComponent(id)}/baja`, { method: 'POST' })
export const activateLocation = (id) =>
  api(`/api/locations/${encodeURIComponent(id)}/alta`, { method: 'POST' })
export const getStaff = () => api('/api/staff')
export const getHolidays = (year) => api(`/api/holidays?year=${year}`)

export const getVidrieras = (from, to) =>
  api(`/api/vidrieras?from=${from}&to=${to}`)
export const putVidriera = (body) =>
  api('/api/vidrieras', { method: 'PUT', body: JSON.stringify(body) })
export const deleteVidriera = (locationId, workDate) =>
  api(
    `/api/vidrieras?locationId=${encodeURIComponent(locationId)}&workDate=${workDate}`,
    { method: 'DELETE' },
  )
export const getShifts = (weekStart) =>
  api(`/api/shifts?weekStart=${weekStart}`)
export const getVacationReport = (from, to) =>
  api(`/api/shifts/vacation-report?from=${from}&to=${to}`)

function hoursReportParams({ from, to, personId, locationId, lunchHours }) {
  const params = new URLSearchParams({ from, to })
  if (personId) params.set('person', personId)
  if (locationId) params.set('location', locationId)
  if (lunchHours != null && lunchHours !== '') params.set('lunch', String(lunchHours))
  return params
}

export const getHoursReport = (search) =>
  api(`/api/shifts/hours-report?${hoursReportParams(search)}`)

export async function downloadHoursReportPdf(search) {
  const res = await fetch(`${BASE}/api/shifts/hours-report.pdf?${hoursReportParams(search)}`, {
    headers: authHeaders(),
  })
  rejectIfUnauthorized(res)
  if (!res.ok) throw new ApiError(await errorMessage(res), res.status)
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `horas-${search.from}-${search.to}.pdf`
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
export async function downloadVacationReportPdf(from, to) {
  const params = new URLSearchParams({ from, to })
  const res = await fetch(`${BASE}/api/shifts/vacation-report.pdf?${params}`, {
    headers: authHeaders(),
  })
  rejectIfUnauthorized(res)
  if (!res.ok) throw new ApiError(await errorMessage(res), res.status)
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `vacaciones-${from}-${to}.pdf`
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
export const createShift = (body) =>
  api('/api/shifts', { method: 'POST', body: JSON.stringify(body) })
export const createVacations = (body) =>
  api('/api/shifts/vacations', { method: 'POST', body: JSON.stringify(body) })
export const getVacationQuota = (userId, periodYear) =>
  api(`/api/vacation-quota?userId=${userId}&periodYear=${periodYear}`)
export const saveVacationAdjustment = (body) =>
  api('/api/vacation-quota', { method: 'PUT', body: JSON.stringify(body) })
export const updateShift = (id, body) =>
  api(`/api/shifts/${id}`, { method: 'PUT', body: JSON.stringify(body) })
export const deleteShift = (id) =>
  api(`/api/shifts/${id}`, { method: 'DELETE' })

export const getTaskBoard = () => api('/api/tasks/board')
export const getTaskHistory = (id) => api(`/api/tasks/${id}/history`)
export const getTaskRetentionSettings = () =>
  api('/api/tasks/settings/retention')
export const updateTaskRetentionSettings = (verifiedRetentionDays) =>
  api('/api/tasks/settings/retention', {
    method: 'PUT',
    body: JSON.stringify({ verifiedRetentionDays }),
  })
export const getTasks = () => api('/api/tasks')
export const createTask = (body) =>
  api('/api/tasks', { method: 'POST', body: JSON.stringify(body) })
export const updateTask = (id, body) =>
  api(`/api/tasks/${id}`, { method: 'PUT', body: JSON.stringify(body) })
export const deleteTask = (id) =>
  api(`/api/tasks/${id}`, { method: 'DELETE' })
export const publishTask = (id) =>
  api(`/api/tasks/${id}/publish`, { method: 'POST' })
export const getTaskAssignees = () => api('/api/tasks/assignees')
export const assignTask = (id, userId) =>
  api(`/api/tasks/${id}/assign`, {
    method: 'POST',
    body: JSON.stringify({ userId }),
  })
export const unassignTask = (id) =>
  api(`/api/tasks/${id}/unassign`, { method: 'POST' })
export const moveTask = (id, status, blockReason = null) =>
  api(`/api/tasks/${id}/move`, {
    method: 'POST',
    body: JSON.stringify({ status, blockReason }),
  })
export const retireTask = (id) =>
  api(`/api/tasks/${id}/retire`, { method: 'POST' })

export const getUsers = () => api('/api/users')
export const createUser = (body) =>
  api('/api/users', { method: 'POST', body: JSON.stringify(body) })
export const updateUser = (id, body) =>
  api(`/api/users/${id}`, { method: 'PUT', body: JSON.stringify(body) })
export const deleteUser = (id) =>
  api(`/api/users/${id}`, { method: 'DELETE' })

export const getRoles = () => api('/api/roles')
export const getPermissions = () => api('/api/permissions')
export const updateRolePermissions = (id, permissionCodes) =>
  api(`/api/roles/${id}/permissions`, {
    method: 'PUT',
    body: JSON.stringify({ permissionCodes }),
  })

export function getAudit({ from, to, username, entityType, limit } = {}) {
  const q = new URLSearchParams()
  if (from) q.set('from', from)
  if (to) q.set('to', to)
  if (username) q.set('username', username)
  if (entityType) q.set('entityType', entityType)
  if (limit != null) q.set('limit', String(limit))
  const qs = q.toString()
  return api(`/api/audit${qs ? `?${qs}` : ''}`)
}

export const getAuditUsers = () => api('/api/audit/users')
