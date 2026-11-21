import clsx from 'clsx'
import type { TaskPriority } from '../lib/types'

const PRIORITY_CONFIG: Record<TaskPriority, { label: string; className: string }> = {
  LOW: { label: 'Low', className: 'bg-surface-100 text-surface-600 border-surface-200/60' },
  MEDIUM: { label: 'Medium', className: 'bg-blue-50 text-blue-700 border-blue-100/60' },
  HIGH: { label: 'High', className: 'bg-amber-50 text-amber-700 border-amber-100/60' },
  URGENT: { label: 'Urgent', className: 'bg-rose-50 text-rose-700 border-rose-100/60' },
}

export function PriorityBadge({ priority, className }: { priority: TaskPriority; className?: string }) {
  const config = PRIORITY_CONFIG[priority] || PRIORITY_CONFIG.MEDIUM

  return (
    <span
      className={clsx(
        'inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold border',
        config.className,
        className,
      )}
    >
      {config.label}
    </span>
  )
}
