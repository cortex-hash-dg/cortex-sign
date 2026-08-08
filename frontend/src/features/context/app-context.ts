import { type OrganizationMembership } from '../auth/api/get-my-organizations'

export type OrganizationRole = OrganizationMembership['papel']

export type AppContext =
  | {
      id: 'personal'
      type: 'PERSONAL'
      label: 'Espaço pessoal'
      description: string
      role: 'OPERADOR'
    }
  | {
      id: string
      type: 'ORGANIZATION'
      label: string
      description: string
      role: OrganizationRole
      membershipId: string
    }

export const CURRENT_CONTEXT_STORAGE_KEY = '@cortex-sign/current-context'

export const personalContext: AppContext = {
  id: 'personal',
  type: 'PERSONAL',
  label: 'Espaço pessoal',
  description: 'Documentos e assinatura da sua conta',
  role: 'OPERADOR',
}

export function membershipToContext(membership: OrganizationMembership): AppContext {
  return {
    id: membership.organizacaoId,
    type: 'ORGANIZATION',
    label: membership.organizacaoNome,
    description: formatOrganizationRole(membership.papel),
    role: membership.papel,
    membershipId: membership.id,
  }
}

export function resolveStoredContext(contexts: AppContext[], storedContextId: string | null) {
  return contexts.find((context) => context.id === storedContextId) ?? contexts[0] ?? personalContext
}

export function formatOrganizationRole(role: OrganizationRole) {
  const labels: Record<OrganizationRole, string> = {
    PROPRIETARIO: 'Proprietário',
    ADMINISTRADOR: 'Administrador',
    GESTOR: 'Gestor',
    OPERADOR: 'Operador',
    AUDITOR: 'Auditor',
  }

  return labels[role] ?? role
}
