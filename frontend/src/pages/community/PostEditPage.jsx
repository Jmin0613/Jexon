import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getPostDetail, updatePost } from '../../api/postApi.js'
import { useAuth } from '../../auth/AuthContext.jsx'
import PostForm, { validatePost } from '../../components/PostForm.jsx'

export default function PostEditPage() {
  const { postId } = useParams()
  const { user, loading: authLoading, refreshUser } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ title: '', content: '' })
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!authLoading && !user) {
      navigate('/login', { replace: true, state: { authRequired: true } })
    }
  }, [authLoading, user, navigate])

  useEffect(() => {
    if (authLoading || !user) return undefined
    let active = true

    getPostDetail(postId)
      .then((post) => {
        if (!active) return
        if (post.writerId !== user.memberId) {
          navigate(`/posts/${postId}`, { replace: true })
          return
        }
        setForm({ title: post.title, content: post.content })
      })
      .catch((requestError) => {
        if (!active) return
        if (requestError.status === 404) {
          setLoadError('존재하지 않는 게시글입니다.')
        } else {
          setLoadError('게시글을 불러오지 못했습니다.')
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [authLoading, user, postId, navigate])

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
      await updatePost(postId, form)
      navigate(`/posts/${postId}`, { replace: true })
    } catch (requestError) {
      if (requestError.status === 401) {
        await refreshUser().catch(() => null)
        navigate('/login', { replace: true, state: { authRequired: true } })
      } else if (requestError.status === 403) {
        setError('게시글을 수정할 권한이 없습니다.')
      } else {
        setError('게시글을 수정하지 못했습니다.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (authLoading || !user || loading) {
    return <p className="status-message" role="status">게시글을 불러오는 중입니다.</p>
  }

  if (loadError) {
    return (
      <div className="status-message error-message" role="alert">
        <p>{loadError}</p>
        <Link className="back-link" to="/posts">목록으로</Link>
      </div>
    )
  }

  return (
    <PostForm
      heading="게시글 수정"
      form={form}
      onChange={handleChange}
      onSubmit={handleSubmit}
      submitting={submitting}
      error={error}
      cancelTo={`/posts/${postId}`}
      submitLabel="저장"
    />
  )
}
