import { NavLink, Outlet } from 'react-router-dom'

const navigation = [
  { to: '/', label: 'Home', end: true },
  { to: '/download', label: 'Download' },
  { to: '/news', label: 'News' },
  { to: '/posts', label: 'Community' },
  { to: '/login', label: 'Login' },
]

export default function MainLayout() {
  return (
    <div className="site-shell">
      <header className="site-header">
        <NavLink className="brand" to="/">Jexon</NavLink>
        <nav className="main-nav" aria-label="Main navigation">
          {navigation.map(({ to, label, end }) => (
            <NavLink key={to} to={to} end={end}>{label}</NavLink>
          ))}
        </nav>
      </header>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  )
}
