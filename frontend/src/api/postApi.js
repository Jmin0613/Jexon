import { apiRequest } from './client.js'

export function getPosts(page = 0) {
  const searchParams = new URLSearchParams({ page: String(page) })
  return apiRequest(`/api/posts?${searchParams}`)
}

export function getPostDetail(postId) {
  return apiRequest(`/api/posts/${postId}`)
}
