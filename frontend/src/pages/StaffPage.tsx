import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { useCreateStaffMember, useStaff, useUpdateStaffMember } from '../hooks/useStaff'
import type { StaffMember, StaffRole } from '../types'

const ROLE_OPTIONS: StaffRole[] = ['OWNER', 'HOUSE_MANAGER', 'STAFF', 'VENDOR']

/** Backend gate for create() is OWNER or HOUSE_MANAGER (StaffMemberController) -- this just keeps the form from being shown to roles that can't use it. */
function canAddStaff(role: string | undefined) {
  return role === 'OWNER' || role === 'HOUSE_MANAGER'
}

function StaffRow({ member }: { member: StaffMember }) {
  const { auth } = useAuth()
  const updateStaff = useUpdateStaffMember()
  const isSelf = member.id === auth?.staffId

  return (
    <li className="flex items-center justify-between gap-4 py-3">
      <div className="min-w-0">
        <p className="text-sm font-medium text-navy">
          {member.fullName}
          {!member.active && <span className="ml-2 rounded bg-navy/10 px-1.5 py-0.5 text-xs font-medium text-navy/60">inactive</span>}
        </p>
        <p className="text-xs text-navy/60">{member.email}</p>
      </div>
      <div className="flex items-center gap-2">
        {auth?.role === 'OWNER' ? (
          <>
            <select
              value={member.role}
              disabled={updateStaff.isPending || isSelf}
              title={isSelf ? "You can't change your own role" : undefined}
              onChange={(e) => updateStaff.mutate({ id: member.id, role: e.target.value as StaffRole })}
              className="rounded border border-navy/30 px-2 py-1 text-sm disabled:opacity-50"
            >
              {ROLE_OPTIONS.map((role) => (
                <option key={role} value={role}>
                  {role}
                </option>
              ))}
            </select>
            <button
              onClick={() => updateStaff.mutate({ id: member.id, active: !member.active })}
              disabled={updateStaff.isPending || isSelf}
              title={isSelf ? "You can't deactivate your own account" : undefined}
              className="rounded border border-navy/30 px-2 py-1 text-xs text-navy/90 hover:bg-ivory disabled:opacity-50"
            >
              {member.active ? 'Deactivate' : 'Reactivate'}
            </button>
          </>
        ) : (
          <span className="text-sm text-navy/60">{member.role}</span>
        )}
      </div>
    </li>
  )
}

function NewStaffForm() {
  const createStaff = useCreateStaffMember()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<StaffRole>('STAFF')

  return (
    <form
      className="flex flex-wrap items-end gap-2 rounded-lg border border-navy/15 bg-white p-4"
      onSubmit={(e) => {
        e.preventDefault()
        if (!fullName.trim() || !email.trim() || password.length < 8) return
        createStaff.mutate(
          { fullName, email, password, role },
          { onSuccess: () => { setFullName(''); setEmail(''); setPassword(''); setRole('STAFF') } },
        )
      }}
    >
      <div className="flex-1 min-w-[160px]">
        <label className="block text-xs text-navy/60">Full name</label>
        <input value={fullName} onChange={(e) => setFullName(e.target.value)} className="w-full rounded border border-navy/30 px-2 py-1 text-sm" />
      </div>
      <div className="flex-1 min-w-[200px]">
        <label className="block text-xs text-navy/60">Email</label>
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} className="w-full rounded border border-navy/30 px-2 py-1 text-sm" />
      </div>
      <div>
        <label className="block text-xs text-navy/60">Password</label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="min. 8 characters"
          className="rounded border border-navy/30 px-2 py-1 text-sm"
        />
      </div>
      <div>
        <label className="block text-xs text-navy/60">Role</label>
        <select value={role} onChange={(e) => setRole(e.target.value as StaffRole)} className="rounded border border-navy/30 px-2 py-1 text-sm">
          {ROLE_OPTIONS.map((r) => (
            <option key={r} value={r}>
              {r}
            </option>
          ))}
        </select>
      </div>
      <button
        type="submit"
        disabled={createStaff.isPending}
        className="rounded bg-navy px-3 py-1.5 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50"
      >
        Add staff
      </button>
    </form>
  )
}

export function StaffPage() {
  const { auth } = useAuth()
  const staff = useStaff()

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-navy">Staff</h1>

      {canAddStaff(auth?.role) && <NewStaffForm />}

      {staff.isLoading && <p className="text-sm text-navy/60">Loading...</p>}

      <div className="rounded-lg border border-navy/15 bg-white p-4 shadow-sm">
        <ul className="divide-y divide-navy/10">
          {staff.data?.map((member) => (
            <StaffRow key={member.id} member={member} />
          ))}
        </ul>
        {staff.data && staff.data.length === 0 && <p className="text-sm text-navy/60">No staff yet.</p>}
      </div>
    </div>
  )
}
