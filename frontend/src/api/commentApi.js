import { apiRequest } from './client.js'

export function getComments(postId, page = 0) {
  const searchParams = new URLSearchParams({ page: String(page) })
  return apiRequest(`/api/posts/${postId}/comments?${searchParams}`)
}

export function createComment(postId, comment) {
  return apiRequest(`/api/posts/${postId}/comments`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(comment),
  })
}

export function updateComment(commentId, comment) {
  return apiRequest(`/api/comments/${commentId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(comment),
  })
}

export function deleteComment(commentId) {
  return apiRequest(`/api/comments/${commentId}`, { method: 'DELETE' })
}
