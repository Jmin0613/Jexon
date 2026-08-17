import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getNewsList } from '../../api/newsApi.js'
import { formatDate } from '../../utils/formatDate.js'

export default function NewsListPage() {
  const [newsPage, setNewsPage] = useState(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const loadNews = useCallback(async () => {
    setLoading(true)
    setError(false)

    try {
      setNewsPage(await getNewsList(page))
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    loadNews()
  }, [loadNews])

  const newsItems = newsPage?.content ?? []

  return (
    <section className="news-page">
      <h1>새소식</h1>

      {loading && (
        <p className="status-message" role="status">
          새소식을 불러오는 중입니다.
        </p>
      )}

      {!loading && error && (
        <div className="status-message error-message" role="alert">
          <p>새소식을 불러오지 못했습니다.</p>
          <button className="retry-button" type="button" onClick={loadNews}>
            다시 시도
          </button>
        </div>
      )}

      {!loading && !error && newsItems.length === 0 && (
        <p className="status-message">등록된 새소식이 없습니다.</p>
      )}

      {!loading && !error && newsItems.length > 0 && (
        <>
          <div className="news-list" role="table" aria-label="새소식 목록">
            <div className="news-list-header" role="row">
              <span role="columnheader">유형</span>
              <span role="columnheader">제목</span>
              <span role="columnheader">작성일</span>
            </div>
            {newsItems.map((news) => (
              <div className="news-list-row" role="row" key={news.newsId}>
                <span className="news-type" role="cell">{news.type}</span>
                <Link role="cell" to={`/news/${news.newsId}`}>{news.title}</Link>
                <time role="cell" dateTime={news.createdAt}>
                  {formatDate(news.createdAt)}
                </time>
              </div>
            ))}
          </div>

          <nav className="pagination" aria-label="새소식 페이지">
            <button
              type="button"
              disabled={newsPage.first}
              onClick={() => setPage((current) => current - 1)}
            >
              이전
            </button>
            <span>{newsPage.number + 1} / {newsPage.totalPages}</span>
            <button
              type="button"
              disabled={newsPage.last}
              onClick={() => setPage((current) => current + 1)}
            >
              다음
            </button>
          </nav>
        </>
      )}
    </section>
  )
}
