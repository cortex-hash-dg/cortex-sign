import { ChevronDown, ChevronFirst, ChevronLast, LogOut, Moon, Sun } from 'lucide-react'
import { createContext, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { NavLink, useLocation } from 'react-router-dom'

import xsignLogo from '../../../assets/brand/xsign-logo-login.png'
import { getNavigationSections, type NavigationItem, type NavigationSection } from '../../navigation/navigation-items'
import { cn } from '../../../shared/lib/cn'
import { type AppContext } from '../../../features/context/app-context'
import { type AuthenticatedUser, type UserRole } from '../../../features/auth/types/auth'

type AppSidebarProps = {
  isCollapsed: boolean
  isMobileOpen: boolean
  currentContext: AppContext
  theme: AppTheme
  user?: AuthenticatedUser
  userRole?: UserRole
  onThemeChange: (theme: AppTheme) => void
  onToggleCollapsed: () => void
  onCloseMobile: () => void
  onLogout: () => void
}

type AppTheme = 'light' | 'dark'

type SidebarContextValue = {
  expanded: boolean
}

const SidebarContext = createContext<SidebarContextValue>({ expanded: true })

export function AppSidebar({
  isCollapsed,
  isMobileOpen,
  currentContext,
  theme,
  user,
  userRole,
  onThemeChange,
  onToggleCollapsed,
  onCloseMobile,
  onLogout,
}: AppSidebarProps) {
  const expanded = !isCollapsed || isMobileOpen
  const location = useLocation()
  const navigationSections = useMemo(() => getNavigationSections(currentContext, userRole), [currentContext, userRole])
  const [isAccountMenuOpen, setIsAccountMenuOpen] = useState(false)
  const accountMenuRef = useRef<HTMLDivElement>(null)
  const [openSectionIds, setOpenSectionIds] = useState(() =>
    new Set(navigationSections.map((section) => section.id)),
  )

  useEffect(() => {
    setOpenSectionIds(new Set(navigationSections.map((section) => section.id)))
  }, [navigationSections])

  useEffect(() => {
    if (!isAccountMenuOpen) {
      return
    }

    function closeOnOutsideClick(event: PointerEvent) {
      if (!accountMenuRef.current?.contains(event.target as Node)) {
        setIsAccountMenuOpen(false)
      }
    }

    document.addEventListener('pointerdown', closeOnOutsideClick)

    return () => document.removeEventListener('pointerdown', closeOnOutsideClick)
  }, [isAccountMenuOpen])

  useEffect(() => {
    setIsAccountMenuOpen(false)
  }, [location.pathname])

  function toggleSection(sectionId: string) {
    setOpenSectionIds((currentSectionIds) => {
      const nextSectionIds = new Set(currentSectionIds)

      if (nextSectionIds.has(sectionId)) {
        nextSectionIds.delete(sectionId)
      } else {
        nextSectionIds.add(sectionId)
      }

      return nextSectionIds
    })
  }

  return (
    <aside
      className={cn(
        'fixed inset-y-0 left-0 z-50 h-screen w-[min(18rem,calc(100vw-1rem))] -translate-x-full transition-transform duration-200 sm:w-72 lg:static lg:w-auto lg:translate-x-0',
        isMobileOpen && 'translate-x-0',
      )}
    >
      <nav className="flex h-full flex-col border-r border-white/10 bg-brand-900 shadow-sm">
        <div
          className={cn(
            'flex h-16 shrink-0 items-center px-4',
            expanded ? 'justify-between gap-3' : 'justify-center',
          )}
        >
          {expanded && (
            <img
              src={xsignLogo}
              className="h-auto w-32 object-contain brightness-0 invert"
              alt="Xsign"
            />
          )}

          <button
            onClick={isMobileOpen ? onCloseMobile : onToggleCollapsed}
            className={cn(
              'grid size-8 shrink-0 place-items-center rounded-lg text-blue-50/72 transition-colors hover:bg-white/10 hover:text-white',
              expanded && 'ml-auto',
            )}
            type="button"
            aria-label={expanded ? 'Recolher sidebar' : 'Expandir sidebar'}
          >
            {expanded ? <ChevronFirst size={20} /> : <ChevronLast size={20} />}
          </button>
        </div>

        <SidebarContext.Provider value={{ expanded }}>
          <ul className={cn('flex-1 px-3 py-4', expanded ? 'overflow-y-auto' : 'overflow-visible')}>
            {navigationSections.map((section) => {
              const isOpen = openSectionIds.has(section.id)
              const isSectionActive = section.path
                ? location.pathname === section.path
                : section.items.some((item) => location.pathname === item.path)

              return (
                <SidebarSection
                  key={section.id}
                  section={section}
                  active={isSectionActive}
                  open={isOpen}
                  onToggle={() => toggleSection(section.id)}
                  onCloseMobile={onCloseMobile}
                />
              )
            })}
          </ul>

          <div className="relative border-t border-white/10 p-3" ref={accountMenuRef}>
            <SidebarAccountFooter
              theme={theme}
              user={user}
              open={isAccountMenuOpen}
              onThemeChange={onThemeChange}
              onToggle={() => setIsAccountMenuOpen((current) => !current)}
              onLogout={onLogout}
            />
          </div>
        </SidebarContext.Provider>
      </nav>
    </aside>
  )
}

function getInitials(name?: string) {
  if (!name) {
    return 'US'
  }

  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase()
}

function SidebarAccountFooter({
  theme,
  user,
  open,
  onThemeChange,
  onToggle,
  onLogout,
}: {
  theme: AppTheme
  user?: AuthenticatedUser
  open: boolean
  onThemeChange: (theme: AppTheme) => void
  onToggle: () => void
  onLogout: () => void
}) {
  const { expanded } = useContext(SidebarContext)

  return (
    <div className="relative">
      {open && (
        <div
          className={cn(
            'absolute z-50 overflow-hidden rounded-card border border-blue-200 bg-brand-50 p-2 shadow-[0_20px_50px_rgba(6,38,85,0.18)]',
            expanded
              ? 'bottom-[calc(100%+0.5rem)] left-0 right-0'
              : 'bottom-0 left-full ml-5 min-w-56',
          )}
        >
          <div className="mb-2 rounded-md bg-white/72 p-2">
            <p className="text-xs font-semibold uppercase tracking-wide text-brand-900/70">Tema</p>
            <div className="mt-2 grid grid-cols-2 gap-1">
              <button
                className={cn(
                  'inline-flex min-h-9 items-center justify-center gap-1.5 rounded-md px-2 text-xs font-medium transition-colors',
                  theme === 'light'
                    ? 'bg-brand-500 text-white shadow-card'
                    : 'text-brand-900 hover:bg-white',
                )}
                type="button"
                onClick={() => onThemeChange('light')}
              >
                <Sun aria-hidden="true" size={15} />
                Claro
              </button>

              <button
                className={cn(
                  'inline-flex min-h-9 items-center justify-center gap-1.5 rounded-md px-2 text-xs font-medium transition-colors',
                  theme === 'dark'
                    ? 'bg-brand-500 text-white shadow-card'
                    : 'text-brand-900 hover:bg-white',
                )}
                type="button"
                onClick={() => onThemeChange('dark')}
              >
                <Moon aria-hidden="true" size={15} />
                Escuro
              </button>
            </div>
          </div>

          <button
            className="flex min-h-10 w-full items-center gap-2 rounded-md px-3 text-left text-sm font-medium text-red-600 transition-colors hover:bg-white"
            type="button"
            onClick={onLogout}
          >
            <LogOut aria-hidden="true" size={17} />
            Sair
          </button>
        </div>
      )}

      <button
        className={cn(
          'group flex min-h-12 w-full items-center rounded-md text-left transition-colors hover:bg-white/10',
          expanded ? 'gap-3 px-2.5' : 'justify-center px-0',
        )}
        type="button"
        onClick={onToggle}
        aria-expanded={open}
      >
        <span className="grid size-9 shrink-0 place-items-center rounded-md bg-white text-xs font-semibold text-brand-900">
          {getInitials(user?.nome)}
        </span>

        <span
          className={cn(
            'min-w-0 overflow-hidden transition-all duration-200',
            expanded ? 'w-44 opacity-100' : 'w-0 opacity-0',
          )}
        >
          <span className="block truncate text-sm font-semibold text-white">
            {user?.nome ?? 'Usuário'}
          </span>
          <span className="block truncate text-xs text-blue-50/62">
            {user?.email ?? 'Conta conectada'}
          </span>
        </span>

        <ChevronDown
          aria-hidden="true"
          className={cn(
            'ml-auto shrink-0 text-blue-50/62 transition-transform duration-200',
            open && 'rotate-180',
            !expanded && 'hidden',
          )}
          size={16}
        />

        {!expanded && (
          <span className="invisible absolute left-full z-50 ml-6 rounded-md bg-blue-50 px-2 py-1 text-sm font-medium text-brand-900 opacity-20 shadow-card transition-all duration-200 -translate-x-3 group-hover:visible group-hover:translate-x-0 group-hover:opacity-100">
            {user?.nome ?? 'Conta'}
          </span>
        )}
      </button>
    </div>
  )
}

function SidebarSection({
  section,
  active,
  open,
  onToggle,
  onCloseMobile,
}: {
  section: NavigationSection
  active: boolean
  open: boolean
  onToggle: () => void
  onCloseMobile: () => void
}) {
  const { expanded } = useContext(SidebarContext)
  const SectionIcon = section.icon
  const hasChildren = section.items.length > 0
  const hasDivider = ['principal', 'documentos', 'assinatura', 'conta', 'gestao'].includes(section.id)

  return (
    <li
      className={cn(
        'group/section relative mb-4',
        hasDivider && 'border-b border-white/10 pb-4',
      )}
    >
      {section.id === 'principal' && (
        <div
          className={cn(
            'mb-1 flex min-h-7 items-center px-3 text-xs font-medium uppercase tracking-wide text-blue-50/55',
            !expanded && 'invisible',
          )}
        >
          Inicio
        </div>
      )}

      {section.path ? (
        <NavLink to={section.path} onClick={onCloseMobile}>
          {({ isActive }) => (
            <SidebarSectionHeader
              icon={<SectionIcon aria-hidden="true" size={19} />}
              text={section.label}
              active={isActive}
            />
          )}
        </NavLink>
      ) : (
        <button className="w-full text-left" type="button" onClick={onToggle}>
          <SidebarSectionHeader
            icon={<SectionIcon aria-hidden="true" size={19} />}
            text={section.label}
            active={active}
            hideTooltip={hasChildren}
            trailing={expanded && hasChildren ? (
              <ChevronDown
                aria-hidden="true"
                className={cn('ml-auto transition-transform duration-200', !open && '-rotate-90')}
                size={15}
              />
            ) : null}
          />
        </button>
      )}

      {hasChildren && expanded && (
        <div
          className={cn(
            'grid transition-[grid-template-rows,opacity,transform] duration-200 ease-out',
            open ? 'grid-rows-[1fr] opacity-100 translate-y-0' : 'grid-rows-[0fr] opacity-0 -translate-y-1',
          )}
        >
          <ul className="mt-1.5 ml-[1.15rem] grid min-h-0 gap-1 overflow-hidden border-l border-white/14 pl-5">
            {section.items.map((item) => (
              <SidebarSubItem
                item={item}
                key={item.path}
                onCloseMobile={onCloseMobile}
              />
            ))}
          </ul>
        </div>
      )}

      {hasChildren && !expanded && (
        <div className="invisible absolute left-full top-0 z-50 ml-5 min-w-48 rounded-md border border-line bg-white p-1.5 opacity-0 shadow-[0_20px_50px_rgba(6,38,85,0.16)] transition-all duration-200 -translate-x-2 group-hover/section:visible group-hover/section:translate-x-0 group-hover/section:opacity-100">
          <p className="px-2 py-1 text-xs font-semibold uppercase tracking-wide text-muted">{section.label}</p>
          <ul className="grid gap-1">
            {section.items.map((item) => (
              <SidebarSubItem
                item={item}
                key={item.path}
                onCloseMobile={onCloseMobile}
                floating
              />
            ))}
          </ul>
        </div>
      )}
    </li>
  )
}

function SidebarSectionHeader({
  icon,
  text,
  active,
  trailing,
  hideTooltip = false,
}: {
  icon: ReactNode
  text: string
  active?: boolean
  trailing?: ReactNode
  hideTooltip?: boolean
}) {
  const { expanded } = useContext(SidebarContext)

  return (
    <div
      className={cn(
        'group relative my-0.5 flex min-h-10 cursor-pointer items-center rounded-md px-3 text-sm font-medium transition-colors',
        active
          ? 'bg-white text-brand-900 shadow-[0_14px_28px_rgba(0,0,0,0.16)]'
          : 'text-blue-50/72 hover:bg-white/10 hover:text-white',
        !expanded && 'justify-center px-0',
      )}
    >
      {icon}
      <span
        className={cn(
          'overflow-hidden whitespace-nowrap transition-all duration-200',
          expanded ? 'ml-3 w-48 opacity-100' : 'w-0 opacity-0',
        )}
      >
        {text}
      </span>
      {trailing}

      {!expanded && !hideTooltip && (
        <span className="invisible absolute left-full z-50 ml-6 rounded-md bg-blue-50 px-2 py-1 text-sm font-medium text-brand-900 opacity-20 shadow-card transition-all duration-200 -translate-x-3 group-hover:visible group-hover:translate-x-0 group-hover:opacity-100">
          {text}
        </span>
      )}
    </div>
  )
}

function SidebarSubItem({
  item,
  onCloseMobile,
  floating = false,
}: {
  item: NavigationItem
  onCloseMobile: () => void
  floating?: boolean
}) {
  const content = (
    <NavLink
      className={({ isActive }) =>
        cn(
          'block rounded-md px-3 py-2 text-sm font-medium transition-colors',
          floating
            ? isActive
              ? 'bg-brand-50 text-brand-900'
              : 'text-muted hover:bg-brand-50 hover:text-brand-900'
            : isActive
              ? 'text-white'
              : 'text-blue-50/60 hover:text-white',
        )
      }
      to={item.path}
      onClick={onCloseMobile}
    >
      {item.label}
    </NavLink>
  )

  if (item.openInNewTab) {
    return (
      <a
        className={cn(
          'block rounded-md px-3 py-2 text-sm font-medium transition-colors',
          floating ? 'text-muted hover:bg-brand-50 hover:text-brand-900' : 'text-blue-50/60 hover:text-white',
        )}
        href={item.path}
        onClick={onCloseMobile}
        rel="noreferrer"
        target="_blank"
      >
        {item.label}
      </a>
    )
  }

  return content
}
