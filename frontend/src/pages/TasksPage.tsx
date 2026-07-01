import { useState } from 'react'
import { useCreateTask, useTasks, useUpdateTaskStatus } from '../hooks/useTasks'
import { useStaff } from '../hooks/useStaff'
import type { HouseholdTask, TaskStatus } from '../types'

const STATUS_OPTIONS: TaskStatus[] = ['OPEN', 'IN_PROGRESS', 'BLOCKED', 'DONE', 'CANCELLED']

function TaskRow({ task }: { task: HouseholdTask }) {
  const updateStatus = useUpdateTaskStatus()
  const staff = useStaff()
  const assignee = staff.data?.find((s) => s.id === task.assignedToId)

  return (
    <li className="flex items-center justify-between gap-4 py-3">
      <div className="min-w-0">
        <p className="truncate text-sm font-medium text-slate-900">{task.title}</p>
        <p className="text-xs text-slate-500">
          {assignee ? `Assigned to ${assignee.fullName}` : 'Unassigned'}
          {task.dueDate && ` · Due ${task.dueDate}`}
          {task.estimatedCost != null && ` · $${task.estimatedCost}`}
          {task.approvalPending && <span className="ml-1 font-medium text-amber-600">· Approval pending</span>}
        </p>
      </div>
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
    </li>
  )
}

function NewTaskForm() {
  const createTask = useCreateTask()
  const [title, setTitle] = useState('')
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
            dueDate: dueDate || undefined,
            estimatedCost: estimatedCost ? Number(estimatedCost) : undefined,
          },
          { onSuccess: () => { setTitle(''); setDueDate(''); setEstimatedCost('') } },
        )
      }}
    >
      <div className="flex-1 min-w-[180px]">
        <label className="block text-xs text-slate-500">Title</label>
        <input value={title} onChange={(e) => setTitle(e.target.value)} className="w-full rounded border border-slate-300 px-2 py-1 text-sm" />
      </div>
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
