import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { HouseholdTask, PageResponse, TaskStatus } from '../types'

export function useTasks() {
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useQuery({
    queryKey: ['tasks', householdId],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<HouseholdTask>>(
        `/households/${householdId}/tasks`,
        { params: { size: 100 } },
      )
      return data.content
    },
  })
}

export function useUpdateTaskStatus() {
  const queryClient = useQueryClient()
  const { auth } = useAuth()

  return useMutation({
    mutationFn: async ({ id, status }: { id: string; status: TaskStatus }) => {
      const { data } = await apiClient.patch<HouseholdTask>(`/tasks/${id}`, { status })
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', auth!.householdId] })
      // Marking DONE can be rejected server-side while an approval is still
      // pending, so the approvals list may need refreshing too.
      queryClient.invalidateQueries({ queryKey: ['approvals', auth!.householdId] })
    },
  })
}

// Backend's PATCH /tasks/{id} only ever *sets* assignedToId when it's
// present in the request (never clears it), so this only supports
// reassigning to another staff member, not unassigning.
export function useAssignTask() {
  const queryClient = useQueryClient()
  const { auth } = useAuth()

  return useMutation({
    mutationFn: async ({ id, assignedToId }: { id: string; assignedToId: string }) => {
      const { data } = await apiClient.patch<HouseholdTask>(`/tasks/${id}`, { assignedToId })
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', auth!.householdId] })
    },
  })
}

export function useCreateTask() {
  const queryClient = useQueryClient()
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useMutation({
    mutationFn: async (input: {
      title: string
      description?: string
      assignedToId?: string
      dueDate?: string
      estimatedCost?: number
    }) => {
      const { data } = await apiClient.post<HouseholdTask>(`/households/${householdId}/tasks`, input)
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', householdId] })
      // A task over the household's spend threshold auto-creates a pending
      // approval server-side, so refresh that list too.
      queryClient.invalidateQueries({ queryKey: ['approvals', householdId] })
    },
  })
}
