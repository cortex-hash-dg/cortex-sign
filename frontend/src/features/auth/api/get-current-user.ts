import { httpClient } from '../../../services/api/http-client'
import type { AuthenticatedUser } from '../types/auth'

export async function getCurrentUser() {
  const response = await httpClient.get<AuthenticatedUser>('/auth/me')
  return response.data
}
