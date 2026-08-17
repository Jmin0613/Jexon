import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getNewsDetail } from '../../api/newsApi.js'
import { formatDate } from '../../utils/formatDate.js'

export default function NewsDetailPage() {
  const { newsId } = useParams()
  const [news, setNews] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true

    async function loadNewsDetail() {
      setLoading(true)
      setError(null)

      try {
        const data = await getNewsDetail(newsId)
        if (active) setNews(data)
      } catch (requestError) {
        if (active) {
          setNews(null)
          setError(requestError.status === 404 ? 'not-found' : 'request')
        }
      } finally {
        if (active) setLoading(false)
      }
    }

    loadNewsDetail()

    return () => {
      active = false
    }
  }, [newsId])

  return (
    <section className="news-page">
      {loading && (
        <p className="status-message" role="status">
          새소식을 불러오는 중입니다.
        </p>
      )}

      {!loading && error && (
        <div className="status-message error-message" role="alert">
          <p>
            {error === 'not-found'
              ? '존재하지 않는 새소식입니다.'
              : '새소식을 불러오지 못했습니다.'}
          </p>
          <Link className="back-link" to="/news">목록으로</Link>
        </div>
      )}

      {!loading && news && (
        <article className="news-detail">
          <header className="news-detail-header">
            <span className="news-type">{news.type}</span>
            <h1>{news.title}</h1>
            <div className="news-dates">
              <span>작성일 {formatDate(news.createdAt)}</span>
              <span>수정일 {formatDate(news.updatedAt)}</span>
            </div>
          </header>
          <div className="news-content">{news.content}</div>
          <Link className="back-link" to="/news">목록으로</Link>
        </article>
      )}
    </section>
  )
}
