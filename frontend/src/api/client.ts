import axios, { type InternalAxiosRequestConfig } from 'axios'
import type { AuthResponse } from '../lib/types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api/v1'

const ACCESS_TOKEN_KEY = 'taskflow_access_token'
const REFRESH_TOKEN_KEY = 'taskflow_refresh_token'

let accessToken: string | null = localStorage.getItem(ACCESS_TOKEN_KEY)
let refreshToken: string | null = localStorage.getItem(REFRESH_TOKEN_KEY)

export function setTokens(tokens: { accessToken: string; refreshToken: string } | null) {
  if (tokens) {
    accessToken = tokens.accessToken
    refreshToken = tokens.refreshToken
    localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken)
  } else {
    accessToken = null
    refreshToken = null
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }
}

export function getAccessToken() {
  return accessToken
}

export const apiClient = axios.create({ baseURL: API_BASE_URL })

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

let pendingRefresh: Promise<string | null> | null = null

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as RetriableRequestConfig | undefined
    const isAuthEndpoint = originalRequest?.url?.includes('/auth/')

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry && !isAuthEndpoint && refreshToken) {
      originalRequest._retry = true

      if (!pendingRefresh) {
        pendingRefresh = axios
          .post<AuthResponse>(`${API_BASE_URL}/auth/refresh`, { refreshToken })
          .then((res) => {
            setTokens({ accessToken: res.data.accessToken, refreshToken: res.data.refreshToken })
            return res.data.accessToken
          })
          .catch(() => {
            setTokens(null)
            window.location.assign('/login')
            return null
          })
          .finally(() => {
            pendingRefresh = null
          })
      }

      const newAccessToken = await pendingRefresh
      if (newAccessToken) {
        originalRequest.headers.set('Authorization', `Bearer ${newAccessToken}`)
        return apiClient(originalRequest)
      }
    }

    return Promise.reject(error)
  },
)

export function extractErrorMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (axios.isAxiosError(error)) {
    const detail = error.response?.data?.detail
    if (typeof detail === 'string') return detail
  }
  return fallback
}
