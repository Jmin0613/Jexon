import { apiFileRequest, apiRequest } from './client.js'

export function getLatestGameVersion() {
  return apiRequest('/api/game-versions/latest')
}

export async function downloadLatestGame() {
  const { blob, contentDisposition } = await apiFileRequest(
    '/api/game-versions/latest/download',
  )

  return {
    blob,
    fileName: getFileName(contentDisposition),
  }
}

function getFileName(contentDisposition) {
  if (!contentDisposition) {
    return null
  }

  const encodedMatch = contentDisposition.match(/filename\*\s*=\s*(?:UTF-8'')?([^;]+)/i)

  if (encodedMatch) {
    try {
      return sanitizeFileName(decodeURIComponent(encodedMatch[1].trim().replace(/^"|"$/g, '')))
    } catch {
      // Fall through to the regular filename value.
    }
  }

  const fileNameMatch = contentDisposition.match(/filename\s*=\s*(?:"([^"]+)"|([^;]+))/i)
  const fileName = fileNameMatch?.[1] ?? fileNameMatch?.[2]?.trim()
  return fileName ? sanitizeFileName(fileName) : null
}

function sanitizeFileName(fileName) {
  return fileName.split(/[\\/]/).pop()
}
