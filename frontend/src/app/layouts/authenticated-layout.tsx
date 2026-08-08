import { useQuery } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'

import { getMyOrganizations } from '../../features/auth/api/get-my-organizations'
import { clearAuthSession } from '../../features/auth/lib/auth-storage'
import { useCurrentUser } from '../../features/auth/hooks/use-current-user'
import {
  CURRENT_CONTEXT_STORAGE_KEY,
  membershipToContext,
  personalContext,
  resolveStoredContext,
  type AppContext,
} from '../../features/context/app-context'
import { cn } from '../../shared/lib/cn'
import { AppHeader } from './components/app-header'
import { AppSidebar } from './components/app-sidebar'

type AppTheme = 'light' | 'dark'

const THEME_STORAGE_KEY = 'cortex-sign-theme'

export function AuthenticatedLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const currentUserQuery = useCurrentUser()
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false)
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false)
  const [theme, setTheme] = useState<AppTheme>(() => {
    const storedTheme = localStorage.getItem(THEME_STORAGE_KEY)

    return storedTheme === 'dark' ? 'dark' : 'light'
  })
  const [selectedContextId, setSelectedContextId] = useState(() =>
    localStorage.getItem(CURRENT_CONTEXT_STORAGE_KEY),
  )

  const myOrganizationsQuery = useQuery({
    queryKey: ['my-organizations'],
    queryFn: getMyOrganizations,
    enabled: currentUserQuery.isSuccess,
  })

  const contexts = useMemo(
    () => [
      personalContext,
      ...(myOrganizationsQuery.data ?? []).map(membershipToContext),
    ],
    [myOrganizationsQuery.data],
  )

  const organizationContexts = useMemo(
    () => contexts.filter((context) => context.type === 'ORGANIZATION'),
    [contexts],
  )
  const shouldUseOnlyOrganization =
    organizationContexts.length === 1
    && (!selectedContextId || selectedContextId === personalContext.id)
  const currentContext = shouldUseOnlyOrganization
    ? organizationContexts[0]
    : resolveStoredContext(contexts, selectedContextId)

  useEffect(() => {
    setIsMobileSidebarOpen(false)
  }, [location.pathname])

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem(THEME_STORAGE_KEY, theme)
  }, [theme])

  useEffect(() => {
    if (shouldUseOnlyOrganization) {
      setSelectedContextId(organizationContexts[0].id)
      localStorage.setItem(CURRENT_CONTEXT_STORAGE_KEY, organizationContexts[0].id)
    }
  }, [organizationContexts, shouldUseOnlyOrganization])

  function logout() {
    clearAuthSession()
    navigate('/login', { replace: true })
  }

  function toggleSidebar() {
    setIsSidebarCollapsed((currentValue) => !currentValue)
  }

  function changeContext(context: AppContext) {
    setSelectedContextId(context.id)
    localStorage.setItem(CURRENT_CONTEXT_STORAGE_KEY, context.id)
  }

  if (currentUserQuery.isLoading) {
    return (
      <main className="grid min-h-screen place-items-center bg-surface-page px-4">
        <div className="rounded-card border border-line bg-white px-5 py-4 text-sm text-muted shadow-card">
          Carregando sua sessão...
        </div>
      </main>
    )
  }

  if (currentUserQuery.isError) {
    clearAuthSession()
    return <Navigate to="/login" replace />
  }

  return (
    <div
      className={cn(
        'h-svh overflow-hidden bg-surface-page text-ink transition-[grid-template-columns] duration-200 lg:grid',
        isSidebarCollapsed ? 'lg:grid-cols-[5.25rem_1fr]' : 'lg:grid-cols-[19rem_1fr]',
      )}
    >
      {isMobileSidebarOpen && (
        <button
          className="fixed inset-0 z-40 bg-brand-900/45 backdrop-blur-[2px] lg:hidden"
          type="button"
          aria-label="Fechar menu"
          onClick={() => setIsMobileSidebarOpen(false)}
        />
      )}

      <AppSidebar
        isCollapsed={isSidebarCollapsed}
        isMobileOpen={isMobileSidebarOpen}
        currentContext={currentContext}
        theme={theme}
        user={currentUserQuery.data}
        userRole={currentUserQuery.data?.perfil}
        onThemeChange={setTheme}
        onToggleCollapsed={toggleSidebar}
        onCloseMobile={() => setIsMobileSidebarOpen(false)}
        onLogout={logout}
      />

      <div className="flex h-full min-h-0 min-w-0 flex-col overflow-hidden">
        <AppHeader
          contexts={contexts}
          currentContext={currentContext}
          onContextChange={changeContext}
          onOpenMobileMenu={() => setIsMobileSidebarOpen(true)}
        />

        <main className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-3 py-4 sm:px-6 lg:px-6 lg:py-5 2xl:px-8">
          {currentUserQuery.data && !currentUserQuery.data.emailVerificado && (
            <div className="mb-4 flex flex-col gap-3 rounded-card border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-ink shadow-card sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p className="font-semibold">Confirme seu e-mail para proteger sua conta.</p>
                <p className="mt-1 text-xs leading-5 text-muted">
                  A confirmação libera comunicações importantes e deixa seu cadastro pronto para produção.
                </p>
              </div>
              <button
                className="inline-flex min-h-9 items-center justify-center rounded-control border border-brand-500 bg-white px-3 text-xs font-medium text-brand-500 transition-colors hover:bg-brand-500 hover:text-white"
                type="button"
                onClick={() => navigate('/app/configuracoes')}
              >
                Verificar agora
              </button>
            </div>
          )}
          <Outlet />
        </main>
      </div>
    </div>
  )
}
