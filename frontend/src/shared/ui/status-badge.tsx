import { CheckCircle2, Clock3, FileText, XCircle } from 'lucide-react'

import { cn } from '../lib/cn'

type StatusBadgeProps = {
  label: string
  tone?: 'blue' | 'green' | 'orange' | 'red' | 'slate'
}

const toneClasses = {
  blue: 'bg-blue-50 text-brand-500',
  green: 'bg-green-50 text-success',
  orange: 'bg-orange-50 text-warning',
  red: 'bg-red-50 text-danger',
  slate: 'bg-surface-page text-muted',
}

function StatusIcon({ tone }: Pick<StatusBadgeProps, 'tone'>) {
  if (tone === 'green') {
    return <CheckCircle2 aria-hidden="true" size={14} />
  }

  if (tone === 'orange') {
    return <Clock3 aria-hidden="true" size={14} />
  }

  if (tone === 'red') {
    return <XCircle aria-hidden="true" size={14} />
  }

  return <FileText aria-hidden="true" size={14} />
}

export function StatusBadge({ label, tone = 'slate' }: StatusBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium',
        toneClasses[tone],
      )}
    >
      <StatusIcon tone={tone} />
      {label}
    </span>
  )
}
