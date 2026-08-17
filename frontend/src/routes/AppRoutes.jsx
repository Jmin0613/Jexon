import { Route, Routes } from 'react-router-dom'
import AdminLayout from '../layouts/AdminLayout.jsx'
import AdminRoute from './AdminRoute.jsx'
import MainLayout from '../layouts/MainLayout.jsx'
import AdminDownloadStatisticsPage from '../pages/admin/AdminDownloadStatisticsPage.jsx'
import AdminGameVersionsPage from '../pages/admin/AdminGameVersionsPage.jsx'
import AdminHomePage from '../pages/admin/AdminHomePage.jsx'
import AdminMembersPage from '../pages/admin/AdminMembersPage.jsx'
import AdminNewsPage from '../pages/admin/AdminNewsPage.jsx'
import LoginPage from '../pages/auth/LoginPage.jsx'
import SignupPage from '../pages/auth/SignupPage.jsx'
import PostDetailPage from '../pages/community/PostDetailPage.jsx'
import PostCreatePage from '../pages/community/PostCreatePage.jsx'
import PostEditPage from '../pages/community/PostEditPage.jsx'
import PostListPage from '../pages/community/PostListPage.jsx'
import DownloadPage from '../pages/game/DownloadPage.jsx'
import HomePage from '../pages/HomePage.jsx'
import NewsDetailPage from '../pages/news/NewsDetailPage.jsx'
import NewsListPage from '../pages/news/NewsListPage.jsx'

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path="download" element={<DownloadPage />} />
        <Route path="news" element={<NewsListPage />} />
        <Route path="news/:newsId" element={<NewsDetailPage />} />
        <Route path="posts" element={<PostListPage />} />
        <Route path="posts/new" element={<PostCreatePage />} />
        <Route path="posts/:postId/edit" element={<PostEditPage />} />
        <Route path="posts/:postId" element={<PostDetailPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="signup" element={<SignupPage />} />
      </Route>

      <Route element={<AdminRoute />}>
        <Route path="admin" element={<AdminLayout />}>
          <Route index element={<AdminHomePage />} />
          <Route path="members" element={<AdminMembersPage />} />
          <Route path="game-versions" element={<AdminGameVersionsPage />} />
          <Route path="news" element={<AdminNewsPage />} />
          <Route path="download-statistics" element={<AdminDownloadStatisticsPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
