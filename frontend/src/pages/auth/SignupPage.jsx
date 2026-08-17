import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { signup } from '../../api/memberApi.js'
import { useAuth } from '../../auth/AuthContext.jsx'

const initialForm = {
  loginId: '',
  password: '',
  passwordConfirm: '',
  nickname: '',
  email: '',
  name: '',
  phoneNumber: '',
}

function validate(form) {
  if (!/^[a-zA-Z0-9]{4,20}$/.test(form.loginId)) {
    return '아이디는 영문과 숫자로 4자 이상 20자 이하로 입력해주세요.'
  }
  if (form.password.length < 12 || form.password.length > 30
      || !/[A-Z]/.test(form.password)
      || !/[a-z]/.test(form.password)
      || !/\d/.test(form.password)
      || !/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/.test(form.password)) {
    return '비밀번호는 12~30자로 대문자, 소문자, 숫자, 특수문자를 포함해주세요.'
  }
  if (form.password !== form.passwordConfirm) {
    return '비밀번호가 일치하지 않습니다.'
  }
  if (form.nickname.trim().length < 2 || form.nickname.trim().length > 20) {
    return '닉네임은 2자 이상 20자 이하로 입력해주세요.'
  }
  if (!form.email || form.email.length > 255 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    return '올바른 이메일을 입력해주세요.'
  }
  if (!form.name.trim() || form.name.length > 60) {
    return '이름은 60자 이하로 입력해주세요.'
  }
  if (!/^010\d{8}$/.test(form.phoneNumber.replace(/\D/g, ''))) {
    return '전화번호는 010으로 시작하는 11자리 숫자로 입력해주세요.'
  }

  return ''
}

export default function SignupPage() {
  const { user, loading } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(initialForm)
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
    const validationError = validate(form)

    if (validationError) {
      setError(validationError)
      return
    }

    setSubmitting(true)
    setError('')

    try {
      await signup({
        ...form,
        phoneNumber: form.phoneNumber.replace(/\D/g, ''),
      })
      navigate('/login', { replace: true, state: { signupSuccess: true } })
    } catch (requestError) {
      const responseMessage = requestError.response?.message
      setError(
        [400, 409].includes(requestError.status) && typeof responseMessage === 'string'
          ? responseMessage
          : '회원가입하지 못했습니다. 입력 정보를 확인해주세요.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="auth-page">
      <form className="auth-form signup-form" onSubmit={handleSubmit} noValidate>
        <h1>회원가입</h1>
        {error && <p className="form-error" role="alert">{error}</p>}

        <label>아이디<input name="loginId" value={form.loginId} onChange={handleChange} autoComplete="username" required /></label>
        <label>비밀번호<input name="password" type="password" value={form.password} onChange={handleChange} autoComplete="new-password" required /></label>
        <label>비밀번호 확인<input name="passwordConfirm" type="password" value={form.passwordConfirm} onChange={handleChange} autoComplete="new-password" required /></label>
        <label>닉네임<input name="nickname" value={form.nickname} onChange={handleChange} required /></label>
        <label>이메일<input name="email" type="email" value={form.email} onChange={handleChange} autoComplete="email" required /></label>
        <label>이름<input name="name" value={form.name} onChange={handleChange} autoComplete="name" required /></label>
        <label>전화번호<input name="phoneNumber" type="tel" value={form.phoneNumber} onChange={handleChange} autoComplete="tel" placeholder="010-1234-5678" required /></label>

        <button className="submit-button" type="submit" disabled={submitting}>
          {submitting ? '가입 중...' : '회원가입'}
        </button>
        <p className="auth-help">이미 계정이 있나요? <Link to="/login">로그인</Link></p>
      </form>
    </section>
  )
}
