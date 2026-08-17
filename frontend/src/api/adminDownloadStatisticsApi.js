import { apiRequest } from './client.js'

export function getDownloadSummary() {
  return apiRequest('/api/admin/download-statistics/summary')
}

export function getVersionDownloadStatistics() {
  return apiRequest('/api/admin/download-statistics/versions')
}

export function getDailyDownloadStatistics() {
  return apiRequest('/api/admin/download-statistics/daily')
}
