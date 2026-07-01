import { useState } from 'react'
import { useDecideApproval, usePendingApprovals } from '../hooks/useApprovals'
import type { ApprovalRequest } from '../types'

function ApprovalRow({ approval }: { approval: ApprovalRequest }) {
  const decide = useDecideApproval()
  const [note, setNote] = useState('')

  return (
    <li className="space-y-2 py-4">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-navy">{approval.justification ?? `${approval.subjectType} approval`}</p>
          <p className="text-xs text-navy/60">
            {approval.subjectType} · ${approval.amount}
          </p>
        </div>
      </div>
      <div className="flex items-center gap-2">
        <input
          value={note}
          onChange={(e) => setNote(e.target.value)}
          placeholder="Decision note (optional)"
          className="flex-1 rounded border border-navy/30 px-2 py-1 text-sm"
        />
        <button
          onClick={() => decide.mutate({ id: approval.id, approve: true, note })}
          disabled={decide.isPending}
          className="rounded bg-green-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
        >
          Approve
        </button>
        <button
          onClick={() => decide.mutate({ id: approval.id, approve: false, note })}
          disabled={decide.isPending}
          className="rounded bg-red-50 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-100 disabled:opacity-50"
        >
          Reject
        </button>
      </div>
    </li>
  )
}

export function ApprovalsPage() {
  const approvals = usePendingApprovals()

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-navy">Approvals</h1>
      <div className="rounded-lg border border-navy/15 bg-white p-4 shadow-sm">
        {approvals.isLoading && <p className="text-sm text-navy/60">Loading...</p>}
        {approvals.data && approvals.data.length === 0 && (
          <p className="py-2 text-sm text-navy/60">Nothing waiting on your approval.</p>
        )}
        <ul className="divide-y divide-navy/10">
          {approvals.data?.map((approval) => (
            <ApprovalRow key={approval.id} approval={approval} />
          ))}
        </ul>
      </div>
    </div>
  )
}
