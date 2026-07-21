import { type ButtonHTMLAttributes } from 'react'
import { cn } from '../lib/cn'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'danger'
}

const variants = {
  primary:
    'border-brand-500 bg-brand-500 text-white hover:border-brand-600 hover:bg-brand-600',
  secondary:
    'border-line bg-white text-ink hover:border-brand-500 hover:text-brand-500',
  danger:
    'border-danger bg-danger text-white hover:bg-red-700 hover:border-red-700',
}

export function Button({
  className,
  type = 'button',
  variant = 'primary',
  ...props
}: ButtonProps) {
  return (
    <button
      className={cn(
        'inline-flex min-h-11 items-center justify-center gap-2 rounded-control border px-4 text-sm font-medium transition-colors duration-150 disabled:pointer-events-none disabled:opacity-50',
        variants[variant],
        className,
      )}
      type={type}
      {...props}
    />
  )
}
