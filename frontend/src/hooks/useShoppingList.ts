import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { PageResponse, ShoppingListItem, ShoppingListItemStatus } from '../types'

export function useShoppingList() {
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useQuery({
    queryKey: ['shoppingList', householdId],
    queryFn: async () => {
      const { data } = await apiClient.get<PageResponse<ShoppingListItem>>(
        `/households/${householdId}/shopping-list`,
        { params: { size: 100 } },
      )
      return data.content
    },
  })
}

export function useUpdateShoppingListStatus() {
  const queryClient = useQueryClient()
  const { auth } = useAuth()

  return useMutation({
    mutationFn: async ({ id, status }: { id: string; status: ShoppingListItemStatus }) => {
      const { data } = await apiClient.patch<ShoppingListItem>(`/shopping-list/${id}`, { status })
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shoppingList', auth!.householdId] })
      queryClient.invalidateQueries({ queryKey: ['approvals', auth!.householdId] })
    },
  })
}

export function useGenerateReorderItems() {
  const queryClient = useQueryClient()
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useMutation({
    mutationFn: async () => {
      const { data } = await apiClient.post<{ itemsQueued: number }>(
        `/households/${householdId}/shopping-list/generate`,
      )
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shoppingList', householdId] })
    },
  })
}
