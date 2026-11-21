import { useQuery } from '@tanstack/react-query'
import { activityApi } from '../../api/endpoints'
import { Spinner, ErrorBanner, EmptyState } from '../../components/ui'
import { extractErrorMessage } from '../../api/client'
import { Clock } from 'lucide-react'
import type { Activity } from '../../lib/types'

interface ActivityTimelineProps {
  taskId?: string
  projectId?: string
}

const ACTIVITY_TYPE_LABELS: Record<string, string> = {
  TASK_CREATED: 'Created task',
  TASK_ASSIGNED: 'Assigned task',
  TASK_STATUS_CHANGED: 'Changed status',
  TASK_PRIORITY_CHANGED: 'Changed priority',
  LABEL_ADDED: 'Added label',
  LABEL_REMOVED: 'Removed label',
  COMMENT_ADDED: 'Added comment',
  TASK_UPDATED: 'Updated task',
}

export function ActivityTimeline({ taskId, projectId }: ActivityTimelineProps) {
  const queryKey = taskId ? ['activities', 'task', taskId] : ['activities', 'project', projectId]
  const queryFn = taskId ? () => activityApi.listForTask(taskId) : () => activityApi.listForProject(projectId!)

  const { data, isLoading, isError, error } = useQuery({
    queryKey,
    queryFn,
    enabled: !!taskId || !!projectId,
  })

  if (isLoading) {
    return <Spinner />
  }

  if (isError || !data) {
    return <ErrorBanner message={extractErrorMessage(error, 'Could not load activity')} />
  }

  if (data.length === 0) {
    return (
      <EmptyState
        title="No activity yet"
        description="Actions on this task will be recorded here."
        icon={<Clock className="h-8 w-8" />}
      />
    )
  }

  return (
    <div className="space-y-3">
      <h3 className="text-sm font-bold text-surface-700 flex items-center gap-2">
        <Clock className="h-4 w-4" />
        Activity
      </h3>
      <div className="relative pl-6">
        <div className="absolute left-2 top-2 bottom-2 w-px bg-surface-200/60" />
        <div className="space-y-4">
          {data.map((activity: Activity) => (
            <div key={activity.id} className="relative">
              <div className="absolute -left-4 top-1 h-2.5 w-2.5 rounded-full bg-brand-400 ring-4 ring-white" />
              <div className="rounded-xl border border-surface-200/60 bg-white/60 backdrop-blur-sm p-3">
                <p className="text-sm text-surface-800">
                  <span className="font-semibold">
                    {activity.actorFirstName} {activity.actorLastName}
                  </span>{' '}
                  {ACTIVITY_TYPE_LABELS[activity.type] || activity.type.toLowerCase().replace(/_/g, ' ')}
                </p>
                <p className="text-xs text-surface-500 mt-1">{activity.description}</p>
                <p className="text-xs text-surface-400 mt-1">
                  {new Date(activity.createdAt).toLocaleString()}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
