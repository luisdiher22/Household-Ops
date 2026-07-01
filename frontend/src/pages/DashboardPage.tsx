import { Link } from 'react-router-dom'
import { useInventoryStatus } from '../hooks/useInventory'
import { useTasks } from '../hooks/useTasks'
import { usePendingApprovals } from '../hooks/useApprovals'
import { useAuth } from '../auth/AuthContext'

function SummaryCard({ title, value, to, tone }: { title: string; value: string | number; to: string; tone: 'default' | 'warning' }) {
  return (
    <Link
      to={to}
      className={`block rounded-lg border p-5 shadow-sm transition hover:shadow-md ${
        tone === 'warning' ? 'border-amber-200 bg-amber-50' : 'border-slate-200 bg-white'
      }`}
    >
      <p className="text-sm text-slate-500">{title}</p>
      <p className={`mt-1 text-3xl font-semibold ${tone === 'warning' ? 'text-amber-700' : 'text-slate-900'}`}>{value}</p>
    </Link>
  )
}

export function DashboardPage() {
  const { auth } = useAuth()
  const inventoryStatus = useInventoryStatus()
  const tasks = useTasks()
  const approvals = usePendingApprovals()

  const openTasks = tasks.data?.filter((t) => t.status === 'OPEN' || t.status === 'IN_PROGRESS').length ?? 0

  const needsAttentionIds = new Set([
    ...(inventoryStatus.data?.expiringSoonItems.map((i) => i.id) ?? []),
    ...(inventoryStatus.data?.runningOutSoonItems.map((i) => i.id) ?? []),
  ])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Welcome back, {auth?.fullName.split(' ')[0]}</h1>
        <p className="text-sm text-slate-500">Here's what's happening with the household.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-4">
        <SummaryCard
          title="Low stock items"
          value={inventoryStatus.isLoading ? '...' : (inventoryStatus.data?.lowStockCount ?? 0)}
          to="/inventory"
          tone={(inventoryStatus.data?.lowStockCount ?? 0) > 0 ? 'warning' : 'default'}
        />
        <SummaryCard
          title="Expiring / running out soon"
          value={inventoryStatus.isLoading ? '...' : needsAttentionIds.size}
          to="/inventory"
          tone={needsAttentionIds.size > 0 ? 'warning' : 'default'}
        />
        <SummaryCard title="Open tasks" value={tasks.isLoading ? '...' : openTasks} to="/tasks" tone="default" />
        <SummaryCard
          title="Pending approvals"
          value={approvals.isLoading ? '...' : (approvals.data?.length ?? 0)}
          to="/approvals"
          tone={(approvals.data?.length ?? 0) > 0 ? 'warning' : 'default'}
        />
      </div>

      {inventoryStatus.data && inventoryStatus.data.lowStockItems.length > 0 && (
        <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Low stock right now</h2>
          <ul className="divide-y divide-slate-100">
            {inventoryStatus.data.lowStockItems.map((item) => (
              <li key={item.id} className="flex items-center justify-between py-2 text-sm">
                <span className="text-slate-700">{item.name}</span>
                <span className="text-slate-500">
                  {item.currentQuantity} {item.unit} (reorder at {item.reorderThreshold})
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
