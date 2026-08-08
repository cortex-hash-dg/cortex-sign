import { httpClient } from '../../../services/api/http-client'

export type OrganizationMemberRole =
  | 'PROPRIETARIO'
  | 'ADMINISTRADOR'
  | 'GESTOR'
  | 'OPERADOR'
  | 'AUDITOR'

export type OrganizationMemberStatus =
  | 'CONVIDADO'
  | 'ATIVO'
  | 'SUSPENSO'
  | 'REMOVIDO'

export type OrganizationMember = {
  id: string
  usuarioId: string
  usuarioNome: string
  usuarioEmail: string
  organizacaoId: string
  organizacaoNome: string
  papel: OrganizationMemberRole
  status: OrganizationMemberStatus
  convidadoEm?: string | null
  aceitoEm?: string | null
  suspensoEm?: string | null
  removidoEm?: string | null
  criadoEm: string
  atualizadoEm: string
}

export type AddOrganizationMemberPayload = {
  usuarioId: string
  papel: OrganizationMemberRole
}

export type UpdateOrganizationMemberPayload = {
  papel: OrganizationMemberRole
  status: OrganizationMemberStatus
  motivoSuspensao?: string
}

export async function listOrganizationMembers(organizationId: string) {
  const response = await httpClient.get<OrganizationMember[]>(`/organizacoes/${organizationId}/membros`)
  return response.data
}

export async function addOrganizationMember(
  organizationId: string,
  payload: AddOrganizationMemberPayload,
) {
  const response = await httpClient.post<OrganizationMember>(`/organizacoes/${organizationId}/membros`, payload)
  return response.data
}

export async function updateOrganizationMember(
  organizationId: string,
  memberId: string,
  payload: UpdateOrganizationMemberPayload,
) {
  const response = await httpClient.put<OrganizationMember>(
    `/organizacoes/${organizationId}/membros/${memberId}`,
    payload,
  )
  return response.data
}

export async function removeOrganizationMember(organizationId: string, memberId: string) {
  await httpClient.delete(`/organizacoes/${organizationId}/membros/${memberId}`)
}
