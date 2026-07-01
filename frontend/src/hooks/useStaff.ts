import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { StaffMember } from '../types'

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
