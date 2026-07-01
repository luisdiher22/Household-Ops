import { useMutation } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import type { AssistantQueryResponse } from '../types'

export function useAssistantQuery() {
  return useMutation({
    mutationFn: async (question: string) => {
      const { data } = await apiClient.post<AssistantQueryResponse>('/assistant/query', { question })
      return data
    },
  })
}
