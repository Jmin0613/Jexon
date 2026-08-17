import { NavLink, Outlet } from 'react-router-dom'

const navigation = [
  { to: '/admin', label: 'Dashboard', end: true },
  { to: '/admin/members', label: 'Members' },
  { to: '/admin/game-versions', label: 'Game Versions' },
  { to: '/admin/news', label: 'News' },
  { to: '/admin/download-statistics', label: 'Download Statistics' },
]

export default function AdminLayout() {
  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <NavLink className="brand" to="/admin">Jexon Admin</NavLink>
        <nav className="admin-nav" aria-label="Admin navigation">
          {navigation.map(({ to, label, end }) => (
            <NavLink key={to} to={to} end={end}>{label}</NavLink>
          ))}
        </nav>
        <NavLink className="admin-site-link" to="/">일반 사이트로 이동</NavLink>
      </aside>
      <main className="admin-content">
        <Outlet />
      </main>
    </div>
  )
}
