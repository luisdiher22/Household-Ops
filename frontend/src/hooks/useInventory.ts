import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { InventoryStatus } from '../types'

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
