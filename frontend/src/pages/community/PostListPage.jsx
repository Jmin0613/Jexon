import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getPosts } from '../../api/postApi.js'
import { formatDate } from '../../utils/formatDate.js'

export default function PostListPage() {
  const [postsPage, setPostsPage] = useState(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const loadPosts = useCallback(async () => {
    setLoading(true)
    setError(false)

    try {
      setPostsPage(await getPosts(page))
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    loadPosts()
  }, [loadPosts])

  const posts = postsPage?.content ?? []

  return (
    <section className="community-page">
      <h1>커뮤니티</h1>

      {loading && (
        <p className="status-message" role="status">
          게시글을 불러오는 중입니다.
        </p>
      )}

      {!loading && error && (
        <div className="status-message error-message" role="alert">
          <p>게시글을 불러오지 못했습니다.</p>
          <button className="retry-button" type="button" onClick={loadPosts}>
            다시 시도
          </button>
        </div>
      )}

      {!loading && !error && posts.length === 0 && (
        <p className="status-message">등록된 게시글이 없습니다.</p>
      )}

      {!loading && !error && posts.length > 0 && (
        <>
          <div className="post-list" role="table" aria-label="게시글 목록">
            <div className="post-list-header" role="row">
              <span role="columnheader">제목</span>
              <span role="columnheader">작성자</span>
              <span role="columnheader">작성일</span>
            </div>
            {posts.map((post) => (
              <div className="post-list-row" role="row" key={post.postId}>
                <Link role="cell" to={`/posts/${post.postId}`}>{post.title}</Link>
                <span role="cell">{post.writerNickname}</span>
                <time role="cell" dateTime={post.createdAt}>
                  {formatDate(post.createdAt)}
                </time>
              </div>
            ))}
          </div>

          <nav className="pagination" aria-label="게시글 페이지">
            <button
              type="button"
              disabled={postsPage.first}
              onClick={() => setPage((current) => current - 1)}
            >
              이전
            </button>
            <span>{postsPage.number + 1} / {postsPage.totalPages}</span>
            <button
              type="button"
              disabled={postsPage.last}
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
