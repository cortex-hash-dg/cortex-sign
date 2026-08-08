import {
  FileText,
  LayoutDashboard,
  ScanLine,
  Settings,
  type LucideIcon,
} from 'lucide-react'

import { type AppContext } from '../../features/context/app-context'
import { type UserRole } from '../../features/auth/types/auth'

export type NavigationItem = {
  label: string
  path: string
  icon?: LucideIcon
  openInNewTab?: boolean
}

export type NavigationSection = {
  id: string
  label: string
  icon: LucideIcon
  path?: string
  items: NavigationItem[]
}

const commonMainSection: NavigationSection = {
  id: 'principal',
  label: 'Dashboard',
  path: '/app/dashboard',
  icon: LayoutDashboard,
  items: [],
}

const commonSignatureItems: NavigationItem[] = [
  { label: 'Assinar documento', path: '/app/assinatura/novo' },
  { label: 'Documentos', path: '/app/assinatura/documentos' },
]

const commonVerificationSection: NavigationSection = {
  id: 'verificacao',
  label: 'Validador',
  icon: ScanLine,
  items: [
    { label: 'Validar documento', path: '/app/verificacao' },
  ],
}

export function getNavigationSections(context?: AppContext, userRole?: UserRole): NavigationSection[] {
  const isGlobalAdmin = userRole === 'SUPER_ADMINISTRADOR' || userRole === 'ADMINISTRADOR_ORGANIZACAO'
  const isSuperAdmin = userRole === 'SUPER_ADMINISTRADOR'

  if (!context || context.type === 'PERSONAL') {
    return [
      commonMainSection,
      {
        id: 'documentos',
        label: 'Documentos',
        icon: FileText,
        items: commonSignatureItems,
      },
      {
        id: 'conta',
        label: 'Gerenciamento',
        icon: Settings,
        items: [
          ...(isGlobalAdmin ? [{ label: 'Usuários', path: '/app/usuarios' }] : []),
          ...(isSuperAdmin ? [{ label: 'Organizações', path: '/app/organizacoes' }] : []),
          { label: 'Configurações', path: '/app/configuracoes' },
        ],
      },
      commonVerificationSection,
    ]
  }

  const canManageOrganization = ['PROPRIETARIO', 'ADMINISTRADOR'].includes(context.role)
  const canManageOrganizations = isSuperAdmin

  return [
    commonMainSection,
    {
      id: 'assinatura',
      label: 'Documentos',
      icon: FileText,
      items: commonSignatureItems,
    },
    {
      id: 'gestao',
      label: 'Gerenciamento',
      icon: Settings,
      items: [
        ...(canManageOrganization ? [{ label: 'Usuários', path: '/app/usuarios' }] : []),
        ...(canManageOrganizations ? [{ label: 'Organizações', path: '/app/organizacoes' }] : []),
        { label: 'Configurações', path: '/app/configuracoes' },
      ],
    },
    commonVerificationSection,
  ]
}
