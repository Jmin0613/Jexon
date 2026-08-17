import { apiRequest } from './client.js'

export function getPosts(page = 0) {
  const searchParams = new URLSearchParams({ page: String(page) })
  return apiRequest(`/api/posts?${searchParams}`)
}

export function getPostDetail(postId) {
  return apiRequest(`/api/posts/${postId}`)
}

export function createPost(post) {
  return apiRequest('/api/posts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(post),
  })
}

export function updatePost(postId, post) {
  return apiRequest(`/api/posts/${postId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(post),
  })
}

export function deletePost(postId) {
  return apiRequest(`/api/posts/${postId}`, { method: 'DELETE' })
}
