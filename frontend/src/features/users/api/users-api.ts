import { httpClient } from '../../../services/api/http-client'

export type UserRole =
  | 'SUPER_ADMINISTRADOR'
  | 'ADMINISTRADOR_ORGANIZACAO'
  | 'GESTOR'
  | 'OPERADOR'
  | 'AUDITOR'

export type User = {
  id: string
  organizacaoId: string
  organizacaoNome: string
  nome: string
  email: string
  cpf?: string | null
  telefone?: string | null
  perfil: UserRole
  ativo: boolean
  criadoEm: string
  atualizadoEm: string
}

export type CreateUserPayload = {
  organizacaoId?: string
  nome: string
  email: string
  senha: string
  perfil: UserRole
}

export type UpdateUserPayload = {
  organizacaoId?: string
  nome: string
  email: string
  perfil: UserRole
  ativo: boolean
}

export async function listUsers() {
  const response = await httpClient.get<User[]>('/usuarios')
  return response.data
}

export async function createUser(payload: CreateUserPayload) {
  const response = await httpClient.post<User>('/usuarios', payload)
  return response.data
}

export async function updateUser(id: string, payload: UpdateUserPayload) {
  const response = await httpClient.put<User>(`/usuarios/${id}`, payload)
  return response.data
}

export async function deleteUser(id: string) {
  await httpClient.delete(`/usuarios/${id}`)
}
