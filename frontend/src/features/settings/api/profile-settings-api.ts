import { httpClient } from '../../../services/api/http-client'

export type ProfileSettings = {
  id: string
  nome: string
  email: string
  cpf: string | null
  telefone: string | null
  nomeSocial: string | null
  dataNascimento: string | null
  emailVerificado: boolean
  telefoneVerificado: boolean
}

export type UpdateProfileSettingsPayload = {
  nome: string
  email: string
  cpf?: string | null
  telefone?: string | null
  nomeSocial?: string | null
  dataNascimento?: string | null
}

export async function getProfileSettings() {
  const response = await httpClient.get<ProfileSettings>('/auth/me/cadastro')
  return response.data
}

export async function updateProfileSettings(payload: UpdateProfileSettingsPayload) {
  const response = await httpClient.put<ProfileSettings>('/auth/me/cadastro', payload)
  return response.data
}
