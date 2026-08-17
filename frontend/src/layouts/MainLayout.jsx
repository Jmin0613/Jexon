import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'

const publicNavigation = [
  { to: '/', label: 'Home', end: true },
  { to: '/download', label: 'Download' },
  { to: '/news', label: 'News' },
  { to: '/posts', label: 'Community' },
]

export default function MainLayout() {
  const { user, loading, authError, logout } = useAuth()
  const navigate = useNavigate()
  const [loggingOut, setLoggingOut] = useState(false)
  const [logoutError, setLogoutError] = useState(false)

  async function handleLogout() {
    setLoggingOut(true)
    setLogoutError(false)

    try {
      await logout()
      navigate('/')
    } catch {
      setLogoutError(true)
    } finally {
      setLoggingOut(false)
    }
  }

  return (
    <div className="site-shell">
      <header className="site-header">
        <NavLink className="brand" to="/">Jexon</NavLink>
        <nav className="main-nav" aria-label="Main navigation">
          {publicNavigation.map(({ to, label, end }) => (
            <NavLink key={to} to={to} end={end}>{label}</NavLink>
          ))}
          {!loading && !authError && !user && (
            <>
              <NavLink to="/login">Login</NavLink>
              <NavLink to="/signup">Sign Up</NavLink>
            </>
          )}
          {!loading && !authError && user && (
            <>
              {user.role === 'ADMIN' && <NavLink to="/admin">Admin</NavLink>}
              <span className="user-nickname">{user.nickname}</span>
              <button
                className="logout-button"
                type="button"
                disabled={loggingOut}
                onClick={handleLogout}
              >
                {loggingOut ? 'Logging out...' : 'Logout'}
              </button>
            </>
          )}
        </nav>
        {!loading && authError && (
          <span className="header-error">인증 상태를 확인하지 못했습니다.</span>
        )}
        {logoutError && <span className="header-error">로그아웃하지 못했습니다.</span>}
      </header>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  )
}
