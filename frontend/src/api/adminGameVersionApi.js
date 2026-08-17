import { apiRequest } from './client.js'

export function getAdminGameVersions(page = 0, status = '') {
  const searchParams = new URLSearchParams({ page: String(page) })
  if (status) searchParams.set('status', status)
  return apiRequest(`/api/admin/game-versions?${searchParams}`)
}

export function getAdminGameVersion(gameVersionId) {
  return apiRequest(`/api/admin/game-versions/${gameVersionId}`)
}

export function createGameVersion(payload) {
  return apiRequest('/api/admin/game-versions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function updateGameVersion(gameVersionId, payload) {
  return apiRequest(`/api/admin/game-versions/${gameVersionId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function uploadGameFile(gameVersionId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return apiRequest(`/api/admin/game-versions/${gameVersionId}/file`, {
    method: 'POST',
    body: formData,
  })
}

export function releaseGameVersion(gameVersionId) {
  return apiRequest(`/api/admin/game-versions/${gameVersionId}/release`, {
    method: 'POST',
  })
}
