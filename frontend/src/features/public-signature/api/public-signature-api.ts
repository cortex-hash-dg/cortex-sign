import axios from 'axios'

import type { SignatureRequest, SignatureType } from '../../documents/api/documents-api'

const publicHttpClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

export type PublicSignature = {
  solicitacaoAssinaturaId: string
  documentoId: string
  documentoTitulo: string
  signatarioNome: string
  signatarioEmail: string
  acessoExternoObrigatorio: boolean
  status: SignatureRequest['status']
  expiraEm: string | null
}

export type SignatureLinkChannel = 'EMAIL'

export type RequestSignatureLinkResponse = {
  canal: SignatureLinkChannel
  destino: string
  mensagem: string
  solicitadoEm: string
}

export type SignWithTokenPayload = {
  codigo?: string
  tipo: SignatureType
  assinaturaManuscritaBase64?: string
  tokenAcessoExterno?: string
}

export type RejectWithTokenPayload = {
  codigo?: string
  motivo: string
  tokenAcessoExterno?: string
}

export type RequestExternalSignerAccessPayload = {
  cpf: string
  canal: SignatureLinkChannel
}

export type ValidateExternalSignerAccessPayload = {
  cpf: string
  codigo: string
}

export type ExternalSignerAccessResponse = {
  tokenAcesso: string
  expiraEm: string
  mensagem: string
}

export async function getPublicSignature(token: string) {
  const response = await publicHttpClient.get<PublicSignature>(`/publico/assinaturas/${token}`)
  return response.data
}

export async function requestSignatureLink(token: string, canal: SignatureLinkChannel) {
  const response = await publicHttpClient.post<RequestSignatureLinkResponse>(
    `/publico/assinaturas/${token}/codigo`,
    { canal },
  )

  return response.data
}

export async function requestExternalSignerAccess(token: string, payload: RequestExternalSignerAccessPayload) {
  const response = await publicHttpClient.post<RequestSignatureLinkResponse>(
    `/publico/assinaturas/${token}/acesso/solicitar`,
    payload,
  )

  return response.data
}

export async function validateExternalSignerAccess(token: string, payload: ValidateExternalSignerAccessPayload) {
  const response = await publicHttpClient.post<ExternalSignerAccessResponse>(
    `/publico/assinaturas/${token}/acesso/validar`,
    payload,
  )

  return response.data
}

export async function signWithToken(token: string, payload: SignWithTokenPayload) {
  const response = await publicHttpClient.post<SignatureRequest>(`/publico/assinaturas/${token}/assinar`, payload)
  return response.data
}

export async function rejectWithToken(token: string, payload: RejectWithTokenPayload) {
  const response = await publicHttpClient.post<SignatureRequest>(`/publico/assinaturas/${token}/rejeitar`, payload)
  return response.data
}
