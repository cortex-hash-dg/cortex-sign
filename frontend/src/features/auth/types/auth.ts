export type UserRole =
  | 'SUPER_ADMINISTRADOR'
  | 'ADMINISTRADOR_ORGANIZACAO'
  | 'GESTOR'
  | 'OPERADOR'
  | 'AUDITOR'

export interface LoginRequest {
  email: string
  senha: string
}

export interface LoginResponse {
  tokenAcesso: string
  tokenAtualizacao: string
  tipoToken: string
  tokenAcessoExpiraEm: string
  tokenAtualizacaoExpiraEm: string
  usuarioId: string
  organizacaoId: string
  organizacaoNome: string
  nome: string
  email: string
  perfil: UserRole
}

export interface AuthenticatedUser {
  id: string
  organizacaoId: string
  organizacaoNome: string
  nome: string
  email: string
  perfil: UserRole
  cpf?: string | null
  telefone?: string | null
  nomeSocial?: string | null
  dataNascimento?: string | null
  emailVerificado?: boolean
  telefoneVerificado?: boolean
}
