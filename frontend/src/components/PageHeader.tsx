import clsx from 'clsx'
import { AlertTriangle } from 'lucide-react'

interface PageHeaderProps {
  title: string
  description?: string
  action?: React.ReactNode
  className?: string
}

export function PageHeader({ title, description, action, className }: PageHeaderProps) {
  return (
    <div className={clsx('flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between', className)}>
      <div className="min-w-0 flex-1">
        <h1 className="text-2xl font-bold text-surface-900 tracking-tight dark:text-surface-100">{title}</h1>
        {description && <p className="mt-1 text-sm text-surface-500 dark:text-surface-400">{description}</p>}
      </div>
      {action && <div className="w-full flex-shrink-0 sm:w-auto">{action}</div>}
    </div>
  )
}

interface SectionHeaderProps {
  title: string
  description?: string
  action?: React.ReactNode
  className?: string
}

export function SectionHeader({ title, description, action, className }: SectionHeaderProps) {
  return (
    <div className={clsx('flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between', className)}>
      <div className="min-w-0 flex-1">
        <h2 className="text-lg font-semibold text-surface-900 dark:text-surface-100">{title}</h2>
        {description && <p className="mt-0.5 text-sm text-surface-500 dark:text-surface-400">{description}</p>}
      </div>
      {action && <div className="w-full flex-shrink-0 sm:w-auto">{action}</div>}
    </div>
  )
}

interface EmptyStateProps {
  icon?: React.ReactNode
  title: string
  description?: string
  action?: React.ReactNode
  className?: string
}

export function EmptyState({ icon, title, description, action, className }: EmptyStateProps) {
  return (
    <div
      className={clsx(
        'flex flex-col items-center justify-center rounded-2xl border border-dashed border-surface-300/60 bg-white/40 backdrop-blur-sm py-16 text-center px-4',
        className,
      )}
    >
      {icon && <div className="mb-4 text-surface-400">{icon}</div>}
      <p className="text-sm font-semibold text-surface-600">{title}</p>
      {description && <p className="mt-2 text-sm text-surface-400 max-w-sm">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

interface ErrorStateProps {
  message: string
  onRetry?: () => void
  className?: string
}

export function ErrorState({ message, onRetry, className }: ErrorStateProps) {
  return (
    <div className={clsx('flex flex-col items-center justify-center py-16 text-center px-4', className)}>
      <div className="mb-4 rounded-full bg-rose-50 p-3 text-rose-600">
        <AlertTriangle className="h-6 w-6" />
      </div>
      <p className="text-sm font-semibold text-surface-900">Something went wrong</p>
      <p className="mt-1 text-sm text-surface-500 max-w-sm">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="mt-4 inline-flex items-center gap-2 rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700 transition-colors"
        >
          Try again
        </button>
      )}
    </div>
  )
}
