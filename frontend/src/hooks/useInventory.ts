import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { InventoryItem, InventoryStatus } from '../types'

export function useInventoryStatus() {
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useQuery({
    queryKey: ['inventoryStatus', householdId],
    queryFn: async () => {
      const { data } = await apiClient.get<InventoryStatus>(`/households/${householdId}/inventory/status`)
      return data
    },
  })
}

export function useInventoryItems() {
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useQuery({
    queryKey: ['inventoryItems', householdId],
    queryFn: async () => {
      const { data } = await apiClient.get<InventoryItem[]>(`/households/${householdId}/inventory`)
      return data
    },
  })
}

export function useCreateInventoryItem() {
  const queryClient = useQueryClient()
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useMutation({
    mutationFn: async (input: {
      name: string
      category: string
      currentQuantity: number
      unit: string
      reorderThreshold: number
      reorderQuantity: number
    }) => {
      const { data } = await apiClient.post<InventoryItem>(`/households/${householdId}/inventory`, input)
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventoryItems', householdId] })
      queryClient.invalidateQueries({ queryKey: ['inventoryStatus', householdId] })
    },
  })
}

export function useUpdateInventoryItem() {
  const queryClient = useQueryClient()
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useMutation({
    mutationFn: async ({
      id,
      ...input
    }: {
      id: string
      currentQuantity?: number
      reorderThreshold?: number
      reorderQuantity?: number
    }) => {
      const { data } = await apiClient.patch<InventoryItem>(`/inventory/${id}`, input)
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventoryItems', householdId] })
      queryClient.invalidateQueries({ queryKey: ['inventoryStatus', householdId] })
    },
  })
}
