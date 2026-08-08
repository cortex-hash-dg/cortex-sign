import { cn } from '../lib/cn'

type InlineMessageProps = {
  message: string
  tone?: 'error' | 'info'
}

export function InlineMessage({ message, tone = 'info' }: InlineMessageProps) {
  return (
    <p
      className={cn(
        'rounded-control border px-3 py-2 text-sm',
        tone === 'error'
          ? 'border-danger/20 bg-red-50 text-danger'
          : 'border-line bg-surface-page text-muted',
      )}
      role={tone === 'error' ? 'alert' : undefined}
    >
      {message}
    </p>
  )
}
