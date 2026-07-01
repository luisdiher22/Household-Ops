import { useState } from 'react'
import { useCreateInventoryItem, useInventoryItems, useUpdateInventoryItem } from '../hooks/useInventory'
import type { InventoryItem } from '../types'

function InventoryRow({ item }: { item: InventoryItem }) {
  const updateItem = useUpdateInventoryItem()
  const [quantity, setQuantity] = useState(String(item.currentQuantity))

  return (
    <li className="flex items-center justify-between gap-4 py-3">
      <div className="min-w-0">
        <p className="text-sm font-medium text-slate-900">
          {item.name}
          {item.lowStock && <span className="ml-2 rounded bg-amber-50 px-1.5 py-0.5 text-xs font-medium text-amber-700">low stock</span>}
        </p>
        <p className="text-xs text-slate-500">
          {item.category} · reorder at {item.reorderThreshold} {item.unit} (orders {item.reorderQuantity} more)
        </p>
      </div>
      <form
        className="flex items-center gap-2"
        onSubmit={(e) => {
          e.preventDefault()
          const parsed = Number(quantity)
          if (Number.isNaN(parsed) || parsed < 0) return
          updateItem.mutate({ id: item.id, currentQuantity: parsed })
        }}
      >
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
    </li>
  )
}

function NewItemForm() {
  const createItem = useCreateInventoryItem()
  const [name, setName] = useState('')
  const [category, setCategory] = useState('')
  const [unit, setUnit] = useState('')
  const [currentQuantity, setCurrentQuantity] = useState('0')
  const [reorderThreshold, setReorderThreshold] = useState('1')
  const [reorderQuantity, setReorderQuantity] = useState('1')

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
          },
          {
            onSuccess: () => {
              setName(''); setCategory(''); setUnit('')
              setCurrentQuantity('0'); setReorderThreshold('1'); setReorderQuantity('1')
            },
          },
        )
      }}
    >
      <div className="flex-1 min-w-[160px]">
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
