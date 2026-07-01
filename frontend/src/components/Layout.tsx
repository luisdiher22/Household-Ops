import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

// ownerOnly here just hides the nav link -- it's not the security boundary.
// Deciding an approval is enforced server-side (ApprovalController requires
// the OWNER role, and specifically the request's own assigned principal);
// viewing the pending list isn't role-restricted on the backend at all,
// since a Staff member knowing "there's a $5,000 request awaiting approval"
// isn't sensitive the way approving/rejecting it would be.
const navItems = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/tasks', label: 'Tasks' },
  { to: '/inventory', label: 'Inventory' },
  { to: '/shopping-list', label: 'Shopping List' },
  { to: '/approvals', label: 'Approvals', ownerOnly: true },
  { to: '/assistant', label: 'Assistant' },
  { to: '/portfolio', label: 'Portfolio', ownerOnly: true },
]

export function Layout() {
  const { auth, logout } = useAuth()

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <div className="flex items-center gap-6">
            <span className="text-lg font-semibold text-slate-900">Household Ops</span>
            <nav className="flex gap-4">
              {navItems
                .filter((item) => !item.ownerOnly || auth?.role === 'OWNER')
                .map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) =>
                      `text-sm font-medium ${isActive ? 'text-slate-900' : 'text-slate-500 hover:text-slate-700'}`
                    }
                  >
                    {item.label}
                  </NavLink>
                ))}
            </nav>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="text-slate-600">
              {auth?.fullName} <span className="text-slate-400">({auth?.role})</span>
            </span>
            <button onClick={logout} className="rounded border border-slate-300 px-2 py-1 text-slate-600 hover:bg-slate-100">
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
