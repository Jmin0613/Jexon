import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getDailyDownloadStatistics, getDownloadSummary, getVersionDownloadStatistics } from '../../api/adminDownloadStatisticsApi.js'
import { useAuth } from '../../auth/AuthContext.jsx'
import { handleAdminRequestError } from '../../utils/adminRequestError.js'

export default function AdminDownloadStatisticsPage() {
  const { refreshUser } = useAuth()
  const navigate = useNavigate()
  const [statistics, setStatistics] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadStatistics = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [summary, versions, daily] = await Promise.all([getDownloadSummary(), getVersionDownloadStatistics(), getDailyDownloadStatistics()])
      setStatistics({ summary, versions, daily })
    } catch (requestError) {
      await handleAdminRequestError(requestError, { refreshUser, navigate, setError, fallback: '다운로드 통계를 불러오지 못했습니다.' })
    } finally { setLoading(false) }
  }, [navigate, refreshUser])

  useEffect(() => { loadStatistics() }, [loadStatistics])

  if (loading) return <p className="status-message">다운로드 통계를 불러오는 중입니다.</p>
  if (error) return <div className="status-message error-message"><p>{error}</p><button className="retry-button" onClick={loadStatistics}>다시 시도</button></div>
  if (!statistics) return null

  return (
    <section className="admin-page">
      <h1>다운로드 통계</h1>
      <div className="stat-summary"><span>전체 다운로드</span><strong>{statistics.summary.totalDownloads.toLocaleString()}회</strong></div>
      <StatisticsTable title="버전별 다운로드" empty={statistics.versions.length === 0} headers={['버전', '상태', '다운로드']}>
        {statistics.versions.map((item) => <tr key={item.gameVersionId}><td>{item.version}</td><td><Status value={item.status} /></td><td>{item.downloadCount.toLocaleString()}회</td></tr>)}
      </StatisticsTable>
      <StatisticsTable title="일별 다운로드" empty={statistics.daily.length === 0} headers={['날짜', '다운로드']}>
        {statistics.daily.map((item) => <tr key={item.date}><td>{formatDate(item.date)}</td><td>{item.downloadCount.toLocaleString()}회</td></tr>)}
      </StatisticsTable>
    </section>
  )
}

function StatisticsTable({ title, empty, headers, children }) {
  return <section className="admin-section"><h2>{title}</h2>{empty ? <p className="admin-empty">표시할 다운로드 기록이 없습니다.</p> : <div className="admin-table-wrap"><table className="admin-table"><thead><tr>{headers.map((header) => <th key={header}>{header}</th>)}</tr></thead><tbody>{children}</tbody></table></div>}</section>
}
function Status({ value }) { return <span className={`admin-status status-${value.toLowerCase()}`}>{value}</span> }
function formatDate(value) { return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(`${value}T00:00:00`)) }
