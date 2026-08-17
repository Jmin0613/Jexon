export async function apiRequest(path, options = {}) {
  const response = await request(path, options)

  if (response.status === 204) {
    return null
  }

  const contentType = response.headers.get('content-type')
  return contentType?.includes('application/json')
    ? response.json()
    : response.text()
}

export async function apiFileRequest(path, options = {}) {
  const response = await request(path, options)

  return {
    blob: await response.blob(),
    contentDisposition: response.headers.get('content-disposition'),
  }
}

async function request(path, options) {
  const response = await fetch(path, {
    ...options,
    credentials: 'include',
  })

  if (!response.ok) {
    throw new Error(`API request failed: ${response.status}`)
  }

  return response
}
