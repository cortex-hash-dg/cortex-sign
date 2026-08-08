import { httpClient } from '../../../services/api/http-client'
import type { LoginRequest, LoginResponse } from '../types/auth'

export async function login(request: LoginRequest) {
  const response = await httpClient.post<LoginResponse>('/auth/login', request)
  return response.data
}
