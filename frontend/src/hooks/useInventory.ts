import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type {
  AdjustmentReason,
  ImportResult,
  InventoryAdjustment,
  InventoryItem,
  InventoryStatus,
  ValuationResponse,
  Vendor,
} from '../types'

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

export function useInventoryValuation() {
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useQuery({
    queryKey: ['inventoryValuation', householdId],
    queryFn: async () => {
      const { data } = await apiClient.get<ValuationResponse>(`/households/${householdId}/inventory/valuation`)
      return data
    },
  })
}

export function useInventoryHistory(itemId: string | null) {
  return useQuery({
    queryKey: ['inventoryHistory', itemId],
    queryFn: async () => {
      const { data } = await apiClient.get<InventoryAdjustment[]>(`/inventory/${itemId}/history`)
      return data
    },
    enabled: itemId != null,
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
      vendorId?: string
      unitCost?: number
      expirationDate?: string
    }) => {
      const { data } = await apiClient.post<InventoryItem>(`/households/${householdId}/inventory`, input)
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventoryItems', householdId] })
      queryClient.invalidateQueries({ queryKey: ['inventoryStatus', householdId] })
      queryClient.invalidateQueries({ queryKey: ['inventoryValuation', householdId] })
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
      vendorId?: string
      unitCost?: number
      expirationDate?: string
      reason?: AdjustmentReason
    }) => {
      const { data } = await apiClient.patch<InventoryItem>(`/inventory/${id}`, input)
      return data
    },
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['inventoryItems', householdId] })
      queryClient.invalidateQueries({ queryKey: ['inventoryStatus', householdId] })
      queryClient.invalidateQueries({ queryKey: ['inventoryValuation', householdId] })
      queryClient.invalidateQueries({ queryKey: ['inventoryHistory', id] })
    },
  })
}

export function useVendors() {
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useQuery({
    queryKey: ['vendors', householdId],
    queryFn: async () => {
      const { data } = await apiClient.get<Vendor[]>(`/households/${householdId}/vendors`)
      return data
    },
  })
}

export function useCreateVendor() {
  const queryClient = useQueryClient()
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useMutation({
    mutationFn: async (input: { name: string; contactEmail?: string; contactPhone?: string; notes?: string }) => {
      const { data } = await apiClient.post<Vendor>(`/households/${householdId}/vendors`, input)
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vendors', householdId] })
    },
  })
}

export function useImportInventoryCsv() {
  const queryClient = useQueryClient()
  const { auth } = useAuth()
  const householdId = auth!.householdId

  return useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData()
      formData.append('file', file)
      const { data } = await apiClient.post<ImportResult>(`/households/${householdId}/inventory/import`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventoryItems', householdId] })
      queryClient.invalidateQueries({ queryKey: ['inventoryStatus', householdId] })
      queryClient.invalidateQueries({ queryKey: ['inventoryValuation', householdId] })
    },
  })
}
