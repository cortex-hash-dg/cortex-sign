import { type PropsWithChildren, type ReactNode } from 'react'

import { cn } from '../lib/cn'

type PageSectionProps = PropsWithChildren<{
  title: string
  action?: ReactNode
  className?: string
}>

export function PageSection({ title, action, className, children }: PageSectionProps) {
  return (
    <section className={cn('overflow-hidden rounded-card border border-line bg-white shadow-card', className)}>
      <div className="flex items-center justify-between gap-3 border-b border-line px-4 py-3.5 2xl:px-5 2xl:py-4">
        <h2 className="text-base font-semibold text-ink">{title}</h2>
        {action}
      </div>
      {children}
    </section>
  )
}
