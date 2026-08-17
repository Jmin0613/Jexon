import { Link } from 'react-router-dom'

const cards = [
  { to: '/admin/members', title: '회원 관리', description: '회원 관리 API 제공 상태를 확인합니다.' },
  { to: '/admin/game-versions', title: '게임 버전 관리', description: '버전을 생성하고 ZIP 파일을 업로드·릴리스합니다.' },
  { to: '/admin/news', title: '새소식 관리', description: '공지, 패치 노트, 이벤트를 작성하고 관리합니다.' },
  { to: '/admin/download-statistics', title: '다운로드 통계', description: '전체·버전별·일별 다운로드 수를 확인합니다.' },
]

export default function AdminHomePage() {
  return (
    <section className="admin-page">
      <h1>관리자 대시보드</h1>
      <div className="admin-card-grid">
        {cards.map((card) => (
          <Link className="admin-card" to={card.to} key={card.to}>
            <h2>{card.title}</h2>
            <p>{card.description}</p>
          </Link>
        ))}
      </div>
    </section>
  )
}
