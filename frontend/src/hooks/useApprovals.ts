import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { ApprovalRequest, PageResponse } from '../types'

export function usePendingApprovals() {
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useQuery({
    queryKey: ['approvals', householdId],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<ApprovalRequest>>(
        `/households/${householdId}/approvals`,
        { params: { status: 'PENDING', size: 100 } },
      )
      return data.content
    },
  })
}

export function useDecideApproval() {
  const queryClient = useQueryClient()
  const { auth } = useAuth()

  return useMutation({
    mutationFn: async ({ id, approve, note }: { id: string; approve: boolean; note?: string }) => {
      const { data } = await apiClient.post<ApprovalRequest>(`/approvals/${id}/decide`, { approve, note })
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['approvals', auth!.householdId] })
      queryClient.invalidateQueries({ queryKey: ['tasks', auth!.householdId] })
      queryClient.invalidateQueries({ queryKey: ['shoppingList', auth!.householdId] })
    },
  })
}
