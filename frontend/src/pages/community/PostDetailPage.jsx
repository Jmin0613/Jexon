import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getComments } from '../../api/commentApi.js'
import { getPostDetail } from '../../api/postApi.js'
import { formatDate } from '../../utils/formatDate.js'

export default function PostDetailPage() {
  const { postId } = useParams()
  const [post, setPost] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [commentsPage, setCommentsPage] = useState(null)
  const [commentRequest, setCommentRequest] = useState({ postId, page: 0 })
  const [commentsLoading, setCommentsLoading] = useState(true)
  const [commentsError, setCommentsError] = useState(false)

  const commentPage = commentRequest.postId === postId ? commentRequest.page : 0

  useEffect(() => {
    let active = true

    async function loadPost() {
      setLoading(true)
      setError(null)

      try {
        const data = await getPostDetail(postId)
        if (active) setPost(data)
      } catch (requestError) {
        if (active) {
          setPost(null)
          setError(requestError.status === 404 ? 'not-found' : 'request')
        }
      } finally {
        if (active) setLoading(false)
      }
    }

    loadPost()

    return () => {
      active = false
    }
  }, [postId])

  useEffect(() => {
    let active = true

    async function loadComments() {
      setCommentsLoading(true)
      setCommentsError(false)

      try {
        const data = await getComments(postId, commentPage)
        if (active) setCommentsPage(data)
      } catch {
        if (active) setCommentsError(true)
      } finally {
        if (active) setCommentsLoading(false)
      }
    }

    loadComments()

    return () => {
      active = false
    }
  }, [postId, commentPage])

  const comments = commentsPage?.content ?? []

  return (
    <section className="community-page">
      {loading && (
        <p className="status-message" role="status">
          게시글을 불러오는 중입니다.
        </p>
      )}

      {!loading && error && (
        <div className="status-message error-message" role="alert">
          <p>
            {error === 'not-found'
              ? '존재하지 않는 게시글입니다.'
              : '게시글을 불러오지 못했습니다.'}
          </p>
          <Link className="back-link" to="/posts">목록으로</Link>
        </div>
      )}

      {!loading && post && (
        <>
          <article className="post-detail">
            <header className="post-detail-header">
              <h1>{post.title}</h1>
              <div className="post-meta">
                <span>{post.writerNickname}</span>
                <span>작성일 {formatDate(post.createdAt)}</span>
                <span>수정일 {formatDate(post.updatedAt)}</span>
              </div>
            </header>
            <div className="post-content">{post.content}</div>
            <Link className="back-link" to="/posts">목록으로</Link>
          </article>

          <section className="comments-section">
            <h2>댓글</h2>

            {commentsLoading && (
              <p className="comment-status" role="status">
                댓글을 불러오는 중입니다.
              </p>
            )}

            {!commentsLoading && commentsError && (
              <p className="comment-status comment-error" role="alert">
                댓글을 불러오지 못했습니다.
              </p>
            )}

            {!commentsLoading && !commentsError && comments.length === 0 && (
              <p className="comment-status">등록된 댓글이 없습니다.</p>
            )}

            {!commentsLoading && !commentsError && comments.length > 0 && (
              <>
                <div className="comment-list">
                  {comments.map((comment) => (
                    <article className="comment" key={comment.commentId}>
                      <header>
                        <strong>{comment.writerNickname}</strong>
                        <div className="comment-dates">
                          <time dateTime={comment.createdAt}>
                            작성일 {formatDate(comment.createdAt)}
                          </time>
                          <time dateTime={comment.updatedAt}>
                            수정일 {formatDate(comment.updatedAt)}
                          </time>
                        </div>
                      </header>
                      <p>{comment.content}</p>
                    </article>
                  ))}
                </div>

                <nav className="pagination" aria-label="댓글 페이지">
                  <button
                    type="button"
                    disabled={commentsPage.first}
                    onClick={() => setCommentRequest({ postId, page: commentPage - 1 })}
                  >
                    이전
                  </button>
                  <span>{commentsPage.number + 1} / {commentsPage.totalPages}</span>
                  <button
                    type="button"
                    disabled={commentsPage.last}
                    onClick={() => setCommentRequest({ postId, page: commentPage + 1 })}
                  >
                    다음
                  </button>
                </nav>
              </>
            )}
          </section>
        </>
      )}
    </section>
  )
}
