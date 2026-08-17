import { apiRequest } from './client.js'

export function signup(member) {
  return apiRequest('/api/members/signup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(member),
  })
}
