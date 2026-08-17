import { apiRequest } from './client.js'

export function createNews(payload) {
  return apiRequest('/api/admin/news', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function updateNews(newsId, payload) {
  return apiRequest(`/api/admin/news/${newsId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

export function deleteNews(newsId) {
  return apiRequest(`/api/admin/news/${newsId}`, { method: 'DELETE' })
}
