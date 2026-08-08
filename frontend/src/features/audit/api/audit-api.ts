import { httpClient } from '../../../services/api/http-client'

export type AuditLog = {
  id: string
  organizacaoId: string | null
  organizacaoNome: string | null
  usuarioId: string | null
  usuarioNome: string | null
  acao: string
  entidadeTipo: string | null
  entidadeId: string | null
  detalhes: string | null
  criadoEm: string
}

export async function listAuditLogs(organizationId?: string) {
  const response = await httpClient.get<AuditLog[]>('/auditorias', {
    params: organizationId ? { organizacaoId: organizationId } : undefined,
  })

  return response.data
}
