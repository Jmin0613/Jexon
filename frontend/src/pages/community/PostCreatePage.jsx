import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createPost } from '../../api/postApi.js'
import { useAuth } from '../../auth/AuthContext.jsx'
import PostForm, { validatePost } from '../../components/PostForm.jsx'

export default function PostCreatePage() {
  const { user, loading, refreshUser } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ title: '', content: '' })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!loading && !user) navigate('/login', { replace: true, state: { authRequired: true } })
  }, [loading, user, navigate])

  function handleChange(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const validationError = validatePost(form)
    if (validationError) {
      setError(validationError)
      return
    }

    setSubmitting(true)
    setError('')

    try {
      const response = await createPost(form)
      navigate(`/posts/${response.postId}`, { replace: true })
    } catch (requestError) {
      if (requestError.status === 401) {
        await refreshUser().catch(() => null)
        navigate('/login', { replace: true, state: { authRequired: true } })
      } else if (requestError.status === 403) {
        setError('게시글을 작성할 권한이 없습니다.')
      } else {
        setError('게시글을 작성하지 못했습니다.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (loading || !user) return null

  return (
    <PostForm
      heading="게시글 작성"
      form={form}
      onChange={handleChange}
      onSubmit={handleSubmit}
      submitting={submitting}
      error={error}
      cancelTo="/posts"
      submitLabel="등록"
    />
  )
}
