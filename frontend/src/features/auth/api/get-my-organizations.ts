import { httpClient } from '../../../services/api/http-client'

export type OrganizationMembership = {
  id: string
  usuarioId: string
  usuarioNome: string
  usuarioEmail: string
  organizacaoId: string
  organizacaoNome: string
  papel: 'PROPRIETARIO' | 'ADMINISTRADOR' | 'GESTOR' | 'OPERADOR' | 'AUDITOR'
  status: 'CONVIDADO' | 'ATIVO' | 'SUSPENSO' | 'REMOVIDO'
  convidadoEm?: string | null
  aceitoEm?: string | null
  suspensoEm?: string | null
  removidoEm?: string | null
  criadoEm: string
  atualizadoEm: string
}

export async function getMyOrganizations() {
  const response = await httpClient.get<OrganizationMembership[]>('/auth/me/organizacoes')
  return response.data
}
