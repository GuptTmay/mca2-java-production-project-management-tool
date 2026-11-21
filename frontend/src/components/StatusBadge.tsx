import clsx from 'clsx'
import { CheckCircle2, Circle, Clock, AlertTriangle, Archive, Pause } from 'lucide-react'
import type { TaskStatus, ProjectStatus } from '../lib/types'

const TASK_STATUS_CONFIG: Record<TaskStatus, { label: string; className: string; icon: any }> = {
  TODO: {
    label: 'To Do',
    className: 'bg-surface-100 text-surface-600 border-surface-200/60',
    icon: Circle,
  },
  IN_PROGRESS: {
    label: 'In Progress',
    className: 'bg-blue-50 text-blue-700 border-blue-100/60',
    icon: Clock,
  },
  IN_REVIEW: {
    label: 'In Review',
    className: 'bg-amber-50 text-amber-700 border-amber-100/60',
    icon: AlertTriangle,
  },
  DONE: {
    label: 'Done',
    className: 'bg-emerald-50 text-emerald-700 border-emerald-100/60',
    icon: CheckCircle2,
  },
}

const PROJECT_STATUS_CONFIG: Record<ProjectStatus, { label: string; className: string; icon: any }> = {
  PLANNING: {
    label: 'Planning',
    className: 'bg-surface-100 text-surface-600 border-surface-200/60',
    icon: Circle,
  },
  ACTIVE: {
    label: 'Active',
    className: 'bg-blue-50 text-blue-700 border-blue-100/60',
    icon: Clock,
  },
  ON_HOLD: {
    label: 'On Hold',
    className: 'bg-amber-50 text-amber-700 border-amber-100/60',
    icon: Pause,
  },
  COMPLETED: {
    label: 'Completed',
    className: 'bg-emerald-50 text-emerald-700 border-emerald-100/60',
    icon: CheckCircle2,
  },
  ARCHIVED: {
    label: 'Archived',
    className: 'bg-surface-100 text-surface-500 border-surface-200/60',
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
