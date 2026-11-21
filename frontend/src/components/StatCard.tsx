import { LucideIcon, TrendingUp, TrendingDown } from 'lucide-react'
import clsx from 'clsx'

interface StatCardProps {
  title: string
  value: string | number
  icon: LucideIcon
  trend?: { value: number; isPositive: boolean }
  description?: string
  className?: string
}

export function StatCard({ title, value, icon: Icon, trend, description, className }: StatCardProps) {
  return (
    <div className={clsx('stat-card', className)}>
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-surface-500">{title}</p>
          <p className="mt-2 text-3xl font-bold text-surface-900 tracking-tight">{value}</p>
          {description && <p className="mt-1 text-xs text-surface-400">{description}</p>}
          {trend && (
            <div className="mt-2 flex items-center gap-1 text-xs font-medium">
              {trend.isPositive ? (
                <TrendingUp className="h-3.5 w-3.5 text-emerald-600" />
              ) : (
                <TrendingDown className="h-3.5 w-3.5 text-rose-600" />
              )}
              <span className={clsx(trend.isPositive ? 'text-emerald-600' : 'text-rose-600')}>
                {trend.value}%
              </span>
              <span className="text-surface-400">from last week</span>
            </div>
          )}
        </div>
        <div className="stat-card-icon">
          <Icon className="h-5 w-5" />
        </div>
      </div>
    </div>
  )
}
