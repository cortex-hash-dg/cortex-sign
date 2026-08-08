import { httpClient } from '../../../services/api/http-client'

export type Organization = {
  id: string
  nome: string
  numeroDocumento?: string | null
  criadoEm: string
  atualizadoEm: string
}

export type CreateOrganizationPayload = {
  nome: string
  numeroDocumento?: string
}

export type UpdateOrganizationPayload = CreateOrganizationPayload

export async function listOrganizations() {
  const response = await httpClient.get<Organization[]>('/organizacoes')
  return response.data
}

export async function createOrganization(payload: CreateOrganizationPayload) {
  const response = await httpClient.post<Organization>('/organizacoes', payload)
  return response.data
}

export async function updateOrganization(id: string, payload: UpdateOrganizationPayload) {
  const response = await httpClient.put<Organization>(`/organizacoes/${id}`, payload)
  return response.data
}

export async function deleteOrganization(id: string) {
  await httpClient.delete(`/organizacoes/${id}`)
}
