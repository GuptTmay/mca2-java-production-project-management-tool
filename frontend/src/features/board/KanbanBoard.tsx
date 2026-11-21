import { DndContext, type DragEndEvent, PointerSensor, useSensor, useSensors, useDraggable, useDroppable } from '@dnd-kit/core'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type KeyboardEvent } from 'react'
import { taskApi } from '../../api/endpoints'
import {
  TASK_PRIORITIES,
  TASK_STATUSES,
  TASK_STATUS_LABELS,
  type PageResponse,
  type Task,
  type TaskPriority,
  type TaskStatus,
  type TeamRole,
} from '../../lib/types'
import { Spinner, ErrorBanner, Badge, EmptyState } from '../../components/ui'
import { extractErrorMessage } from '../../api/client'
import { toast } from 'sonner'
import clsx from 'clsx'
import { CalendarClock, Search, X } from 'lucide-react'

const PRIORITY_COLORS: Record<TaskPriority, string> = {
  LOW: 'bg-surface-100 text-surface-600 border border-surface-200/60',
  MEDIUM: 'bg-blue-50 text-blue-700 border border-blue-100/60',
  HIGH: 'bg-amber-50 text-amber-700 border border-amber-100/60',
  URGENT: 'bg-rose-50 text-rose-700 border border-rose-100/60',
}

const STATUS_DOT_COLORS: Record<TaskStatus, string> = {
  TODO: 'bg-surface-400',
  IN_PROGRESS: 'bg-blue-500',
  IN_REVIEW: 'bg-amber-500',
  DONE: 'bg-emerald-500',
}

async function getAllBoardTasks(projectId: string): Promise<PageResponse<Task>> {
  const firstPage = await taskApi.board(projectId, 0, 200)
  const otherPages = await Promise.all(
    Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) =>
      taskApi.board(projectId, index + 1, 200),
    ),
  )

  return {
    ...firstPage,
    content: [firstPage, ...otherPages].flatMap((page) => page.content),
  }
}

function TaskCard({ task, onClick, canDrag }: { task: Task; onClick: () => void; canDrag: boolean }) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: task.id,
    disabled: !canDrag,
  })
  const style = transform
    ? { transform: `translate3d(${transform.x}px, ${transform.y}px, 0)`, zIndex: 10 }
    : undefined
  const dragProps = canDrag ? { ...listeners, ...attributes } : {}

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...dragProps}
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={(event: KeyboardEvent<HTMLDivElement>) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault()
          onClick()
        }
      }}
      aria-label={`Open task: ${task.title}`}
      className={clsx(
        'task-card w-full text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500',
        isDragging && 'task-card-dragging',
        !canDrag && 'cursor-default',
      )}
    >
      <p className="text-sm font-semibold text-surface-800 dark:text-surface-100 line-clamp-2">{task.title}</p>
      {task.description && (
        <p className="text-xs text-surface-500 dark:text-surface-400 line-clamp-2">{task.description}</p>
      )}
      <div className="flex items-center justify-between">
        <Badge className={PRIORITY_COLORS[task.priority]}>{task.priority}</Badge>
        {task.deadline && (
          <span className={clsx(
            'flex items-center gap-1 text-xs',
            new Date(task.deadline).getTime() < Date.now() && task.status !== 'DONE'
              ? 'font-semibold text-rose-600 dark:text-rose-400'
              : 'text-surface-400 dark:text-surface-500',
          )}>
            <CalendarClock className="h-3 w-3" />
            {new Date(task.deadline).toLocaleDateString()}
          </span>
        )}
      </div>
      {task.labels.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {task.labels.slice(0, 3).map((label) => (
            <span
              key={label.id}
              className="rounded-full px-2 py-0.5 text-[10px] font-semibold text-white shadow-sm"
              style={{ backgroundColor: label.color }}
            >
              {label.name}
            </span>
          ))}
          {task.labels.length > 3 && (
            <span className="rounded-full bg-surface-100 px-2 py-0.5 text-[10px] font-semibold text-surface-500 dark:bg-surface-700 dark:text-surface-300">
              +{task.labels.length - 3}
            </span>
          )}
        </div>
      )}
    </div>
  )
}

