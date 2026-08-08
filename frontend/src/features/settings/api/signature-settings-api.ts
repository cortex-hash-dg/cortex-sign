import { httpClient } from '../../../services/api/http-client'

export type SignatureSettings = {
  nomeAssinatura: string | null
  assinaturaManuscritaBase64: string | null
  possuiAssinaturaManuscrita: boolean
}

export type UpdateSignatureSettingsPayload = {
  nomeAssinatura?: string | null
  assinaturaManuscritaBase64?: string | null
}

export async function getSignatureSettings() {
  const response = await httpClient.get<SignatureSettings>('/auth/me/assinatura')
  return response.data
}

export async function updateSignatureSettings(payload: UpdateSignatureSettingsPayload) {
  const response = await httpClient.put<SignatureSettings>('/auth/me/assinatura', payload)
  return response.data
}
