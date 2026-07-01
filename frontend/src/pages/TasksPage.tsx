import { useState } from 'react'
import { useAssignTask, useCreateTask, useTasks, useUpdateTaskStatus } from '../hooks/useTasks'
import { useStaff } from '../hooks/useStaff'
import { useAuth } from '../auth/AuthContext'
import type { HouseholdTask, TaskStatus } from '../types'

const STATUS_OPTIONS: TaskStatus[] = ['OPEN', 'IN_PROGRESS', 'BLOCKED', 'DONE', 'CANCELLED']

/** Only an Owner/House Manager may assign tasks -- enforced server-side too (TaskController), this just keeps the control from being shown to roles that can't use it. */
function canAssignTasks(role: string | undefined) {
  return role === 'OWNER' || role === 'HOUSE_MANAGER'
}

function TaskRow({ task }: { task: HouseholdTask }) {
  const updateStatus = useUpdateTaskStatus()
  const assignTask = useAssignTask()
  const staff = useStaff()
  const { auth } = useAuth()
  const assignee = staff.data?.find((s) => s.id === task.assignedToId)

  return (
    <li className="flex items-center justify-between gap-4 py-3">
      <div className="min-w-0">
        <p className="truncate text-sm font-medium text-slate-900">{task.title}</p>
        <p className="text-xs text-slate-500">
          {task.dueDate && `Due ${task.dueDate}`}
          {task.estimatedCost != null && ` · $${task.estimatedCost}`}
          {task.approvalPending && <span className="ml-1 font-medium text-amber-600">· Approval pending</span>}
        </p>
      </div>
      <div className="flex items-center gap-2">
        {canAssignTasks(auth?.role) ? (
          <select
            value={task.assignedToId ?? ''}
            disabled={assignTask.isPending || staff.isLoading}
            onChange={(e) => e.target.value && assignTask.mutate({ id: task.id, assignedToId: e.target.value })}
            className="rounded border border-slate-300 px-2 py-1 text-sm"
          >
            <option value="" disabled>
              {assignee ? assignee.fullName : 'Unassigned'}
            </option>
            {staff.data?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.fullName}
              </option>
            ))}
          </select>
        ) : (
          <span className="text-sm text-slate-500">{assignee ? assignee.fullName : 'Unassigned'}</span>
        )}
        <select
          value={task.status}
          disabled={updateStatus.isPending}
          onChange={(e) => updateStatus.mutate({ id: task.id, status: e.target.value as TaskStatus })}
          className="rounded border border-slate-300 px-2 py-1 text-sm"
        >
          {STATUS_OPTIONS.map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>
      </div>
    </li>
  )
}

function NewTaskForm() {
  const createTask = useCreateTask()
  const staff = useStaff()
  const { auth } = useAuth()
  const [title, setTitle] = useState('')
  const [assignedToId, setAssignedToId] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [estimatedCost, setEstimatedCost] = useState('')

  return (
    <form
      className="flex flex-wrap items-end gap-2 rounded-lg border border-slate-200 bg-white p-4"
      onSubmit={(e) => {
        e.preventDefault()
        if (!title.trim()) return
        createTask.mutate(
          {
            title,
            assignedToId: assignedToId || undefined,
            dueDate: dueDate || undefined,
            estimatedCost: estimatedCost ? Number(estimatedCost) : undefined,
          },
          { onSuccess: () => { setTitle(''); setAssignedToId(''); setDueDate(''); setEstimatedCost('') } },
        )
      }}
    >
      <div className="flex-1 min-w-[180px]">
        <label className="block text-xs text-slate-500">Title</label>
        <input value={title} onChange={(e) => setTitle(e.target.value)} className="w-full rounded border border-slate-300 px-2 py-1 text-sm" />
      </div>
      {canAssignTasks(auth?.role) && (
        <div>
          <label className="block text-xs text-slate-500">Assign to</label>
          <select
            value={assignedToId}
            onChange={(e) => setAssignedToId(e.target.value)}
            className="rounded border border-slate-300 px-2 py-1 text-sm"
          >
            <option value="">Unassigned</option>
            {staff.data?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.fullName}
              </option>
            ))}
          </select>
        </div>
      )}
      <div>
        <label className="block text-xs text-slate-500">Due date</label>
        <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} className="rounded border border-slate-300 px-2 py-1 text-sm" />
      </div>
      <div>
        <label className="block text-xs text-slate-500">Estimated cost</label>
        <input
          type="number"
          value={estimatedCost}
          onChange={(e) => setEstimatedCost(e.target.value)}
          className="w-28 rounded border border-slate-300 px-2 py-1 text-sm"
        />
      </div>
      <button
        type="submit"
        disabled={createTask.isPending}
        className="rounded bg-slate-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
      >
        Add task
      </button>
    </form>
  )
}

export function TasksPage() {
  const tasks = useTasks()

  const grouped = STATUS_OPTIONS.map((status) => ({
    status,
    items: tasks.data?.filter((t) => t.status === status) ?? [],
  })).filter((group) => group.items.length > 0)

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-slate-900">Tasks</h1>
      <NewTaskForm />

      {tasks.isLoading && <p className="text-sm text-slate-500">Loading...</p>}

      {grouped.map((group) => (
        <div key={group.status} className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <h2 className="mb-1 text-sm font-semibold text-slate-900">{group.status}</h2>
          <ul className="divide-y divide-slate-100">
            {group.items.map((task) => (
              <TaskRow key={task.id} task={task} />
            ))}
          </ul>
        </div>
      ))}

      {tasks.data && tasks.data.length === 0 && <p className="text-sm text-slate-500">No tasks yet.</p>}
    </div>
  )
}
