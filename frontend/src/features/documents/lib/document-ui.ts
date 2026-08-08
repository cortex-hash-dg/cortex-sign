import type { DocumentStatus, SignatureRequestStatus } from '../api/documents-api'

export const documentStatusLabels: Record<DocumentStatus, string> = {
  RASCUNHO: 'Rascunho',
  ENVIADO_PARA_ASSINATURA: 'Aguardando assinatura',
  ASSINADO: 'Assinado',
  CANCELADO: 'Cancelado',
  REJEITADO: 'Rejeitado',
  ARQUIVADO: 'Arquivado',
}

export const signatureRequestStatusLabels: Record<SignatureRequestStatus, string> = {
  PENDENTE: 'Pendente',
  ASSINADA: 'Assinada',
  REJEITADA: 'Rejeitada',
  CANCELADA: 'Cancelada',
  EXPIRADA: 'Expirada',
}

export function documentStatusTone(status: DocumentStatus) {
  if (status === 'ASSINADO') {
    return 'green' as const
  }

  if (status === 'ENVIADO_PARA_ASSINATURA') {
    return 'orange' as const
  }

  if (status === 'CANCELADO' || status === 'REJEITADO') {
    return 'red' as const
  }

  if (status === 'RASCUNHO') {
    return 'blue' as const
  }

  return 'slate' as const
}

export function signatureRequestStatusTone(status: SignatureRequestStatus) {
  if (status === 'ASSINADA') {
    return 'green' as const
  }

  if (status === 'PENDENTE') {
    return 'orange' as const
  }

  if (status === 'REJEITADA' || status === 'CANCELADA' || status === 'EXPIRADA') {
    return 'red' as const
  }

  return 'slate' as const
}

export function formatDate(value?: string | null) {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

export function formatSize(value?: number | null) {
  if (!value) {
    return '-'
  }

  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }

  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

export function getPublicSignatureLink(token: string) {
  const isLocalHost = ['localhost', '127.0.0.1'].includes(window.location.hostname)
  const protocol = isLocalHost ? 'http:' : window.location.protocol

  return `${protocol}//${window.location.host}/assinaturas/${token}`
}
