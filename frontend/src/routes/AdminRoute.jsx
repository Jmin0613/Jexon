import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'

export default function AdminRoute() {
  const { user, loading, authError } = useAuth()

  if (loading) return <p className="status-message">관리자 권한을 확인하는 중입니다.</p>
  if (authError) return <p className="status-message error-message">인증 상태를 확인하지 못했습니다.</p>
  if (!user) return <Navigate to="/login" replace state={{ authRequired: true }} />
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />

  return <Outlet />
}
