import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createGameVersion, getAdminGameVersion, getAdminGameVersions, releaseGameVersion, updateGameVersion, uploadGameFile } from '../../api/adminGameVersionApi.js'
import { useAuth } from '../../auth/AuthContext.jsx'
import { handleAdminRequestError } from '../../utils/adminRequestError.js'

const emptyForm = { version: '', title: '', description: '' }

export default function AdminGameVersionsPage() {
  const { refreshUser } = useAuth()
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const [versionPage, setVersionPage] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [editingId, setEditingId] = useState(null)
  const [uploads, setUploads] = useState({})
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const handleError = useCallback((requestError, fallback) => handleAdminRequestError(requestError, { refreshUser, navigate, setError, fallback }), [navigate, refreshUser])

  const loadVersions = useCallback(async () => {
    setLoading(true); setError('')
    try { setVersionPage(await getAdminGameVersions(page, status)) }
    catch (requestError) { await handleError(requestError, '게임 버전 목록을 불러오지 못했습니다.') }
    finally { setLoading(false) }
  }, [handleError, page, status])
  useEffect(() => { loadVersions() }, [loadVersions])

  async function handleSubmit(event) {
    event.preventDefault(); setSubmitting(true); setError('')
    try {
      if (editingId) await updateGameVersion(editingId, { title: form.title, description: form.description })
      else await createGameVersion(form)
      setEditingId(null); setForm(emptyForm)
      if (page === 0) await loadVersions(); else setPage(0)
    } catch (requestError) { await handleError(requestError, '게임 버전을 저장하지 못했습니다. 입력 내용과 중복 버전을 확인해주세요.') }
    finally { setSubmitting(false) }
  }

  async function startEditing(gameVersionId) {
    setError('')
    try {
      const detail = await getAdminGameVersion(gameVersionId)
      setEditingId(gameVersionId); setForm({ version: detail.version, title: detail.title, description: detail.description })
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (requestError) { await handleError(requestError, '게임 버전 상세 정보를 불러오지 못했습니다.') }
  }

  async function handleUpload(gameVersionId, file) {
    if (!file) return
    setBusyId(gameVersionId); setError('')
    try {
      const result = await uploadGameFile(gameVersionId, file)
      setUploads((current) => ({ ...current, [gameVersionId]: result }))
    } catch (requestError) { await handleError(requestError, 'ZIP 파일을 업로드하지 못했습니다. DRAFT 상태와 파일 형식을 확인해주세요.') }
    finally { setBusyId(null) }
  }

  async function handleRelease(gameVersionId) {
    if (!window.confirm('이 버전을 공개 릴리스하시겠습니까? 기존 공개 버전은 비활성화됩니다.')) return
    setBusyId(gameVersionId); setError('')
    try { await releaseGameVersion(gameVersionId); await loadVersions() }
    catch (requestError) { await handleError(requestError, '버전을 릴리스하지 못했습니다. 업로드된 게임 파일이 있는지 확인해주세요.') }
    finally { setBusyId(null) }
  }

  function changeFilter(event) { setStatus(event.target.value); setPage(0) }

  return <section className="admin-page">
    <h1>게임 버전 관리</h1>
    <form className="admin-form" onSubmit={handleSubmit}>
      <h2>{editingId ? '게임 버전 수정' : '게임 버전 생성'}</h2>
      <label>버전<input value={form.version} disabled={Boolean(editingId)} placeholder="v1.0.0" pattern="v\d+\.\d+\.\d+" maxLength="30" required onChange={(event) => setForm({ ...form, version: event.target.value })} /></label>
      <label>제목<input value={form.title} minLength="10" maxLength="100" required onChange={(event) => setForm({ ...form, title: event.target.value })} /></label>
      <label>설명<textarea value={form.description} minLength="10" maxLength="500" rows="5" required onChange={(event) => setForm({ ...form, description: event.target.value })} /></label>
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="form-actions">{editingId && <button className="small-button" type="button" onClick={() => { setEditingId(null); setForm(emptyForm) }}>취소</button>}<button className="submit-button" disabled={submitting}>{submitting ? '저장 중...' : '저장'}</button></div>
    </form>

    <section className="admin-section">
      <div className="section-heading"><h2>버전 목록</h2><label>상태 <select value={status} onChange={changeFilter}><option value="">전체</option><option value="DRAFT">DRAFT</option><option value="RELEASED">RELEASED</option><option value="INACTIVE">INACTIVE</option></select></label></div>
      <p className="admin-hint">파일 조회 API가 없어 업로드 정보는 이 화면에서 성공한 직후에만 표시됩니다.</p>
      {loading ? <p className="status-message">게임 버전 목록을 불러오는 중입니다.</p> : versionPage?.content.length === 0 ? <p className="admin-empty">해당 상태의 게임 버전이 없습니다.</p> : <div className="admin-version-list">{versionPage?.content.map((item) => <article className="admin-version-card" key={item.gameVersionId}>
        <div className="version-card-heading"><div><h3>{item.title}</h3><strong>{item.version}</strong></div><Status value={item.status} /></div>
        <dl><div><dt>생성일</dt><dd>{formatDate(item.createdAt)}</dd></div><div><dt>릴리스일</dt><dd>{item.releasedAt ? formatDate(item.releasedAt) : '-'}</dd></div></dl>
        {uploads[item.gameVersionId] && <p className="upload-result">업로드 완료: {uploads[item.gameVersionId].originalFileName} ({formatBytes(uploads[item.gameVersionId].fileSize)})</p>}
        <div className="table-actions"><button className="small-button" disabled={busyId === item.gameVersionId} onClick={() => startEditing(item.gameVersionId)}>수정</button>
          {item.status === 'DRAFT' && <label className="file-button">{busyId === item.gameVersionId ? '처리 중...' : 'ZIP 업로드'}<input type="file" accept=".zip,application/zip" disabled={busyId === item.gameVersionId} onChange={(event) => handleUpload(item.gameVersionId, event.target.files[0])} /></label>}
          {(item.status === 'DRAFT' || item.status === 'INACTIVE') && <button className="small-primary-button" disabled={busyId === item.gameVersionId} onClick={() => handleRelease(item.gameVersionId)}>릴리스</button>}
        </div>
      </article>)}</div>}
      {versionPage && versionPage.totalPages > 1 && <div className="pagination"><button disabled={versionPage.first} onClick={() => setPage((value) => value - 1)}>이전</button><span>{page + 1} / {versionPage.totalPages}</span><button disabled={versionPage.last} onClick={() => setPage((value) => value + 1)}>다음</button></div>}
    </section>
  </section>
}

function Status({ value }) { return <span className={`admin-status status-${value.toLowerCase()}`}>{value}</span> }
function formatDate(value) { return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(value)) }
function formatBytes(bytes) { if (bytes === 0) return '0 B'; const units = ['B', 'KB', 'MB', 'GB']; const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1); return `${(bytes / (1024 ** index)).toFixed(index === 0 ? 0 : 2)} ${units[index]}` }
