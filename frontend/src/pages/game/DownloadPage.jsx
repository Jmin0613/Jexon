import { useCallback, useEffect, useRef, useState } from 'react'
import {
  downloadLatestGame,
  getLatestGameVersion,
} from '../../api/gameVersionApi.js'
import { formatDate } from '../../utils/formatDate.js'

function formatFileSize(bytes) {
  if (!Number.isFinite(bytes) || bytes < 0) {
    return '-'
  }

  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let unitIndex = 0

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex += 1
  }

  const fractionDigits = unitIndex === 0 ? 0 : 2
  return `${size.toFixed(fractionDigits)} ${units[unitIndex]}`
}

export default function DownloadPage() {
  const [latestVersion, setLatestVersion] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const [downloadError, setDownloadError] = useState(false)
  const downloadInProgressRef = useRef(false)

  const loadLatestVersion = useCallback(async () => {
    setLoading(true)
    setError(false)

    try {
      const data = await getLatestGameVersion()
      setLatestVersion(data)
    } catch {
      setLatestVersion(null)
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadLatestVersion()
  }, [loadLatestVersion])

  async function handleDownload() {
    if (!latestVersion || downloadInProgressRef.current) {
      return
    }

    downloadInProgressRef.current = true
    setDownloading(true)
    setDownloadError(false)

    try {
      const { blob, fileName } = await downloadLatestGame()
      const objectUrl = URL.createObjectURL(blob)
      const anchor = document.createElement('a')

      try {
        anchor.href = objectUrl
        anchor.download = fileName || latestVersion.originalFileName || 'jexon-game.zip'
        document.body.appendChild(anchor)
        anchor.click()
      } finally {
        anchor.remove()
        URL.revokeObjectURL(objectUrl)
      }
    } catch {
      setDownloadError(true)
    } finally {
      downloadInProgressRef.current = false
      setDownloading(false)
    }
  }

  return (
    <section className="download-page">
      <h1>Download</h1>

      {loading && (
        <p className="status-message" role="status">
          최신 버전 정보를 불러오는 중입니다.
        </p>
      )}

      {!loading && error && (
        <div className="status-message error-message" role="alert">
          <p>최신 버전 정보를 불러오지 못했습니다.</p>
          <button className="retry-button" type="button" onClick={loadLatestVersion}>
            다시 시도
          </button>
        </div>
      )}

      {!loading && latestVersion && (
        <article className="version-card">
          <div className="version-heading">
            <h2>{latestVersion.title}</h2>
            <span className="version-badge">{latestVersion.version}</span>
          </div>

          <p className="version-description">{latestVersion.description}</p>

          <dl className="version-details">
            <div>
              <dt>출시일</dt>
              <dd>{formatDate(latestVersion.releasedAt)}</dd>
            </div>
            <div>
              <dt>파일명</dt>
              <dd>{latestVersion.originalFileName}</dd>
            </div>
            <div>
              <dt>파일 크기</dt>
              <dd>{formatFileSize(latestVersion.fileSize)}</dd>
            </div>
            <div>
              <dt>SHA-256</dt>
              <dd className="checksum">{latestVersion.checksum}</dd>
            </div>
          </dl>

          {downloadError && (
            <p className="download-error" role="alert">
              게임 파일을 다운로드하지 못했습니다.
            </p>
          )}

          <button
            className="download-button"
            type="button"
            onClick={handleDownload}
            disabled={downloading}
          >
            {downloading ? '다운로드 준비 중...' : '게임 다운로드'}
          </button>
        </article>
      )}
    </section>
  )
}
