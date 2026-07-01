import { useAuth } from '../auth/AuthContext'
import { usePortfolio, useSwitchHousehold } from '../hooks/usePortfolio'

function formatCurrency(value: number): string {
  return value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function PortfolioPage() {
  const { auth } = useAuth()
  const portfolio = usePortfolio()
  const switchHousehold = useSwitchHousehold()

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-navy">Portfolio</h1>
        <p className="mt-1 text-sm text-navy/60">
          A cross-property summary for households you have access to. Switch properties here or from the
          dropdown next to your name to act on that property's tasks, inventory, and approvals.
        </p>
      </div>

      {portfolio.isLoading && <p className="text-sm text-navy/60">Loading...</p>}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {portfolio.data?.properties.map((property) => {
          const isActive = property.householdId === auth?.householdId
          return (
          <div key={property.householdId} className="rounded-lg border border-navy/15 bg-white p-4 shadow-sm">
            <div className="flex items-start justify-between">
              <div>
                <h2 className="text-sm font-semibold text-navy">{property.name}</h2>
                <p className="text-xs text-navy/60">{property.address}</p>
              </div>
              {isActive ? (
                <span className="rounded bg-navy/10 px-1.5 py-0.5 text-xs font-medium text-navy/70">currently viewing</span>
              ) : (
                <button
                  onClick={() => switchHousehold.mutate(property.householdId)}
                  disabled={switchHousehold.isPending}
                  className="rounded border border-navy/30 px-2 py-1 text-xs text-navy/90 hover:bg-ivory disabled:opacity-50"
                >
                  Switch here
                </button>
              )}
            </div>
            <dl className="mt-3 grid grid-cols-3 gap-2 text-center">
              <div>
                <dt className="text-xs text-navy/60">Inventory value</dt>
                <dd className="text-sm font-semibold text-navy">${formatCurrency(property.inventoryValue)}</dd>
              </div>
              <div>
                <dt className="text-xs text-navy/60">Low stock</dt>
                <dd className={`text-sm font-semibold ${property.lowStockCount > 0 ? 'text-amber-700' : 'text-navy'}`}>
                  {property.lowStockCount}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-navy/60">Pending approvals</dt>
                <dd className={`text-sm font-semibold ${property.pendingApprovalCount > 0 ? 'text-red-700' : 'text-navy'}`}>
                  {property.pendingApprovalCount}
                </dd>
              </div>
            </dl>
          </div>
          )
        })}
      </div>
    </div>
  )
}
