import { useState } from 'react'
import { Building2, ChevronDown, Menu } from 'lucide-react'

import { type AppContext } from '../../../features/context/app-context'

type AppHeaderProps = {
  contexts: AppContext[]
  currentContext: AppContext
  onContextChange: (context: AppContext) => void
  onOpenMobileMenu: () => void
}

export function AppHeader({
  contexts,
  currentContext,
  onContextChange,
  onOpenMobileMenu,
}: AppHeaderProps) {
  const [isContextMenuOpen, setIsContextMenuOpen] = useState(false)
  const organizationContexts = contexts.filter((context) => context.type === 'ORGANIZATION')
  const hasMultipleOrganizations = organizationContexts.length > 1
  const selectedOrganization = currentContext.type === 'ORGANIZATION'
    ? currentContext
    : organizationContexts[0]
  const displayedContext = selectedOrganization ?? currentContext

  return (
    <header className="min-h-16 border-b border-line bg-white/92 px-3 py-3 backdrop-blur sm:px-6 lg:px-6 2xl:px-8">
      <div className="flex min-h-10 items-center gap-3 sm:gap-4">
        <button
          className="grid size-10 shrink-0 place-items-center rounded-control border border-line bg-white text-brand-900 shadow-card transition-colors hover:border-brand-500 lg:hidden"
          type="button"
          aria-label="Abrir menu"
          onClick={onOpenMobileMenu}
        >
          <Menu aria-hidden="true" size={20} />
        </button>

        <div className="min-w-0 flex-1" />

        <div className="relative ml-auto min-w-0">
            {hasMultipleOrganizations ? (
              <button
                className="inline-flex min-h-10 max-w-[min(18rem,calc(100vw-6rem))] items-center gap-2 rounded-control border border-line bg-white px-3 text-sm font-medium text-ink shadow-card transition-colors hover:border-brand-500"
                type="button"
                onClick={() => setIsContextMenuOpen((current) => !current)}
                aria-expanded={isContextMenuOpen}
              >
                <Building2 aria-hidden="true" className="shrink-0 text-brand-900" size={18} />
                <span className="truncate">{displayedContext.label}</span>
                <ChevronDown aria-hidden="true" className="shrink-0 text-muted" size={16} />
              </button>
            ) : (
              <div className="inline-flex min-h-10 max-w-[min(18rem,calc(100vw-6rem))] items-center gap-2 rounded-control border border-line bg-white px-3 text-sm font-medium text-ink shadow-card">
                <Building2 aria-hidden="true" className="shrink-0 text-brand-900" size={18} />
                <span className="truncate">{displayedContext.label}</span>
              </div>
            )}

            {hasMultipleOrganizations && isContextMenuOpen && (
              <div className="absolute right-0 top-[calc(100%+0.5rem)] z-30 w-72 overflow-hidden rounded-card border border-line bg-white shadow-[0_20px_50px_rgba(6,38,85,0.16)]">
                <div className="py-1">
                  {organizationContexts.map((context) => (
                    <button
                      className="flex w-full flex-col px-3 py-2.5 text-left text-sm transition-colors hover:bg-brand-50"
                      key={context.id}
                      type="button"
                      onClick={() => {
                        onContextChange(context)
                        setIsContextMenuOpen(false)
                      }}
                    >
                      <span className="font-medium text-ink">{context.label}</span>
                      <span className="text-xs text-muted">{context.description}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
      </div>
    </header>
  )
}
