import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  createComment,
  deleteComment,
  getComments,
  updateComment,
} from '../../api/commentApi.js'
import { deletePost, getPostDetail } from '../../api/postApi.js'
import { useAuth } from '../../auth/AuthContext.jsx'
import { formatDate } from '../../utils/formatDate.js'

export default function PostDetailPage() {
  const { postId } = useParams()
  const { user, loading: authLoading, refreshUser } = useAuth()
  const navigate = useNavigate()
  const [post, setPost] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [commentsPage, setCommentsPage] = useState(null)
  const [commentRequest, setCommentRequest] = useState({ postId, page: 0 })
  const [commentsLoading, setCommentsLoading] = useState(true)
  const [commentsError, setCommentsError] = useState(false)
  const [commentsReloadKey, setCommentsReloadKey] = useState(0)
  const [postActionError, setPostActionError] = useState('')
  const [deletingPost, setDeletingPost] = useState(false)
  const [commentContent, setCommentContent] = useState('')
  const [commentFormError, setCommentFormError] = useState('')
  const [commentSubmitting, setCommentSubmitting] = useState(false)
  const [editingCommentId, setEditingCommentId] = useState(null)
  const [editContent, setEditContent] = useState('')
  const [commentActionError, setCommentActionError] = useState('')
  const [commentBusyId, setCommentBusyId] = useState(null)

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
  }, [postId, commentPage, commentsReloadKey])

  const comments = commentsPage?.content ?? []
  const isPostAuthor = Boolean(user && post && post.writerId === user.memberId)

  async function handleMutationError(requestError, setMessage, fallbackMessage, forbiddenMessage) {
    if (requestError.status === 401) {
      await refreshUser().catch(() => null)
      navigate('/login', { state: { authRequired: true } })
      return
    }

    setMessage(requestError.status === 403 ? forbiddenMessage : fallbackMessage)
  }

  async function handlePostDelete() {
    if (!window.confirm('게시글을 삭제하시겠습니까?')) return

    setDeletingPost(true)
    setPostActionError('')

    try {
      await deletePost(postId)
      navigate('/posts', { replace: true })
    } catch (requestError) {
      await handleMutationError(
        requestError,
        setPostActionError,
        '게시글을 삭제하지 못했습니다.',
        '게시글을 삭제할 권한이 없습니다.',
      )
    } finally {
      setDeletingPost(false)
    }
  }

  async function handleCommentCreate(event) {
    event.preventDefault()
    if (!commentContent.trim()) {
      setCommentFormError('댓글 내용을 입력해주세요.')
      return
    }
    if (commentContent.length > 1000) {
      setCommentFormError('댓글 내용은 1000자 이하로 입력해주세요.')
      return
    }

    setCommentSubmitting(true)
    setCommentFormError('')

    try {
      await createComment(postId, { content: commentContent })
      setCommentContent('')
      if (commentPage === 0) {
        setCommentsReloadKey((current) => current + 1)
      } else {
        setCommentRequest({ postId, page: 0 })
      }
    } catch (requestError) {
      await handleMutationError(
        requestError,
        setCommentFormError,
        '댓글을 등록하지 못했습니다.',
        '댓글을 작성할 권한이 없습니다.',
      )
    } finally {
      setCommentSubmitting(false)
    }
  }

  function startCommentEdit(comment) {
    setEditingCommentId(comment.commentId)
    setEditContent(comment.content)
    setCommentActionError('')
  }

  async function handleCommentUpdate(commentId) {
    if (!editContent.trim()) {
      setCommentActionError('댓글 내용을 입력해주세요.')
      return
    }
    if (editContent.length > 1000) {
      setCommentActionError('댓글 내용은 1000자 이하로 입력해주세요.')
      return
    }

    setCommentBusyId(commentId)
    setCommentActionError('')

    try {
      await updateComment(commentId, { content: editContent })
      setEditingCommentId(null)
      setEditContent('')
      setCommentsReloadKey((current) => current + 1)
    } catch (requestError) {
      await handleMutationError(
        requestError,
        setCommentActionError,
        '댓글을 수정하지 못했습니다.',
        '댓글을 수정할 권한이 없습니다.',
      )
    } finally {
      setCommentBusyId(null)
    }
  }

  async function handleCommentDelete(commentId) {
    if (!window.confirm('댓글을 삭제하시겠습니까?')) return

    setCommentBusyId(commentId)
    setCommentActionError('')

    try {
      await deleteComment(commentId)
      if (comments.length === 1 && commentPage > 0) {
        setCommentRequest({ postId, page: commentPage - 1 })
      } else {
        setCommentsReloadKey((current) => current + 1)
      }
    } catch (requestError) {
      await handleMutationError(
        requestError,
        setCommentActionError,
        '댓글을 삭제하지 못했습니다.',
        '댓글을 삭제할 권한이 없습니다.',
      )
    } finally {
      setCommentBusyId(null)
    }
  }

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
            {postActionError && <p className="form-error" role="alert">{postActionError}</p>}
            <div className="detail-actions">
              <Link className="back-link" to="/posts">목록으로</Link>
              {!authLoading && isPostAuthor && (
                <>
                  <Link className="secondary-link" to={`/posts/${postId}/edit`}>수정</Link>
                  <button
                    className="danger-button"
                    type="button"
                    disabled={deletingPost}
                    onClick={handlePostDelete}
                  >
                    {deletingPost ? '삭제 중...' : '삭제'}
                  </button>
                </>
              )}
            </div>
          </article>

          <section className="comments-section">
            <h2>댓글</h2>

            {!authLoading && user && (
              <form className="comment-form" onSubmit={handleCommentCreate} noValidate>
                <label htmlFor="new-comment">댓글 내용</label>
                <textarea
                  id="new-comment"
                  value={commentContent}
                  onChange={(event) => setCommentContent(event.target.value)}
                  maxLength={1000}
                  rows={4}
                />
                {commentFormError && <p className="comment-error" role="alert">{commentFormError}</p>}
                <button className="submit-button" type="submit" disabled={commentSubmitting}>
                  {commentSubmitting ? '등록 중...' : '댓글 등록'}
                </button>
              </form>
            )}

            {!authLoading && !user && (
              <p className="login-notice">
                댓글 작성은 로그인이 필요합니다. <Link to="/login">로그인</Link>
              </p>
            )}

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
                {commentActionError && (
                  <p className="comment-error" role="alert">{commentActionError}</p>
                )}
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
                      {editingCommentId === comment.commentId ? (
                        <div className="comment-edit">
                          <textarea
                            value={editContent}
                            onChange={(event) => setEditContent(event.target.value)}
                            maxLength={1000}
                            rows={4}
                          />
                          <div className="comment-actions">
                            <button
                              className="small-primary-button"
                              type="button"
                              disabled={commentBusyId === comment.commentId}
                              onClick={() => handleCommentUpdate(comment.commentId)}
                            >
                              저장
                            </button>
                            <button
                              className="small-button"
                              type="button"
                              disabled={commentBusyId === comment.commentId}
                              onClick={() => {
                                setEditingCommentId(null)
                                setEditContent('')
                                setCommentActionError('')
                              }}
                            >
                              취소
                            </button>
                          </div>
                        </div>
                      ) : (
                        <>
                          <p>{comment.content}</p>
                          {!authLoading && user?.memberId === comment.writerId && (
                            <div className="comment-actions">
                              <button
                                className="small-button"
                                type="button"
                                onClick={() => startCommentEdit(comment)}
                              >
                                수정
                              </button>
                              <button
                                className="small-danger-button"
                                type="button"
                                disabled={commentBusyId === comment.commentId}
                                onClick={() => handleCommentDelete(comment.commentId)}
                              >
                                삭제
                              </button>
                            </div>
                          )}
                        </>
                      )}
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
