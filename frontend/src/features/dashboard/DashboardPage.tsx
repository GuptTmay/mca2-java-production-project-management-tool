import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '../../api/endpoints'
import { Card, Spinner, ErrorBanner } from '../../components/ui'
import { StatCard } from '../../components/StatCard'
import { PageHeader, EmptyState } from '../../components/PageHeader'
import { StatusBadge } from '../../components/StatusBadge'
import { PriorityBadge } from '../../components/PriorityBadge'
import { extractErrorMessage } from '../../api/client'
import { Link } from 'react-router-dom'
import { LayoutDashboard, Clock, AlertTriangle, CheckCircle2, ArrowRight, Activity } from 'lucide-react'
import { useState } from 'react'
import clsx from 'clsx'

export function DashboardPage() {
  const [timeRange, setTimeRange] = useState<'week' | 'month'>('week')
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['dashboard', 'my-tasks'],
    queryFn: () => dashboardApi.myTasks(0, 10),
  })

  if (isLoading) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    )
  }

  if (isError || !data) {
    return <ErrorBanner message={extractErrorMessage(error, 'Could not load your dashboard')} />
  }

  const totalTasks = data.totalAssignedTasks
  const todoTasks = data.tasksByStatus['TODO'] ?? 0
  const inProgressTasks = data.tasksByStatus['IN_PROGRESS'] ?? 0
  const inReviewTasks = data.tasksByStatus['IN_REVIEW'] ?? 0
  const doneTasks = data.tasksByStatus['DONE'] ?? 0
  const overdueTasks = data.overdueTasks

  const completionRate = totalTasks > 0 ? Math.round((doneTasks / totalTasks) * 100) : 0

  return (
    <div className="space-y-8 animate-fade-in">
      <PageHeader
        title={`Good morning`}
        description="Here's what's happening with your tasks today."
        action={
          <div className="flex items-center gap-2">
            <select
              value={timeRange}
              onChange={(e) => setTimeRange(e.target.value as 'week' | 'month')}
              className="glass-input py-2 text-sm w-32"
            >
              <option value="week">This week</option>
              <option value="month">This month</option>
            </select>
          </div>
        }
      />

      {/* Stats Grid */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard title="Total Tasks" value={totalTasks} icon={LayoutDashboard} />
        <StatCard title="In Progress" value={inProgressTasks} icon={Clock} />
        <StatCard title="In Review" value={inReviewTasks} icon={Activity} />
        <StatCard title="Completed" value={doneTasks} icon={CheckCircle2} trend={{ value: completionRate, isPositive: true }} />
      </div>

      {/* Task Progress */}
      <Card className="p-6">
        <h3 className="text-lg font-semibold text-surface-900 mb-4">Task Progress</h3>
        <div className="space-y-4">
          {[
            { label: 'To Do', count: todoTasks, color: 'bg-surface-400' },
            { label: 'In Progress', count: inProgressTasks, color: 'bg-blue-500' },
            { label: 'In Review', count: inReviewTasks, color: 'bg-amber-500' },
            { label: 'Done', count: doneTasks, color: 'bg-emerald-500' },
          ].map((item) => (
            <div key={item.label} className="flex items-center gap-4">
              <div className="w-24 text-sm font-medium text-surface-600">{item.label}</div>
              <div className="flex-1 h-2.5 rounded-full bg-surface-100 overflow-hidden">
                <div
                  className={clsx('h-full rounded-full transition-all duration-500', item.color)}
                  style={{ width: `${totalTasks > 0 ? (item.count / totalTasks) * 100 : 0}%` }}
                />
              </div>
              <div className="w-12 text-right text-sm font-semibold text-surface-900">{item.count}</div>
            </div>
          ))}
        </div>
      </Card>

      {/* Overdue Tasks */}
      {overdueTasks > 0 && (
        <Card className="p-6 border-rose-200/60 bg-rose-50/30">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-rose-100 text-rose-600">
              <AlertTriangle className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-sm font-semibold text-rose-900">Overdue Tasks</h3>
              <p className="text-sm text-rose-700">You have {overdueTasks} overdue task{overdueTasks !== 1 ? 's' : ''} that need attention.</p>
            </div>
          </div>
        </Card>
      )}

      {/* Recent Tasks */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-surface-900">Your Tasks</h3>
          <Link to="/my-tasks" className="text-sm font-medium text-brand-600 hover:text-brand-700 flex items-center gap-1">
            View all <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
        {data.tasks.content?.length === 0 ? (
          <EmptyState title="No tasks assigned" description="Tasks assigned to you will appear here." />
        ) : (
          <div className="space-y-2">
            {data.tasks.content?.slice(0, 10).map((task) => (
              <Link
                key={task.id}
                to={`/projects/${task.projectId}`}
                className="flex items-center justify-between rounded-xl border border-surface-200/60 bg-white/60 backdrop-blur-sm px-5 py-4 hover:border-brand-200 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 group"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-3">
                    <p className="font-semibold text-surface-800 group-hover:text-brand-700 transition-colors truncate">
                      {task.title}
                    </p>
                    <StatusBadge status={task.status} />
                  </div>
                  {task.description && (
                    <p className="mt-1 text-sm text-surface-500 truncate">{task.description}</p>
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
    </div>
  )
}
