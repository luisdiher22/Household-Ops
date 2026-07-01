import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { PortfolioResponse } from '../types'

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
