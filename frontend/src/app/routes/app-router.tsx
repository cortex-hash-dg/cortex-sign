import { Navigate, createBrowserRouter } from 'react-router-dom'

import { AuthenticatedLayout } from '../layouts/authenticated-layout'
import { GuestRoute } from '../../features/auth/components/guest-route'
import { ProtectedRoute } from '../../features/auth/components/protected-route'
import { LoginPage } from '../../features/auth/pages/login-page'
import { AuditPage } from '../../features/audit/pages/audit-page'
import { DashboardPage } from '../../features/dashboard/pages/dashboard-page'
import { DocumentsPage } from '../../features/documents/pages/documents-page'
import { StartSignaturePage } from '../../features/documents/pages/start-signature-page'
import { OrganizationsPage } from '../../features/organizations/pages/organizations-page'
import { PublicSignaturePage } from '../../features/public-signature/pages/public-signature-page'
import { PrivacyPolicyPage } from '../../features/legal/pages/privacy-policy-page'
import { UsersPage } from '../../features/users/pages/users-page'
import { ConfirmEmailPage } from '../../features/settings/pages/confirm-email-page'
import { SettingsPage } from '../../features/settings/pages/settings-page'
import { VerificationPage } from '../../features/verification/pages/verification-page'

export const appRouter = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/app/dashboard" replace />,
  },
  {
    path: '/assinaturas/:token',
    element: <PublicSignaturePage />,
  },
  {
    path: '/verificar/certificado/:certificadoId?',
    element: <VerificationPage />,
  },
  {
    path: '/politica-de-privacidade',
    element: <PrivacyPolicyPage />,
  },
  {
    path: '/confirmar-email',
    element: <ConfirmEmailPage />,
  },
  {
    element: <GuestRoute />,
    children: [
      {
        path: '/login',
        element: <LoginPage />,
      },
    ],
  },
  {
    path: '/app',
    element: <ProtectedRoute />,
    children: [
      {
        element: <AuthenticatedLayout />,
        children: [
          {
            index: true,
            element: <Navigate to="dashboard" replace />,
          },
          {
            path: 'dashboard',
            element: <DashboardPage />,
          },
          {
            path: 'documentos',
            element: <Navigate to="assinatura/documentos" replace />,
          },
          {
            path: 'assinatura/novo',
            element: <StartSignaturePage />,
          },
          {
            path: 'assinatura/documentos',
            element: <DocumentsPage />,
          },
          {
            path: 'usuarios',
            element: <UsersPage />,
          },
          {
            path: 'organizacoes',
            element: <OrganizationsPage />,
          },
          {
            path: 'auditoria',
            element: <AuditPage />,
          },
          {
            path: 'verificacao',
            element: <VerificationPage />,
          },
          {
            path: 'configuracoes',
            element: <SettingsPage />,
          },
        ],
      },
    ],
  },
])
