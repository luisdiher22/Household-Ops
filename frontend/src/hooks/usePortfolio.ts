import { useMutation, useQuery } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { AuthResponse, PortfolioResponse } from '../types'

// Only meaningful for an Owner, and only fetched for one -- see PortfolioPage,
// which doesn't render the nav link at all for other roles (mirrors Approvals).
export function usePortfolio() {
  const { auth } = useAuth()

  return useQuery({
    queryKey: ['portfolio', auth?.staffId],
    queryFn: async () => {
      const { data } = await apiClient.get<PortfolioResponse>('/portfolio')
      return data
    },
    enabled: auth?.role === 'OWNER',
  })
}

// No manual cache invalidation needed on success -- every hook that queries
// household-scoped data keys off auth.householdId, so updating the session
// here makes React Query treat them as new query keys and refetch on their
// own against the newly active property.
export function useSwitchHousehold() {
  const { setAuthResponse } = useAuth()

  return useMutation({
    mutationFn: async (householdId: string) => {
      const { data } = await apiClient.post<AuthResponse>('/auth/switch-household', { householdId })
      return data
    },
    onSuccess: (data) => setAuthResponse(data),
  })
}
