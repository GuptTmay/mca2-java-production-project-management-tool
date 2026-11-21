import clsx from 'clsx'
import { CheckCircle2, Circle, Clock, AlertTriangle, Archive, Pause } from 'lucide-react'
import { PROJECT_STATUS_LABELS, type TaskStatus, type ProjectStatus } from '../lib/types'

const TASK_STATUS_CONFIG: Record<TaskStatus, { label: string; className: string; icon: any }> = {
  TODO: {
    label: 'To Do',
    className: 'bg-surface-100 text-surface-600 border-surface-200/60 dark:bg-surface-800 dark:text-surface-300 dark:border-surface-700',
    icon: Circle,
  },
  IN_PROGRESS: {
    label: 'In Progress',
    className: 'bg-blue-50 text-blue-700 border-blue-100/60 dark:bg-blue-900/30 dark:text-blue-300 dark:border-blue-800/50',
    icon: Clock,
  },
  IN_REVIEW: {
    label: 'In Review',
    className: 'bg-amber-50 text-amber-700 border-amber-100/60 dark:bg-amber-900/30 dark:text-amber-300 dark:border-amber-800/50',
    icon: AlertTriangle,
  },
  DONE: {
    label: 'Done',
    className: 'bg-emerald-50 text-emerald-700 border-emerald-100/60 dark:bg-emerald-900/30 dark:text-emerald-300 dark:border-emerald-800/50',
    icon: CheckCircle2,
  },
}

const PROJECT_STATUS_CONFIG: Record<ProjectStatus, { label: string; className: string; icon: any }> = {
  PLANNING: {
    label: PROJECT_STATUS_LABELS.PLANNING,
    className: 'bg-surface-100 text-surface-600 border-surface-200/60 dark:bg-surface-800 dark:text-surface-300 dark:border-surface-700',
    icon: Circle,
  },
  ACTIVE: {
    label: PROJECT_STATUS_LABELS.ACTIVE,
    className: 'bg-blue-50 text-blue-700 border-blue-100/60 dark:bg-blue-900/30 dark:text-blue-300 dark:border-blue-800/50',
    icon: Clock,
  },
  ON_HOLD: {
    label: PROJECT_STATUS_LABELS.ON_HOLD,
    className: 'bg-amber-50 text-amber-700 border-amber-100/60 dark:bg-amber-900/30 dark:text-amber-300 dark:border-amber-800/50',
    icon: Pause,
  },
  COMPLETED: {
    label: PROJECT_STATUS_LABELS.COMPLETED,
    className: 'bg-emerald-50 text-emerald-700 border-emerald-100/60 dark:bg-emerald-900/30 dark:text-emerald-300 dark:border-emerald-800/50',
    icon: CheckCircle2,
  },
  ARCHIVED: {
    label: PROJECT_STATUS_LABELS.ARCHIVED,
    className: 'bg-surface-100 text-surface-500 border-surface-200/60 dark:bg-surface-800 dark:text-surface-400 dark:border-surface-700',
    icon: Archive,
  },
}

export function StatusBadge({ status, className }: { status: TaskStatus | ProjectStatus; className?: string }) {
  const taskConfig = TASK_STATUS_CONFIG[status as TaskStatus]
  const projectConfig = PROJECT_STATUS_CONFIG[status as ProjectStatus]
  const config = taskConfig || projectConfig || { label: status, className: 'bg-surface-100 text-surface-600 border-surface-200/60', icon: Circle }
  const Icon = config.icon

  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold border',
        config.className,
        className,
      )}
    >
      <Icon className="h-3 w-3" />
      {config.label}
    </span>
  )
}
