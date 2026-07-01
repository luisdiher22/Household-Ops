import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { StaffMember, StaffRole } from '../types'

export function useStaff() {
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useQuery({
    queryKey: ['staff', householdId],
    queryFn: async () => {
      const { data } = await apiClient.get<StaffMember[]>(`/households/${householdId}/staff`)
      return data
    },
  })
}

// Backend gate is OWNER or HOUSE_MANAGER (StaffMemberController.create) -- this
// mutation itself doesn't re-check the role, the page only renders the form
// for those roles, matching the pattern used for task assignment.
export function useCreateStaffMember() {
  const queryClient = useQueryClient()
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useMutation({
    mutationFn: async (input: { fullName: string; email: string; password: string; role: StaffRole }) => {
      const { data } = await apiClient.post<StaffMember>(`/households/${householdId}/staff`, input)
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['staff', householdId] })
    },
  })
}

// Backend gate is OWNER-only (StaffMemberController.update) -- role and active
// changes are both routed through this one endpoint there, so this hook
// mirrors that instead of splitting into two calls.
export function useUpdateStaffMember() {
  const queryClient = useQueryClient()
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useMutation({
    mutationFn: async ({ id, ...input }: { id: string; role?: StaffRole; active?: boolean }) => {
      const { data } = await apiClient.patch<StaffMember>(`/staff/${id}`, input)
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['staff', householdId] })
    },
  })
}
