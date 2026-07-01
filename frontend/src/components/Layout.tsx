import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { usePortfolio, useSwitchHousehold } from '../hooks/usePortfolio'

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
  { to: '/staff', label: 'Staff' },
  { to: '/assistant', label: 'Assistant' },
  { to: '/portfolio', label: 'Portfolio', ownerOnly: true },
]

// Only rendered for an Owner with more than one property -- a single-property
// Owner (or any other role) has nothing to switch between.
function PropertySwitcher() {
  const { auth } = useAuth()
  const portfolio = usePortfolio()
  const switchHousehold = useSwitchHousehold()

  if (auth?.role !== 'OWNER' || !portfolio.data || portfolio.data.properties.length < 2) return null

  return (
    <select
      value={auth.householdId}
      onChange={(e) => switchHousehold.mutate(e.target.value)}
      disabled={switchHousehold.isPending}
      title="Switch which property you're viewing"
      className="rounded border border-navy/30 bg-white px-2 py-1 text-sm text-navy/90 disabled:opacity-50"
    >
      {portfolio.data.properties.map((property) => (
        <option key={property.householdId} value={property.householdId}>
          {property.name}
        </option>
      ))}
    </select>
  )
}

function HamburgerIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <line x1="3" y1="6" x2="21" y2="6" />
      <line x1="3" y1="12" x2="21" y2="12" />
      <line x1="3" y1="18" x2="21" y2="18" />
    </svg>
  )
}

export function Layout() {
  const { auth, logout } = useAuth()
  const [sidebarOpen, setSidebarOpen] = useState(true)

  return (
    <div className="flex min-h-screen bg-ivory">
      <aside
        className={`flex flex-shrink-0 flex-col overflow-hidden bg-navy transition-[width] duration-200 ${
          sidebarOpen ? 'w-56' : 'w-0'
        }`}
      >
        <div className="w-56 px-4 py-4 text-lg font-semibold text-white">Household Ops</div>
        <nav className="flex w-56 flex-col gap-1 px-2 pb-4">
          {navItems
            .filter((item) => !item.ownerOnly || auth?.role === 'OWNER')
            .map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `rounded px-3 py-2 text-sm font-medium transition-colors ${
                    isActive ? 'bg-steel text-white' : 'text-ivory/80 hover:bg-white/10 hover:text-white'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
        </nav>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="border-b border-navy/15 bg-white">
          <div className="flex items-center justify-between px-4 py-3">
            <button
              onClick={() => setSidebarOpen((open) => !open)}
              title={sidebarOpen ? 'Collapse menu' : 'Expand menu'}
              className="rounded p-1.5 text-navy hover:bg-ivory"
            >
              <HamburgerIcon />
            </button>
            <div className="flex items-center gap-3 text-sm">
              <PropertySwitcher />
              <span className="text-navy/70">
                {auth?.fullName} <span className="text-navy/40">({auth?.role})</span>
              </span>
              <button onClick={logout} className="rounded border border-navy/30 px-2 py-1 text-navy/70 hover:bg-ivory">
                Log out
              </button>
            </div>
          </div>
        </header>
        <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
