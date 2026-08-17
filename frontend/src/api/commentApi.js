import { apiRequest } from './client.js'

export function getComments(postId, page = 0) {
  const searchParams = new URLSearchParams({ page: String(page) })
  return apiRequest(`/api/posts/${postId}/comments?${searchParams}`)
}
