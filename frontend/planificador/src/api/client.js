import { clearToken, getToken } from '../lib/authStorage'

const BASE = import.meta.env.VITE_API_URL ?? ''

class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function api(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers,
  })

  if (res.status === 401) {
    clearToken()
    window.dispatchEvent(new CustomEvent('planificator:unauthorized'))
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    const msg = [err.message, err.detail].filter(Boolean).join(': ')
    throw new ApiError(msg || `Error ${res.status}`, res.status)
  }

  if (res.status === 204) return null
  return res.json()
}

export const login = (username, password) =>
  api('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })

export const getMe = () => api('/api/auth/me')

export const logout = () => api('/api/auth/logout', { method: 'POST' })

export const changePassword = (currentPassword, newPassword) =>
  api('/api/auth/change-password', {
    method: 'POST',
    body: JSON.stringify({ currentPassword, newPassword }),
  })

export const getLocations = () => api('/api/locations')
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
export const createShift = (body) =>
  api('/api/shifts', { method: 'POST', body: JSON.stringify(body) })
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
