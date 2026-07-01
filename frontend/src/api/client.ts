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

let refreshPromise: Promise<AuthResponse> | null = null

async function refreshAccessToken(): Promise<AuthResponse> {
  const auth = loadAuth()
  if (!auth) throw new Error('No refresh token available')

  // Use a bare axios call (not apiClient) to avoid the request interceptor
  // attaching the now-expired access token to the refresh call itself.
  const response = await axios.post<AuthResponse>('/api/auth/refresh', {
    refreshToken: auth.refreshToken,
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