function Column({ status, tasks, canDrop, canDragTask, onTaskClickAction }: {
  status: TaskStatus;
  tasks: Task[];
  onTaskClickAction: (task: Task) => void;
  canDrop: boolean;
  canDragTask: (task: Task) => boolean;
}) {
  const { setNodeRef, isOver } = useDroppable({
    id: status,
    disabled: !canDrop,
  })
  return (
    <div
      ref={setNodeRef}
      className={clsx('kanban-column max-h-[70vh] overflow-y-auto scrollbar-thin', isOver && 'kanban-column-drag')}
    >
      <div className="sticky top-0 z-10 -mx-3 -mt-3 mb-3 flex items-center justify-between border-b border-surface-200/60 bg-white/95 px-3 pb-3 pt-3 backdrop-blur-sm dark:border-surface-700/60 dark:bg-surface-900/95">
        <h3 className="flex items-center gap-2 text-sm font-bold text-surface-800 dark:text-surface-100">
          <span className={clsx('h-2.5 w-2.5 rounded-full', STATUS_DOT_COLORS[status])} />
          {TASK_STATUS_LABELS[status]}
        </h3>
        <span
          aria-label={`${tasks.length} ${tasks.length === 1 ? 'task' : 'tasks'}`}
          className="rounded-full bg-surface-100 px-2.5 py-0.5 text-xs font-bold tabular-nums text-surface-600 dark:bg-surface-700 dark:text-surface-200"
        >
          {tasks.length}
        </span>
      </div>
      <div className="min-h-[100px] space-y-2.5">
        {tasks.map((task) => (
          <TaskCard
            key={task.id}
            task={task}
            onClick={() => onTaskClickAction(task)}
            canDrag={canDragTask(task)}
          />
        ))}
        {tasks.length === 0 && (
          <p className="rounded-xl border border-dashed border-surface-200/70 px-3 py-6 text-center text-xs text-surface-400 dark:border-surface-700/70">
            {canDrop ? 'Drop a task here' : 'No tasks'}
          </p>
        )}
      </div>
    </div>
  )
}

function getAllowedTargets(currentStatus: TaskStatus, role: TeamRole): TaskStatus[] {
  if (role === 'OWNER' || role === 'MANAGER') {
    switch (currentStatus) {
      case 'TODO':
        return ['IN_PROGRESS']
      case 'IN_PROGRESS':
        return ['TODO', 'IN_REVIEW']
      case 'IN_REVIEW':
        return ['IN_PROGRESS', 'DONE']
      case 'DONE':
        return ['IN_PROGRESS']
    }
  }
  switch (currentStatus) {
    case 'TODO':
      return ['IN_PROGRESS']
    case 'IN_PROGRESS':
      return ['TODO', 'IN_REVIEW']
    case 'IN_REVIEW':
    case 'DONE':
      return []
  }
  return []
}

