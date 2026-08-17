import { apiRequest } from './client.js'

export function getAdminMembers(page = 0, status = '') {
  const searchParams = new URLSearchParams({ page: String(page) })
  if (status) searchParams.set('status', status)
  return apiRequest(`/api/admin/members?${searchParams}`)
}

export function updateMemberStatus(memberId, status) {
  return apiRequest(`/api/admin/members/${memberId}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  })
}
