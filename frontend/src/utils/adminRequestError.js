export async function handleAdminRequestError(error, { refreshUser, navigate, setError, fallback }) {
  if (error.status === 401) {
    try {
      await refreshUser()
    } catch {
      // The route change below is still the safest fallback when auth refresh fails.
    }
    navigate('/login', { replace: true, state: { authRequired: true } })
    return
  }

  if (error.status === 403) {
    try {
      await refreshUser()
    } catch {
      // The server denial remains authoritative even if auth refresh fails.
    }
    navigate('/', { replace: true })
    return
  }

  setError(fallback)
}