export function KanbanBoard({ projectId, onTaskClick, canChangeStatus = true, userRole }: {
  projectId: string;
  onTaskClick: (task: Task) => void;
  canChangeStatus?: boolean;
  userRole?: TeamRole;
}) {
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [priorityFilter, setPriorityFilter] = useState<TaskPriority | 'ALL'>('ALL')
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }))

  const boardQuery = useQuery({
    queryKey: ['tasks', 'board', projectId],
    queryFn: () => getAllBoardTasks(projectId),
  })

  const updateStatus = useMutation({
    mutationFn: ({ taskId, status }: { taskId: string; status: TaskStatus }) => taskApi.updateStatus(taskId, status),
    onError: (err) => {
      toast.error(extractErrorMessage(err, 'That move is not allowed'))
      queryClient.invalidateQueries({ queryKey: ['tasks', 'board', projectId] })
    },
  })

  if (boardQuery.isLoading) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    )
  }

  if (boardQuery.isError || !boardQuery.data) {
    return <ErrorBanner message={extractErrorMessage(boardQuery.error, 'Could not load the board')} />
  }

  const tasks = boardQuery.data.content
  const normalizedSearch = search.trim().toLocaleLowerCase()
  const visibleTasks = tasks.filter((task) => {
    const matchesSearch = !normalizedSearch ||
      task.title.toLocaleLowerCase().includes(normalizedSearch) ||
      task.description?.toLocaleLowerCase().includes(normalizedSearch)
    return matchesSearch && (priorityFilter === 'ALL' || task.priority === priorityFilter)
  })
  const tasksByStatus = (status: TaskStatus) => visibleTasks.filter((task) => task.status === status)
  const hasFilters = normalizedSearch.length > 0 || priorityFilter !== 'ALL'

  function clearFilters() {
    setSearch('')
    setPriorityFilter('ALL')
  }

  function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event
    if (!over) return
    const taskId = active.id as string
    const newStatus = over.id as TaskStatus
    const task = tasks.find((t) => t.id === taskId)
    if (!task || task.status === newStatus) return

    if (!userRole) return
    const allowedTargets = getAllowedTargets(task.status, userRole)
    if (!allowedTargets.includes(newStatus)) {
      toast.error('That status change is not allowed for your role')
      return
    }

    queryClient.setQueryData(['tasks', 'board', projectId], (old: typeof boardQuery.data) =>
      old
        ? { ...old, content: old.content.map((t) => (t.id === taskId ? { ...t, status: newStatus } : t)) }
        : old,
    )
    updateStatus.mutate({ taskId, status: newStatus })
  }

  const role = userRole || 'MEMBER'
  const draggable = canChangeStatus
  const canDropOn = (status: TaskStatus) => visibleTasks.some((task) =>
    getAllowedTargets(task.status, role).includes(status),
  )

  const board = (
    <>
      <div className="mb-3 flex flex-col gap-3 rounded-2xl border border-surface-200/60 bg-white/50 p-3 dark:border-surface-700/60 dark:bg-surface-900/30 sm:flex-row sm:items-center">
        <label className="relative min-w-0 flex-1">
          <span className="sr-only">Search board tasks</span>
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-surface-400" />
          <input
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search tasks..."
            className="glass-input w-full pl-9 pr-9 text-sm"
          />
          {search && (
            <button
              type="button"
              onClick={() => setSearch('')}
              aria-label="Clear task search"
              className="absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-1 text-surface-400 hover:text-surface-700 dark:hover:text-surface-200"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </label>
        <label className="flex shrink-0 items-center gap-2">
          <span className="text-xs font-semibold text-surface-500">Priority</span>
          <select
            aria-label="Filter tasks by priority"
            value={priorityFilter}
            onChange={(event) => setPriorityFilter(event.target.value as TaskPriority | 'ALL')}
            className="glass-input py-2 text-sm"
          >
            <option value="ALL">All priorities</option>
            {TASK_PRIORITIES.map((priority) => (
              <option key={priority} value={priority}>{priority}</option>
            ))}
          </select>
        </label>
        <span className="shrink-0 text-xs tabular-nums text-surface-500" aria-live="polite">
          {visibleTasks.length} of {tasks.length} tasks
        </span>
        {hasFilters && (
          <button
            type="button"
            onClick={clearFilters}
            className="shrink-0 text-xs font-semibold text-brand-600 hover:text-brand-700 dark:text-brand-300"
          >
            Clear filters
          </button>
        )}
      </div>

      {visibleTasks.length === 0 ? (
        <EmptyState
          title={tasks.length === 0 ? 'No tasks yet' : 'No matching tasks'}
          description={tasks.length === 0
            ? 'Tasks created for this project will appear here.'
            : 'Try another search or priority filter.'}
        />
      ) : (
        <div
          className="w-full min-w-0 max-w-full overflow-x-auto px-1 pt-2 pb-5 scrollbar-thin"
          aria-label="Project task board"
        >
          <div className="flex w-max gap-4">
            {TASK_STATUSES.map((status) => (
              <Column
                key={status}
                status={status}
                tasks={tasksByStatus(status)}
                canDrop={draggable && canDropOn(status)}
                canDragTask={(task) => draggable && getAllowedTargets(task.status, role).length > 0}
                onTaskClickAction={onTaskClick}
              />
            ))}
          </div>
        </div>
      )}
    </>
  )

  return draggable
    ? <DndContext sensors={sensors} onDragEnd={handleDragEnd}>{board}</DndContext>
    : board
}
