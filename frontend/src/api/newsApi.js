import { apiRequest } from './client.js'

export function getNewsList(page = 0) {
  const searchParams = new URLSearchParams({ page: String(page) })
  return apiRequest(`/api/news?${searchParams}`)
}

export function getNewsDetail(newsId) {
  return apiRequest(`/api/news/${newsId}`)
}
