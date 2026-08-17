import { Link } from 'react-router-dom'
import gameOverImage from '../assets/game/gameOver.png'
import gamePlayImage from '../assets/game/gamePlay.png'
import gameStartImage from '../assets/game/gameStart.png'

const journey = [
  { label: 'START', title: '게임 시작', description: '스페이스 키를 눌러 달리기를 시작하세요.', image: gameStartImage },
  { label: 'PLAY', title: '장애물 돌파', description: '타이밍에 맞춰 점프하고 점수를 쌓으세요.', image: gamePlayImage },
  { label: 'GAME OVER', title: '최고 점수 도전', description: '기록을 갱신하며 더 멀리 달려보세요.', image: gameOverImage },
]

export default function HomePage() {
  return (
    <div className="home-page">
      <section className="home-hero">
        <div className="hero-copy">
          <span className="eyebrow">JEXON ORIGINAL GAME</span>
          <h1>Jexon Dino</h1>
          <p>장애물을 뛰어넘으며 최고 점수에 도전하는 간단하고 경쾌한 공룡 러닝 게임입니다.</p>
          <div className="hero-actions">
            <Link className="hero-cta" to="/download">지금 플레이하기</Link>
            <a className="hero-secondary" href="#how-to-play">게임 살펴보기</a>
          </div>
        </div>
        <div className="hero-visual">
          <img src={gamePlayImage} alt="Jexon Dino에서 공룡이 장애물을 뛰어넘는 플레이 장면" />
          <span className="hero-score">RUN · JUMP · SCORE</span>
        </div>
      </section>

      <section className="home-features" aria-labelledby="features-title">
        <div className="section-intro">
          <span className="eyebrow">GAME FEATURES</span>
          <h2 id="features-title">가볍게 시작하고, 기록에 도전하세요</h2>
        </div>
        <div className="feature-grid">
          <article><span>01</span><h3>간단한 조작</h3><p>SPACE 키 하나로 공룡을 점프시킬 수 있습니다.</p></article>
          <article><span>02</span><h3>점수 도전</h3><p>장애물을 피하고 나만의 최고 점수를 갱신하세요.</p></article>
          <article><span>03</span><h3>바로 실행</h3><p>최신 버전을 내려받아 간단하게 게임을 시작하세요.</p></article>
        </div>
      </section>

      <section className="game-journey" id="how-to-play" aria-labelledby="journey-title">
        <div className="section-intro">
          <span className="eyebrow">HOW TO PLAY</span>
          <h2 id="journey-title">달리기는 단순하지만, 기록은 끝이 없습니다</h2>
        </div>
        <div className="journey-grid">
          {journey.map((item) => (
            <article className="journey-card" key={item.label}>
              <img src={item.image} alt={`Jexon Dino ${item.title} 화면`} />
              <div><span>{item.label}</span><h3>{item.title}</h3><p>{item.description}</p></div>
            </article>
          ))}
        </div>
      </section>

      <section className="home-final-cta">
        <div><span className="eyebrow">READY TO RUN?</span><h2>지금 Jexon Dino를 시작해보세요</h2></div>
        <Link className="hero-cta" to="/download">게임 다운로드</Link>
      </section>
    </div>
  )
}
