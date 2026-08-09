import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

import {
  clearAuthSession,
  getAccessToken,
  getAuthSession,
  saveRefreshedAuthSession,
} from '../../features/auth/lib/auth-storage'
import type { LoginResponse } from '../../features/auth/types/auth'

const apiBaseUrl = import.meta.env.VITE_API_URL ?? '/api'

export const httpClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

const authHttpClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

type RetriableRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean
}

let refreshSessionPromise: Promise<LoginResponse> | null = null

function isAuthRoute(url?: string) {
  return Boolean(
    url?.includes('/auth/login') ||
      url?.includes('/auth/refresh') ||
      url?.includes('/auth/logout'),
  )
}

function shouldTryRefresh(error: AxiosError) {
  const config = error.config as RetriableRequestConfig | undefined

  return (
    error.response?.status === 401 &&
    Boolean(config) &&
    !config?._retry &&
    !isAuthRoute(config?.url)
  )
}

async function refreshSession() {
  const session = getAuthSession()

  if (!session?.tokenAtualizacao) {
    throw new Error('Sessão sem token de atualização')
  }

  if (!refreshSessionPromise) {
    refreshSessionPromise = authHttpClient
      .post<LoginResponse>('/auth/refresh', {
        tokenAtualizacao: session.tokenAtualizacao,
      })
      .then((response) => {
        saveRefreshedAuthSession(response.data)
        return response.data
      })
      .finally(() => {
        refreshSessionPromise = null
      })
  }

  return refreshSessionPromise
}

httpClient.interceptors.request.use((config) => {
  const token = getAccessToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

httpClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (!axios.isAxiosError(error)) {
      return Promise.reject(error)
    }

    if (shouldTryRefresh(error)) {
      const originalRequest = error.config as RetriableRequestConfig
      originalRequest._retry = true

      try {
        const refreshedSession = await refreshSession()
        const tokenType = refreshedSession.tipoToken || 'Bearer'

        originalRequest.headers.Authorization = `${tokenType} ${refreshedSession.tokenAcesso}`

        return httpClient(originalRequest)
      } catch (refreshError) {
        clearAuthSession()
        return Promise.reject(refreshError)
      }
    }

    if (error.response?.status === 401 && !isAuthRoute(error.config?.url)) {
      clearAuthSession()
    }

    return Promise.reject(error)
  },
)
