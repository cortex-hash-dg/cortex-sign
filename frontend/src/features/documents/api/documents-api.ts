import { httpClient } from '../../../services/api/http-client'
import { getAccessToken } from '../../auth/lib/auth-storage'

export type DocumentStatus =
  | 'RASCUNHO'
  | 'ENVIADO_PARA_ASSINATURA'
  | 'ASSINADO'
  | 'CANCELADO'
  | 'REJEITADO'
  | 'ARQUIVADO'

export type StorageProviderType = 'LOCAL' | 'NEXTCLOUD'

export type StoredFile = {
  id: string
  provedor: StorageProviderType
  nomeOriginal: string
  nomeArmazenado: string
  caminho: string
  tipoConteudo: string | null
  tamanhoBytes: number | null
  checksumSha256: string | null
  criadoEm: string
}

export type DocumentVersion = {
  id: string
  numeroVersao: number
  arquivo: StoredFile
  criadoPorUsuarioId: string
  criadoPorUsuarioNome: string
  criadoEm: string
}

export type DocumentSummary = {
  id: string
  organizacaoId: string
  organizacaoNome: string
  titulo: string
  status: DocumentStatus
  criadoPorUsuarioId: string
  criadoPorUsuarioNome: string
  criadoEm: string
  atualizadoEm: string
}

export type DocumentDetail = DocumentSummary & {
  arquivoAtual: StoredFile | null
  versoes: DocumentVersion[]
}

export type CreateDocumentPayload = {
  organizacaoId?: string
  titulo?: string
  arquivo: File
}

export type UpdateDocumentPayload = {
  titulo: string
}

export type SignerType = 'INTERNO' | 'EXTERNO'

export type Signer = {
  id: string
  documentoId: string
  documentoTitulo: string
  nome: string
  email: string
  numeroDocumento: string | null
  telefone: string | null
  tipo: SignerType
  ordemAssinatura: number
  criadoEm: string
  atualizadoEm: string
}

export type CreateSignerPayload = {
  nome: string
  email: string
  numeroDocumento?: string
  telefone?: string
  tipo: SignerType
  ordemAssinatura: number
}

export type UpdateSignerPayload = CreateSignerPayload

export type SignatureRequestStatus =
  | 'PENDENTE'
  | 'ASSINADA'
  | 'REJEITADA'
  | 'CANCELADA'
  | 'EXPIRADA'

export type SignatureStatus = 'VALIDA' | 'INVALIDA' | 'REJEITADA'
export type SignatureType = 'LINK_TOKEN' | 'TOKEN_CODIGO' | 'ICP_BRASIL' | 'MOCK'
export type SignatureProviderType = 'MOCK' | 'CORTEX_SIGN' | 'ICP_BRASIL'

export type Signature = {
  id: string
  solicitacaoAssinaturaId: string
  tipo: SignatureType
  status: SignatureStatus
  provedor: SignatureProviderType
  protocolo: string | null
  assinadoEm: string | null
  rejeitadoEm: string | null
  motivoRejeicao: string | null
  documentoHashSha256: string | null
  evidenciaHashSha256: string | null
  termoAceite: string | null
  metadados: string | null
  criadoEm: string
}

export type SignatureRequest = {
  id: string
  documentoId: string
  documentoTitulo: string
  signatarioId: string
  signatarioNome: string
  signatarioEmail: string
  status: SignatureRequestStatus
  token: string | null
  expiraEm: string | null
  assinatura: Signature | null
  criadoEm: string
  atualizadoEm: string
}

export type CreateSignatureRequestPayload = {
  signatarioId: string
  validadeDias?: number
  semValidade?: boolean
}

export type SignatureInvitationChannel = 'EMAIL'

export type SendSignatureInvitationResponse = {
  canal: SignatureInvitationChannel
  destino: string
  mensagem: string
  solicitadoEm: string
}

export async function listDocuments(organizationId?: string) {
  const response = await httpClient.get<DocumentSummary[]>('/documentos', {
    params: organizationId ? { organizacaoId: organizationId } : undefined,
  })

  return response.data
}

export async function createDocument(payload: CreateDocumentPayload) {
  const formData = new FormData()

  if (payload.organizacaoId) {
    formData.append('organizacaoId', payload.organizacaoId)
  }

  if (payload.titulo) {
    formData.append('titulo', payload.titulo)
  }

  formData.append('arquivo', payload.arquivo)

  const token = getAccessToken()

  const response = await httpClient.post<DocumentDetail>('/documentos', formData, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  })

  return response.data
}

export async function getDocument(id: string) {
  const response = await httpClient.get<DocumentDetail>(`/documentos/${id}`)
  return response.data
}

export async function updateDocument(id: string, payload: UpdateDocumentPayload) {
  const response = await httpClient.put<DocumentDetail>(`/documentos/${id}`, payload)
  return response.data
}

export async function archiveDocument(id: string) {
  await httpClient.delete(`/documentos/${id}`)
}

export async function downloadDocument(id: string) {
  const response = await httpClient.get<Blob>(`/documentos/${id}/download`, {
    responseType: 'blob',
  })

  return response.data
}

export async function listSigners(documentId: string) {
  const response = await httpClient.get<Signer[]>(`/documentos/${documentId}/signatarios`)
  return response.data
}

export async function createSigner(documentId: string, payload: CreateSignerPayload) {
  const response = await httpClient.post<Signer>(`/documentos/${documentId}/signatarios`, payload)
  return response.data
}

export async function updateSigner(id: string, payload: UpdateSignerPayload) {
  const response = await httpClient.put<Signer>(`/signatarios/${id}`, payload)
  return response.data
}

export async function deleteSigner(id: string) {
  await httpClient.delete(`/signatarios/${id}`)
}

export async function listSignatureRequests(documentId: string) {
  const response = await httpClient.get<SignatureRequest[]>(`/documentos/${documentId}/solicitacoes-assinatura`)
  return response.data
}

export async function createSignatureRequest(documentId: string, payload: CreateSignatureRequestPayload) {
  const response = await httpClient.post<SignatureRequest>(
    `/documentos/${documentId}/solicitacoes-assinatura`,
    payload,
  )

  return response.data
}

export async function cancelSignatureRequest(id: string) {
  await httpClient.post(`/solicitacoes-assinatura/${id}/cancelar`)
}

export async function sendSignatureInvitation(token: string, canal: SignatureInvitationChannel) {
  const response = await httpClient.post<SendSignatureInvitationResponse>(
    `/publico/assinaturas/${token}/codigo`,
    { canal },
  )

  return response.data
}
