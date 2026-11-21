import clsx from 'clsx'
import type { ButtonHTMLAttributes, ForwardedRef, InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from 'react'
import { forwardRef } from 'react'
import { Loader2 } from 'lucide-react'

export function Button({
  className,
  variant = 'primary',
  loading = false,
  children,
  disabled,
  size = 'md',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
  loading?: boolean
  size?: 'sm' | 'md' | 'lg'
}) {
  const sizes: Record<string, string> = {
    sm: 'px-3 py-2 text-xs gap-1.5',
    md: 'px-4 py-2.5 text-sm gap-2',
    lg: 'px-6 py-3 text-base gap-2.5',
  }

  const base = clsx(
    'inline-flex items-center justify-center rounded-xl font-semibold transition-all duration-200',
    'disabled:opacity-50 disabled:cursor-not-allowed',
    'active:scale-[0.98]',
    sizes[size],
  )

  const variants: Record<string, string> = {
    primary: clsx(
      'bg-gradient-to-r from-brand-600 to-indigo-600 text-white',
      'hover:from-brand-700 hover:to-indigo-700',
      'shadow-lg shadow-brand-600/20 hover:shadow-xl hover:shadow-brand-600/30',
    ),
    secondary: clsx(
      'glass border border-surface-200/60 text-surface-700',
      'hover:border-brand-200 hover:text-brand-700 hover:shadow-glass-sm',
    ),
    danger: clsx(
      'bg-gradient-to-r from-rose-600 to-red-600 text-white',
      'hover:from-rose-700 hover:to-red-700',
      'shadow-lg shadow-rose-600/20 hover:shadow-xl hover:shadow-rose-600/30',
    ),
    ghost: clsx(
      'text-surface-500 hover:bg-surface-100/60 hover:text-brand-700',
    ),
  }

  return (
    <button className={clsx(base, variants[variant], className)} disabled={disabled || loading} {...props}>
      {loading && <Loader2 className="h-4 w-4 animate-spin" />}
      {children}
    </button>
  )
}

export const Input = forwardRef(function Input(
  { className, ...props }: InputHTMLAttributes<HTMLInputElement>,
  ref: ForwardedRef<HTMLInputElement>,
) {
  return (
    <input
      ref={ref}
      className={clsx('glass-input', className)}
      {...props}
    />
  )
})

export const Textarea = forwardRef(function Textarea(
  { className, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>,
  ref: ForwardedRef<HTMLTextAreaElement>,
) {
  return (
    <textarea
      ref={ref}
      className={clsx('glass-input resize-none', className)}
      {...props}
    />
  )
})

export function Field({ label, children, error, hint }: { label: string; children: ReactNode; error?: string; hint?: string }) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-semibold text-surface-700">{label}</span>
      {children}
      {hint && <span className="mt-1.5 block text-xs text-surface-400">{hint}</span>}
      {error && <span className="mt-1.5 block text-xs text-rose-600 font-medium">{error}</span>}
    </label>
  )
}

export function Card({ className, children, glass = true }: { className?: string; children: ReactNode; glass?: boolean }) {
  return (
    <div className={clsx(
      glass ? 'glass-card' : 'rounded-2xl border border-surface-200 bg-white shadow-sm',
      className
    )}>
      {children}
    </div>
  )
}

export function Spinner({ className }: { className?: string }) {
  return (
    <div className="flex justify-center py-16">
      <div className={clsx('h-8 w-8 rounded-full border-2 border-surface-200 border-t-brand-600 animate-spin', className)} />
    </div>
  )
}

export function EmptyState({ title, description, icon: Icon }: { title: string; description?: string; icon?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-surface-300/60 bg-white/40 backdrop-blur-sm py-16 text-center px-4">
      {Icon && <div className="mb-4 text-surface-400">{Icon}</div>}
      <p className="text-sm font-semibold text-surface-600">{title}</p>
      {description && <p className="mt-2 text-sm text-surface-400 max-w-sm">{description}</p>}
    </div>
  )
}

export function ErrorBanner({ message, onDismiss }: { message: string; onDismiss?: () => void }) {
  return (
    <div className="relative rounded-xl bg-rose-50/80 backdrop-blur-sm px-4 py-3 text-sm text-rose-700 border border-rose-100/60 font-medium flex items-start justify-between gap-2">
      <span>{message}</span>
      {onDismiss && (
        <button onClick={onDismiss} className="text-rose-400 hover:text-rose-600 transition-colors flex-shrink-0">
          &times;
        </button>
      )}
    </div>
  )
}

export function Badge({ children, className, variant = 'default' }: { children: ReactNode; className?: string; variant?: 'default' | 'success' | 'warning' | 'danger' | 'info' }) {
  const variants: Record<string, string> = {
    default: 'bg-surface-100 text-surface-600 border border-surface-200/60',
    success: 'bg-emerald-50 text-emerald-700 border border-emerald-100/60',
    warning: 'bg-amber-50 text-amber-700 border border-amber-100/60',
    danger: 'bg-rose-50 text-rose-700 border border-rose-100/60',
    info: 'bg-brand-50 text-brand-700 border border-brand-100/60',
  }

  return (
    <span className={clsx('inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold', variants[variant], className)}>
      {children}
    </span>
  )
}

export function Skeleton({ className }: { className?: string }) {
  return <div className={clsx('skeleton', className)} />
}
