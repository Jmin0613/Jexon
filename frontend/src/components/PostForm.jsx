import { Link } from 'react-router-dom'

export function validatePost({ title, content }) {
  if (!title.trim()) return '게시글 제목을 입력해주세요.'
  if (title.length > 100) return '게시글 제목은 100자 이하로 입력해주세요.'
  if (!content.trim()) return '게시글 내용을 입력해주세요.'
  if (content.length > 5000) return '게시글 내용은 5000자 이하로 입력해주세요.'
  return ''
}

export default function PostForm({
  heading,
  form,
  onChange,
  onSubmit,
  submitting,
  error,
  cancelTo,
  submitLabel,
}) {
  return (
    <section className="community-page">
      <form className="post-form" onSubmit={onSubmit} noValidate>
        <h1>{heading}</h1>
        {error && <p className="form-error" role="alert">{error}</p>}
        <label>
          제목
          <input
            name="title"
            value={form.title}
            onChange={onChange}
            maxLength={100}
            required
          />
        </label>
        <label>
          내용
          <textarea
            name="content"
            value={form.content}
            onChange={onChange}
            maxLength={5000}
            rows={14}
            required
          />
        </label>
        <div className="form-actions">
          <button className="submit-button" type="submit" disabled={submitting}>
            {submitting ? '처리 중...' : submitLabel}
          </button>
          <Link className="cancel-link" to={cancelTo}>취소</Link>
        </div>
      </form>
    </section>
  )
}
