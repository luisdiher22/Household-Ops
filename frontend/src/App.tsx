import { Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { Layout } from './components/Layout'
import { LoginPage } from './pages/LoginPage'
import { DashboardPage } from './pages/DashboardPage'
import { TasksPage } from './pages/TasksPage'
import { InventoryPage } from './pages/InventoryPage'
import { ShoppingListPage } from './pages/ShoppingListPage'
import { ApprovalsPage } from './pages/ApprovalsPage'
import { AssistantPage } from './pages/AssistantPage'
import { PortfolioPage } from './pages/PortfolioPage'
import { StaffPage } from './pages/StaffPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/tasks" element={<TasksPage />} />
          <Route path="/inventory" element={<InventoryPage />} />
          <Route path="/shopping-list" element={<ShoppingListPage />} />
          <Route path="/approvals" element={<ApprovalsPage />} />
          <Route path="/assistant" element={<AssistantPage />} />
          <Route path="/portfolio" element={<PortfolioPage />} />
          <Route path="/staff" element={<StaffPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
