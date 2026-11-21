import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '../../api/endpoints'
import { Card, Spinner, ErrorBanner } from '../../components/ui'
import { PageHeader, EmptyState } from '../../components/PageHeader'
import { StatusBadge } from '../../components/StatusBadge'
import { PriorityBadge } from '../../components/PriorityBadge'
import { Link } from 'react-router-dom'
import { CheckSquare, Clock, Filter } from 'lucide-react'
import { useState } from 'react'
import { TASK_STATUSES, type TaskStatus, type TaskPriority } from '../../lib/types'

const STATUS_FILTERS: { label: string; value: TaskStatus | 'all' }[] = [
  { label: 'All', value: 'all' },
  ...TASK_STATUSES.map(status => ({ label: status.replace('_', ' '), value: status })),
]

const PRIORITY_FILTERS: { label: string; value: TaskPriority | 'all' }[] = [
  { label: 'All Priorities', value: 'all' },
  { label: 'Urgent', value: 'URGENT' },
  { label: 'High', value: 'HIGH' },
  { label: 'Medium', value: 'MEDIUM' },
  { label: 'Low', value: 'LOW' },
]

export function MyTasksPage() {
  const [statusFilter, setStatusFilter] = useState<TaskStatus | 'all'>('all')
  const [priorityFilter, setPriorityFilter] = useState<TaskPriority | 'all'>('all')

  const { data, isLoading, isError } = useQuery({
    queryKey: ['dashboard', 'my-tasks', statusFilter, priorityFilter],
    queryFn: () => dashboardApi.myTasks(0, 100),
  })

  const tasks = data?.tasks.content || []

  const filteredTasks = tasks.filter((task) => {
    if (statusFilter !== 'all' && task.status !== statusFilter) return false
    if (priorityFilter !== 'all' && task.priority !== priorityFilter) return false
    return true
  })

  if (isLoading) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    )
  }

  if (isError || !data) {
    return <ErrorBanner message="Could not load your tasks" />
  }

  const totalTasks = data.totalAssignedTasks
  const todoTasks = data.tasksByStatus['TODO'] ?? 0
  const inProgressTasks = data.tasksByStatus['IN_PROGRESS'] ?? 0
  const doneTasks = data.tasksByStatus['DONE'] ?? 0

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title="My Tasks"
        description="All tasks assigned to you across all projects"
      />

      {/* Stats */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Card className="stat-card">
          <div className="flex items-center gap-3">
            <div className="stat-card-icon">
              <CheckSquare className="h-5 w-5" />
            </div>
            <div>
              <p className="text-xs font-semibold text-surface-400 uppercase tracking-wider">Total</p>
              <p className="text-2xl font-bold text-surface-900">{totalTasks}</p>
            </div>
          </div>
        </Card>
        <Card className="stat-card">
          <div className="flex items-center gap-3">
            <div className="stat-card-icon bg-gradient-to-br from-blue-50 to-cyan-50 text-blue-600">
              <Clock className="h-5 w-5" />
            </div>
            <div>
              <p className="text-xs font-semibold text-surface-400 uppercase tracking-wider">To Do</p>
              <p className="text-2xl font-bold text-blue-600">{todoTasks}</p>
            </div>
          </div>
        </Card>
        <Card className="stat-card">
          <div className="flex items-center gap-3">
            <div className="stat-card-icon bg-gradient-to-br from-amber-50 to-orange-50 text-amber-600">
              <Clock className="h-5 w-5" />
            </div>
            <div>
              <p className="text-xs font-semibold text-surface-400 uppercase tracking-wider">In Progress</p>
              <p className="text-2xl font-bold text-amber-600">{inProgressTasks}</p>
            </div>
          </div>
        </Card>
        <Card className="stat-card">
          <div className="flex items-center gap-3">
            <div className="stat-card-icon bg-gradient-to-br from-emerald-50 to-teal-50 text-emerald-600">
              <CheckSquare className="h-5 w-5" />
            </div>
            <div>
              <p className="text-xs font-semibold text-surface-400 uppercase tracking-wider">Done</p>
              <p className="text-2xl font-bold text-emerald-600">{doneTasks}</p>
            </div>
          </div>
        </Card>
      </div>

      {/* Filters */}
      <Card className="p-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 text-sm font-medium text-surface-600">
            <Filter className="h-4 w-4" />
            Filters:
          </div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as TaskStatus | 'all')}
            className="glass-input py-2 text-sm w-40"
          >
            {STATUS_FILTERS.map(filter => (
              <option key={filter.value} value={filter.value}>{filter.label}</option>
            ))}
          </select>
          <select
            value={priorityFilter}
            onChange={(e) => setPriorityFilter(e.target.value as TaskPriority | 'all')}
            className="glass-input py-2 text-sm w-40"
          >
            {PRIORITY_FILTERS.map(filter => (
              <option key={filter.value} value={filter.value}>{filter.label}</option>
            ))}
          </select>
        </div>
      </Card>

      {/* Task List */}
      {filteredTasks.length === 0 ? (
        <EmptyState title="No tasks found" description="You don't have any tasks matching the current filters." />
      ) : (
        <div className="space-y-2">
          {filteredTasks.map((task) => (
            <Link
              key={task.id}
              to={`/projects/${task.projectId}`}
              className="flex items-center justify-between rounded-xl border border-surface-200/60 bg-white/60 backdrop-blur-sm px-5 py-4 hover:border-brand-200 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 group"
            >
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-3 mb-1">
                  <p className="font-semibold text-surface-800 group-hover:text-brand-700 transition-colors truncate">
                    {task.title}
                  </p>
                  <StatusBadge status={task.status} />
                </div>
                {task.description && (
                  <p className="text-sm text-surface-500 truncate">{task.description}</p>
                )}
              </div>
              <div className="flex items-center gap-3 ml-4">
                <PriorityBadge priority={task.priority} />
                {task.deadline && (
                  <span className="text-xs text-surface-400 whitespace-nowrap">
                    {new Date(task.deadline).toLocaleDateString()}
                  </span>
                )}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
