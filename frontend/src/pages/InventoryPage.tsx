import { useRef, useState } from 'react'
import {
  useCreateInventoryItem,
  useCreateVendor,
  useImportInventoryCsv,
  useInventoryHistory,
  useInventoryItems,
  useInventoryValuation,
  useUpdateInventoryItem,
  useVendors,
} from '../hooks/useInventory'
import { useCreateShoppingListItem } from '../hooks/useShoppingList'
import type { InventoryItem } from '../types'

// Fixed to en-US regardless of the browser's locale -- otherwise
// toLocaleString() renders e.g. "1 424,5" under some system locales, which
// reads as broken rather than just differently formatted.
function formatCurrency(value: number): string {
  return value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function ValuationSummary() {
  const valuation = useInventoryValuation()
  if (!valuation.data || valuation.data.byCategory.length === 0) return null

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-baseline justify-between">
        <h2 className="text-sm font-semibold text-slate-900">Inventory value</h2>
        <span className="text-lg font-semibold text-slate-900">${formatCurrency(valuation.data.totalValue)}</span>
      </div>
      <ul className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">
        {valuation.data.byCategory.map((c) => (
          <li key={c.category}>
            {c.category}: ${formatCurrency(c.totalValue)} ({c.itemCount})
          </li>
        ))}
      </ul>
    </div>
  )
}

function VendorsSection() {
  const vendors = useVendors()
  const createVendor = useCreateVendor()
  const [name, setName] = useState('')
  const [contactEmail, setContactEmail] = useState('')

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <h2 className="mb-2 text-sm font-semibold text-slate-900">Vendors</h2>
      <ul className="mb-3 space-y-1 text-sm text-slate-700">
        {vendors.data?.map((v) => (
          <li key={v.id}>
            {v.name}
            {v.contactEmail && <span className="text-slate-400"> · {v.contactEmail}</span>}
          </li>
        ))}
        {vendors.data?.length === 0 && <li className="text-slate-500">No vendors yet.</li>}
      </ul>
      <form
        className="flex flex-wrap items-end gap-2"
        onSubmit={(e) => {
          e.preventDefault()
          if (!name.trim()) return
          createVendor.mutate(
            { name, contactEmail: contactEmail || undefined },
            { onSuccess: () => { setName(''); setContactEmail('') } },
          )
        }}
      >
        <div>
          <label className="block text-xs text-slate-500">Vendor name</label>
          <input value={name} onChange={(e) => setName(e.target.value)} className="rounded border border-slate-300 px-2 py-1 text-sm" />
        </div>
        <div>
          <label className="block text-xs text-slate-500">Contact email</label>
          <input value={contactEmail} onChange={(e) => setContactEmail(e.target.value)} className="rounded border border-slate-300 px-2 py-1 text-sm" />
        </div>
        <button
          type="submit"
          disabled={createVendor.isPending}
          className="rounded border border-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50 disabled:opacity-50"
        >
          Add vendor
        </button>
      </form>
    </div>
  )
}

