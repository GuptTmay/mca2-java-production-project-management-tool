import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { Toaster } from 'sonner'
import AppLayout from './components/AppLayout'
import ProtectedRoute from './routes/ProtectedRoute'
import { LoginPage } from './features/auth/LoginPage'
import { RegisterPage } from './features/auth/RegisterPage'
import { DashboardPage } from './features/dashboard/DashboardPage'
import { TeamsPage } from './features/teams/TeamsPage'
import { TeamDetailPage } from './features/teams/TeamDetailPage'
import { ProjectsPage } from './features/projects/ProjectsPage'
import { ProjectDetailPage } from './features/projects/ProjectDetailPage'
import { MyTasksPage } from './features/tasks/MyTasksPage'
import { ProfilePage } from './features/profile/ProfilePage'
import { AdminUsersPage } from './features/admin/AdminUsersPage'

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<DashboardPage />} />
              <Route path="teams" element={<TeamsPage />} />
              <Route path="teams/:teamId" element={<TeamDetailPage />} />
              <Route path="projects" element={<ProjectsPage />} />
              <Route path="projects/:projectId" element={<ProjectDetailPage />} />
              <Route path="my-tasks" element={<MyTasksPage />} />
              <Route path="profile" element={<ProfilePage />} />
              <Route path="admin/users" element={<AdminUsersPage />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
        <Toaster richColors position="top-right" />
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
