import { httpClient } from '../../../services/api/http-client'

export type EmailVerificationRequested = {
  email: string
  mensagem: string
  expiraEm: string | null
}

export type EmailVerificationConfirmed = {
  emailVerificado: boolean
  mensagem: string
}

export async function requestEmailVerification() {
  const response = await httpClient.post<EmailVerificationRequested>('/auth/email/confirmacao')
  return response.data
}

export async function confirmEmailVerification(token: string) {
  const response = await httpClient.post<EmailVerificationConfirmed>('/auth/email/confirmacao/validar', {
    token,
  })
  return response.data
}