function ImportControl() {
  const importCsv = useImportInventoryCsv()
  const fileInputRef = useRef<HTMLInputElement>(null)

  function downloadTemplate() {
    const csv = 'name,category,unit,currentQuantity,reorderThreshold,reorderQuantity,unitCost\nOlive Oil,PANTRY,bottles,4,3,4,18.50\n'
    const blob = new Blob([csv], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'inventory-template.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold text-slate-900">Bulk import</h2>
        <button onClick={downloadTemplate} className="text-xs text-slate-500 underline hover:text-slate-700">
          Download CSV template
        </button>
      </div>
      <input
        ref={fileInputRef}
        type="file"
        accept=".csv"
        className="mt-2 text-sm"
        onChange={(e) => {
          const file = e.target.files?.[0]
          if (file) importCsv.mutate(file)
          if (fileInputRef.current) fileInputRef.current.value = ''
        }}
      />
      {importCsv.isSuccess && (
        <p className="mt-2 text-sm text-slate-600">
          Imported {importCsv.data.imported} item{importCsv.data.imported === 1 ? '' : 's'}.
          {importCsv.data.errors.length > 0 && (
            <span className="ml-1 text-amber-600">{importCsv.data.errors.length} row(s) skipped: {importCsv.data.errors.join('; ')}</span>
          )}
        </p>
      )}
    </div>
  )
}

function HistoryPanel({ itemId }: { itemId: string }) {
  const history = useInventoryHistory(itemId)
  return (
    <div className="mt-2 rounded border border-slate-100 bg-slate-50 p-2 text-xs text-slate-600">
      {history.isLoading && <p>Loading history...</p>}
      {history.data?.length === 0 && <p>No adjustments logged yet.</p>}
      <ul className="space-y-1">
        {history.data?.map((a) => (
          <li key={a.id}>
            {new Date(a.occurredAt).toLocaleDateString()} — {a.reason} {a.delta > 0 ? '+' : ''}{a.delta} ({a.previousQuantity} → {a.newQuantity})
            {a.adjustedByName && ` by ${a.adjustedByName}`}
          </li>
        ))}
      </ul>
    </div>
  )
}

function ReorderButton({ item }: { item: InventoryItem }) {
  const createShoppingListItem = useCreateShoppingListItem()

  return (
    <button
      onClick={() =>
        createShoppingListItem.mutate({
          description: item.vendorName ? `Restock: ${item.name} (from ${item.vendorName})` : `Restock: ${item.name}`,
          quantity: item.reorderQuantity,
          estimatedCost: item.unitCost != null ? item.unitCost * item.reorderQuantity : undefined,
        })
      }
      disabled={createShoppingListItem.isPending}
      title={item.vendorName ? `Add to shopping list, pre-filled from ${item.vendorName}` : 'Add to shopping list'}
      className="rounded border border-amber-300 bg-amber-50 px-2 py-1 text-xs font-medium text-amber-700 hover:bg-amber-100 disabled:opacity-50"
    >
      {createShoppingListItem.isSuccess ? 'Added ✓' : 'Reorder'}
    </button>
  )
}

function InventoryRow({ item }: { item: InventoryItem }) {
  const updateItem = useUpdateInventoryItem()
  const [quantity, setQuantity] = useState(String(item.currentQuantity))
  const [isCorrection, setIsCorrection] = useState(false)
  const [showHistory, setShowHistory] = useState(false)

  return (
    <li className="py-3">
      <div className="flex items-center justify-between gap-4">
        <div className="min-w-0">
          <p className="text-sm font-medium text-slate-900">
            {item.name}
            {item.lowStock && <span className="ml-2 rounded bg-amber-50 px-1.5 py-0.5 text-xs font-medium text-amber-700">low stock</span>}
            {item.expiringSoon && <span className="ml-2 rounded bg-red-50 px-1.5 py-0.5 text-xs font-medium text-red-700">expiring soon</span>}
          </p>
          <p className="text-xs text-slate-500">
            reorder at {item.reorderThreshold} {item.unit} (orders {item.reorderQuantity} more)
            {item.vendorName && ` · ${item.vendorName}`}
            {item.unitCost != null && ` · $${formatCurrency(item.unitCost)}/unit ($${formatCurrency(item.totalValue ?? 0)} total)`}
            {item.expirationDate && ` · expires ${item.expirationDate}`}
            {item.predictedDaysUntilEmpty != null && (
              <span className="font-medium text-slate-600"> · ~{item.predictedDaysUntilEmpty}d until empty</span>
            )}
          </p>
          <button onClick={() => setShowHistory((s) => !s)} className="mt-0.5 text-xs text-slate-400 underline hover:text-slate-600">
            {showHistory ? 'Hide history' : 'Show history'}
          </button>
        </div>
        <div className="flex items-center gap-2">
          {item.lowStock && <ReorderButton item={item} />}
          <form
            className="flex items-center gap-2"
            onSubmit={(e) => {
              e.preventDefault()
              const parsed = Number(quantity)
              if (Number.isNaN(parsed) || parsed < 0) return
              updateItem.mutate({ id: item.id, currentQuantity: parsed, reason: isCorrection ? 'MANUAL_CORRECTION' : undefined })
            }}
          >
            <label className="flex items-center gap-1 text-xs text-slate-500">
              <input type="checkbox" checked={isCorrection} onChange={(e) => setIsCorrection(e.target.checked)} title="Recount correction, not real usage" />
              recount
            </label>
            <input
              type="number"
              min="0"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              className="w-20 rounded border border-slate-300 px-2 py-1 text-sm"
            />
            <span className="text-xs text-slate-500">{item.unit}</span>
            <button
              type="submit"
              disabled={updateItem.isPending || Number(quantity) === item.currentQuantity}
              className="rounded border border-slate-300 px-2 py-1 text-xs text-slate-700 hover:bg-slate-50 disabled:opacity-50"
            >
              Update
            </button>
          </form>
        </div>
      </div>
      {showHistory && <HistoryPanel itemId={item.id} />}
    </li>
  )
}

function NewItemForm() {
  const createItem = useCreateInventoryItem()
  const vendors = useVendors()
  const [name, setName] = useState('')
  const [category, setCategory] = useState('')
  const [unit, setUnit] = useState('')
  const [currentQuantity, setCurrentQuantity] = useState('0')
  const [reorderThreshold, setReorderThreshold] = useState('1')
  const [reorderQuantity, setReorderQuantity] = useState('1')
  const [vendorId, setVendorId] = useState('')
  const [unitCost, setUnitCost] = useState('')
  const [expirationDate, setExpirationDate] = useState('')

  return (
    <form
      className="flex flex-wrap items-end gap-2 rounded-lg border border-slate-200 bg-white p-4"
      onSubmit={(e) => {
        e.preventDefault()
        if (!name.trim() || !category.trim() || !unit.trim()) return
        createItem.mutate(
          {
            name,
            category,
            unit,
            currentQuantity: Number(currentQuantity) || 0,
            reorderThreshold: Number(reorderThreshold) || 0,
            reorderQuantity: Number(reorderQuantity) || 1,
            vendorId: vendorId || undefined,
            unitCost: unitCost ? Number(unitCost) : undefined,
            expirationDate: expirationDate || undefined,
          },
          {
            onSuccess: () => {
              setName(''); setCategory(''); setUnit('')
              setCurrentQuantity('0'); setReorderThreshold('1'); setReorderQuantity('1')
              setVendorId(''); setUnitCost(''); setExpirationDate('')
            },
          },
        )
      }}
    >
      <div className="flex-1 min-w-[140px]">
        <label className="block text-xs text-slate-500">Name</label>
        <input value={name} onChange={(e) => setName(e.target.value)} className="w-full rounded border border-slate-300 px-2 py-1 text-sm" />
      </div>
      <div>
        <label className="block text-xs text-slate-500">Category</label>
        <input value={category} onChange={(e) => setCategory(e.target.value)} placeholder="PANTRY" className="w-28 rounded border border-slate-300 px-2 py-1 text-sm" />
      </div>
      <div>
        <label className="block text-xs text-slate-500">Unit</label>
        <input value={unit} onChange={(e) => setUnit(e.target.value)} placeholder="bottles" className="w-24 rounded border border-slate-300 px-2 py-1 text-sm" />
      </div>
      <div>
        <label className="block text-xs text-slate-500">Quantity</label>
        <input type="number" min="0" value={currentQuantity} onChange={(e) => setCurrentQuantity(e.target.value)} className="w-20 rounded border border-slate-300 px-2 py-1 text-sm" />
      </div>
      <div>
        <label className="block text-xs text-slate-500">Reorder at</label>
        <input type="number" min="0" value={reorderThreshold} onChange={(e) => setReorderThreshold(e.target.value)} className="w-20 rounded border border-slate-300 px-2 py-1 text-sm" />
      </div>
      <div>
        <label className="block text-xs text-slate-500">Reorder qty</label>
        <input type="number" min="1" value={reorderQuantity} onChange={(e) => setReorderQuantity(e.target.value)} className="w-20 rounded border border-slate-300 px-2 py-1 text-sm" />
      </div>
      <div>
        <label className="block text-xs text-slate-500">Vendor</label>
        <select value={vendorId} onChange={(e) => setVendorId(e.target.value)} className="rounded border border-slate-300 px-2 py-1 text-sm">
          <option value="">None</option>
          {vendors.data?.map((v) => (
            <option key={v.id} value={v.id}>{v.name}</option>
          ))}
        </select>
      </div>
      <div>
        <label className="block text-xs text-slate-500">Unit cost</label>
        <input type="number" step="0.01" value={unitCost} onChange={(e) => setUnitCost(e.target.value)} className="w-24 rounded border border-slate-300 px-2 py-1 text-sm" />
      </div>
      <div>
        <label className="block text-xs text-slate-500">Expires</label>
        <input type="date" value={expirationDate} onChange={(e) => setExpirationDate(e.target.value)} className="rounded border border-slate-300 px-2 py-1 text-sm" />
      </div>
      <button
        type="submit"
        disabled={createItem.isPending}
        className="rounded bg-slate-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
      >
        Add item
      </button>
    </form>
  )
}

export function InventoryPage() {
  const items = useInventoryItems()

  const byCategory = new Map<string, InventoryItem[]>()
  for (const item of items.data ?? []) {
    const list = byCategory.get(item.category) ?? []
    list.push(item)
    byCategory.set(item.category, list)
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-slate-900">Inventory</h1>

      <ValuationSummary />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <VendorsSection />
        <ImportControl />
      </div>

      <NewItemForm />

      {items.isLoading && <p className="text-sm text-slate-500">Loading...</p>}
      {items.data && items.data.length === 0 && <p className="text-sm text-slate-500">No inventory items tracked yet.</p>}

      {[...byCategory.entries()].map(([category, categoryItems]) => (
        <div key={category} className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <h2 className="mb-1 text-sm font-semibold text-slate-900">{category}</h2>
          <ul className="divide-y divide-slate-100">
            {categoryItems.map((item) => (
              <InventoryRow key={item.id} item={item} />
            ))}
          </ul>
        </div>
      ))}
    </div>
  )
}
