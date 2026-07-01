import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { AuthResponse } from '../types'
import { clearAuth, loadAuth, saveAuth } from './authStorage'

export const apiClient = axios.create({
  baseURL: '/api',
})

apiClient.interceptors.request.use((config) => {
  const auth = loadAuth()
  if (auth && config.headers) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

// Shared across all callers so that if several requests 401 at once (e.g. a
// page loading multiple queries in parallel right as the token expires),
// they all await the same in-flight refresh instead of each firing their own
// /auth/refresh call and racing to save a token.
let refreshPromise: Promise<AuthResponse> | null = null

async function refreshAccessToken(): Promise<AuthResponse> {
  const auth = loadAuth()
  if (!auth) throw new Error('No refresh token available')

  // Use a bare axios call (not apiClient) to avoid the request interceptor
  // attaching the now-expired access token to the refresh call itself.
  // activeHouseholdId carries forward whichever property the user last
  // switched to (see usePropertySwitcher) -- otherwise a background refresh
  // mid-session would silently drop them back to their home household.
  const response = await axios.post<AuthResponse>('/api/auth/refresh', {
    refreshToken: auth.refreshToken,
    activeHouseholdId: auth.householdId,
  })
  saveAuth(response.data)
  return response.data
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined

    if (error.response?.status === 401 && original && !original._retried) {
      original._retried = true
      try {
        refreshPromise ??= refreshAccessToken().finally(() => {
          refreshPromise = null
        })
        const refreshed = await refreshPromise
        original.headers = original.headers ?? {}
        original.headers.Authorization = `Bearer ${refreshed.accessToken}`
        return apiClient.request(original)
      } catch {
        clearAuth()
        window.location.href = '/login'
      }
    }

    return Promise.reject(error)
  },
)
