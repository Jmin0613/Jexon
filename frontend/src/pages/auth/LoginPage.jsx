import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.jsx'

export default function LoginPage() {
  const { user, loading, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ loginId: '', password: '' })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!loading && user) navigate('/', { replace: true })
  }, [loading, user, navigate])

  function handleChange(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError('')

    try {
      await login(form)
      navigate('/', { replace: true })
    } catch (requestError) {
      setError(requestError.status === 401
        ? '아이디 또는 비밀번호를 확인해주세요.'
        : '로그인하지 못했습니다. 잠시 후 다시 시도해주세요.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit}>
        <h1>로그인</h1>
        {location.state?.signupSuccess && (
          <p className="form-success">회원가입이 완료되었습니다. 로그인해주세요.</p>
        )}
        {location.state?.authRequired && (
          <p className="form-error">로그인이 필요한 기능입니다.</p>
        )}
        {error && <p className="form-error" role="alert">{error}</p>}

        <label>
          아이디
          <input
            name="loginId"
            value={form.loginId}
            onChange={handleChange}
            autoComplete="username"
            required
          />
        </label>
        <label>
          비밀번호
          <input
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange}
            autoComplete="current-password"
            required
          />
        </label>
        <button className="submit-button" type="submit" disabled={submitting}>
          {submitting ? '로그인 중...' : '로그인'}
        </button>
        <p className="auth-help">계정이 없나요? <Link to="/signup">회원가입</Link></p>
      </form>
    </section>
  )
}
