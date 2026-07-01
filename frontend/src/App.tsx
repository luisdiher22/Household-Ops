import { Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { Layout } from './components/Layout'
import { LoginPage } from './pages/LoginPage'
import { DashboardPage } from './pages/DashboardPage'
import { TasksPage } from './pages/TasksPage'
import { ShoppingListPage } from './pages/ShoppingListPage'
import { ApprovalsPage } from './pages/ApprovalsPage'
import { AssistantPage } from './pages/AssistantPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/tasks" element={<TasksPage />} />
          <Route path="/shopping-list" element={<ShoppingListPage />} />
          <Route path="/approvals" element={<ApprovalsPage />} />
          <Route path="/assistant" element={<AssistantPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
