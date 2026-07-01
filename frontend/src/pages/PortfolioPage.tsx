import { usePortfolio } from '../hooks/usePortfolio'

function formatCurrency(value: number): string {
  return value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function PortfolioPage() {
  const portfolio = usePortfolio()

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Portfolio</h1>
        <p className="mt-1 text-sm text-slate-500">
          A cross-property summary for households you have access to beyond your own. This is read-only --
          switch to that property's own login to act on it day-to-day.
        </p>
      </div>

      {portfolio.isLoading && <p className="text-sm text-slate-500">Loading...</p>}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {portfolio.data?.properties.map((property) => (
          <div key={property.householdId} className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex items-start justify-between">
              <div>
                <h2 className="text-sm font-semibold text-slate-900">{property.name}</h2>
                <p className="text-xs text-slate-500">{property.address}</p>
              </div>
              {property.primary && (
                <span className="rounded bg-slate-100 px-1.5 py-0.5 text-xs font-medium text-slate-600">your household</span>
              )}
            </div>
            <dl className="mt-3 grid grid-cols-3 gap-2 text-center">
              <div>
                <dt className="text-xs text-slate-500">Inventory value</dt>
                <dd className="text-sm font-semibold text-slate-900">${formatCurrency(property.inventoryValue)}</dd>
              </div>
              <div>
                <dt className="text-xs text-slate-500">Low stock</dt>
                <dd className={`text-sm font-semibold ${property.lowStockCount > 0 ? 'text-amber-700' : 'text-slate-900'}`}>
                  {property.lowStockCount}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-slate-500">Pending approvals</dt>
                <dd className={`text-sm font-semibold ${property.pendingApprovalCount > 0 ? 'text-red-700' : 'text-slate-900'}`}>
                  {property.pendingApprovalCount}
                </dd>
              </div>
            </dl>
          </div>
        ))}
      </div>
    </div>
  )
}
