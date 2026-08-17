import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createNews, deleteNews, updateNews } from '../../api/adminNewsApi.js'
import { getNewsDetail, getNewsList } from '../../api/newsApi.js'
import { useAuth } from '../../auth/AuthContext.jsx'
import { handleAdminRequestError } from '../../utils/adminRequestError.js'

const emptyForm = { type: 'NOTICE', title: '', content: '' }

export default function AdminNewsPage() {
  const { refreshUser } = useAuth()
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [newsPage, setNewsPage] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [editingId, setEditingId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const handleError = useCallback((requestError, fallback) => handleAdminRequestError(requestError, { refreshUser, navigate, setError, fallback }), [navigate, refreshUser])

  const loadNews = useCallback(async () => {
    setLoading(true); setError('')
    try { setNewsPage(await getNewsList(page)) }
    catch (requestError) { await handleError(requestError, '새소식 목록을 불러오지 못했습니다.') }
    finally { setLoading(false) }
  }, [handleError, page])
  useEffect(() => { loadNews() }, [loadNews])

  async function handleSubmit(event) {
    event.preventDefault(); setSubmitting(true); setError('')
    try {
      if (editingId) await updateNews(editingId, form); else await createNews(form)
      setForm(emptyForm); setEditingId(null)
      if (page === 0) await loadNews(); else setPage(0)
    } catch (requestError) { await handleError(requestError, '새소식을 저장하지 못했습니다. 입력 내용을 확인해주세요.') }
    finally { setSubmitting(false) }
  }

  async function startEditing(newsId) {
    setError('')
    try {
      const detail = await getNewsDetail(newsId)
      setEditingId(newsId); setForm({ type: detail.type, title: detail.title, content: detail.content })
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (requestError) { await handleError(requestError, '새소식 상세 정보를 불러오지 못했습니다.') }
  }

  async function handleDelete(newsId) {
    if (!window.confirm('이 새소식을 삭제하시겠습니까?')) return
    setError('')
    try { await deleteNews(newsId); await loadNews() }
    catch (requestError) { await handleError(requestError, '새소식을 삭제하지 못했습니다.') }
  }

  return <section className="admin-page">
    <h1>새소식 관리</h1>
    <form className="admin-form" onSubmit={handleSubmit}>
      <h2>{editingId ? '새소식 수정' : '새소식 작성'}</h2>
      <label>유형<select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}><option value="NOTICE">공지</option><option value="PATCH_NOTE">패치 노트</option><option value="EVENT">이벤트</option></select></label>
      <label>제목<input value={form.title} maxLength="150" required onChange={(event) => setForm({ ...form, title: event.target.value })} /></label>
      <label>내용<textarea value={form.content} maxLength="10000" rows="8" required onChange={(event) => setForm({ ...form, content: event.target.value })} /></label>
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="form-actions">{editingId && <button className="small-button" type="button" onClick={() => { setEditingId(null); setForm(emptyForm) }}>취소</button>}<button className="submit-button" disabled={submitting}>{submitting ? '저장 중...' : '저장'}</button></div>
    </form>
    <section className="admin-section"><h2>새소식 목록</h2>
      {loading ? <p className="status-message">새소식 목록을 불러오는 중입니다.</p> : newsPage?.content.length === 0 ? <p className="admin-empty">등록된 새소식이 없습니다.</p> : <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>유형</th><th>제목</th><th>작성일</th><th>관리</th></tr></thead><tbody>{newsPage?.content.map((news) => <tr key={news.newsId}><td>{news.type}</td><td>{news.title}</td><td>{formatDate(news.createdAt)}</td><td className="table-actions"><button className="small-button" onClick={() => startEditing(news.newsId)}>수정</button><button className="small-danger-button" onClick={() => handleDelete(news.newsId)}>삭제</button></td></tr>)}</tbody></table></div>}
      {newsPage && newsPage.totalPages > 1 && <div className="pagination"><button disabled={newsPage.first} onClick={() => setPage((value) => value - 1)}>이전</button><span>{page + 1} / {newsPage.totalPages}</span><button disabled={newsPage.last} onClick={() => setPage((value) => value + 1)}>다음</button></div>}
    </section>
  </section>
}

function formatDate(value) { return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(value)) }
