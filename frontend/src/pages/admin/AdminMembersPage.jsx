import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAdminMembers, updateMemberStatus } from '../../api/adminMemberApi.js'
import { useAuth } from '../../auth/AuthContext.jsx'
import { handleAdminRequestError } from '../../utils/adminRequestError.js'

export default function AdminMembersPage() {
  const { user, refreshUser } = useAuth()
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const [memberPage, setMemberPage] = useState(null)
  const [loading, setLoading] = useState(true)
  const [changingId, setChangingId] = useState(null)
  const [error, setError] = useState('')

  const handleError = useCallback((requestError, fallback) => handleAdminRequestError(requestError, {
    refreshUser, navigate, setError, fallback,
  }), [navigate, refreshUser])

  const loadMembers = useCallback(async () => {
    setLoading(true)
    setError('')
    try { setMemberPage(await getAdminMembers(page, status)) }
    catch (requestError) { await handleError(requestError, '회원 목록을 불러오지 못했습니다.') }
    finally { setLoading(false) }
  }, [handleError, page, status])

  useEffect(() => { loadMembers() }, [loadMembers])

  async function changeStatus(member) {
    const targetStatus = member.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
    const message = targetStatus === 'SUSPENDED'
      ? '이 회원을 정지하시겠습니까?'
      : '이 회원의 정지를 해제하시겠습니까?'
    if (!window.confirm(message)) return

    setChangingId(member.memberId)
    setError('')
    try {
      await updateMemberStatus(member.memberId, targetStatus)
      await loadMembers()
    } catch (requestError) {
      await handleError(requestError, requestError.response?.message || '회원 상태를 변경하지 못했습니다.')
    } finally { setChangingId(null) }
  }

  function changeFilter(event) {
    setStatus(event.target.value)
    setPage(0)
  }

  return <section className="admin-page">
    <div className="section-heading">
      <h1>회원 관리</h1>
      <label>상태 <select value={status} onChange={changeFilter}><option value="">전체</option><option value="ACTIVE">ACTIVE</option><option value="SUSPENDED">SUSPENDED</option><option value="WITHDRAWN">WITHDRAWN</option></select></label>
    </div>
    {error && <div className="status-message error-message"><p>{error}</p><button className="retry-button" onClick={loadMembers}>다시 시도</button></div>}
    {loading ? <p className="status-message">회원 목록을 불러오는 중입니다.</p> : memberPage?.content.length === 0 ? <p className="admin-empty">조건에 해당하는 회원이 없습니다.</p> : <div className="admin-table-wrap"><table className="admin-table">
      <thead><tr><th>아이디</th><th>닉네임</th><th>이메일</th><th>역할</th><th>상태</th><th>가입일</th><th>관리</th></tr></thead>
      <tbody>{memberPage?.content.map((member) => {
        const isSelf = member.memberId === user.memberId
        const canChange = !isSelf && (member.status === 'ACTIVE' || member.status === 'SUSPENDED')
        return <tr key={member.memberId}><td>{member.loginId}</td><td>{member.nickname}</td><td>{member.email}</td><td>{member.role}</td><td><span className={`admin-status status-${member.status.toLowerCase()}`}>{member.status}</span></td><td>{formatDate(member.createdAt)}</td><td>
          {canChange ? <button className={member.status === 'ACTIVE' ? 'small-danger-button' : 'small-primary-button'} disabled={changingId === member.memberId} onClick={() => changeStatus(member)}>{changingId === member.memberId ? '처리 중...' : member.status === 'ACTIVE' ? '정지' : '정지 해제'}</button> : <span className="admin-action-unavailable">{isSelf ? '현재 계정' : '-'}</span>}
        </td></tr>
      })}</tbody>
    </table></div>}
    {memberPage && memberPage.totalPages > 1 && <div className="pagination"><button disabled={memberPage.first} onClick={() => setPage((value) => value - 1)}>이전</button><span>{page + 1} / {memberPage.totalPages}</span><button disabled={memberPage.last} onClick={() => setPage((value) => value + 1)}>다음</button></div>}
  </section>
}

function formatDate(value) {
  return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(value))
}
