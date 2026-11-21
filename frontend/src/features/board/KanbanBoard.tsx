import { DndContext, type DragEndEvent, PointerSensor, useSensor, useSensors, useDraggable, useDroppable } from '@dnd-kit/core'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { taskApi } from '../../api/endpoints'
import { TASK_STATUSES, TASK_STATUS_LABELS, type Task, type TaskStatus, type TeamRole } from '../../lib/types'
import { Spinner, ErrorBanner, Badge } from '../../components/ui'
import { extractErrorMessage } from '../../api/client'
import { toast } from 'sonner'
import clsx from 'clsx'
import { CalendarClock } from 'lucide-react'

const PRIORITY_COLORS: Record<string, string> = {
  LOW: 'bg-surface-100 text-surface-600 border border-surface-200/60',
  MEDIUM: 'bg-blue-50 text-blue-700 border border-blue-100/60',
  HIGH: 'bg-amber-50 text-amber-700 border border-amber-100/60',
  URGENT: 'bg-rose-50 text-rose-700 border border-rose-100/60',
}

function TaskCard({ task, onClick, canDrag }: { task: Task; onClick: () => void; canDrag: boolean }) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: task.id,
    disabled: !canDrag,
  })
  const style = transform
    ? { transform: `translate3d(${transform.x}px, ${transform.y}px, 0)`, zIndex: 10 }
    : undefined

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...(canDrag ? { ...listeners, ...attributes } : {})}
      onClick={onClick}
      className={clsx(
        'task-card',
        isDragging && 'task-card-dragging',
        !canDrag && 'opacity-80 cursor-default',
      )}
    >
      <p className="text-sm font-semibold text-surface-800 dark:text-surface-100 line-clamp-2">{task.title}</p>
      {task.description && (
        <p className="text-xs text-surface-500 dark:text-surface-400 line-clamp-2">{task.description}</p>
      )}
      <div className="flex items-center justify-between">
        <Badge className={PRIORITY_COLORS[task.priority]}>{task.priority}</Badge>
        {task.deadline && (
          <span className="flex items-center gap-1 text-xs text-surface-400 dark:text-surface-500">
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
        </div>
      )}
    </div>
  )
}

function Column({ status, tasks, canDrop, onTaskClickAction }: {
  status: TaskStatus;
  tasks: Task[];
  onTaskClickAction: (task: Task) => void;
  canDrop: boolean;
}) {
  const { setNodeRef, isOver } = useDroppable({
    id: status,
    disabled: !canDrop,
  })
  return (
    <div
      ref={setNodeRef}
      className={clsx(
        'kanban-column',
        isOver && 'kanban-column-drag',
        !canDrop && 'opacity-60',
      )}
    >
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-bold text-surface-700">{TASK_STATUS_LABELS[status]}</h3>
        <span className="rounded-full bg-brand-100 px-2.5 py-0.5 text-xs font-bold text-brand-700">{tasks.length}</span>
      </div>
      <div className="min-h-[100px] space-y-2.5">
        {tasks.map((task) => (
          <TaskCard key={task.id} task={task} onClick={() => onTaskClickAction(task)} canDrag={canDrop} />
        ))}
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

export function KanbanBoard({ projectId, onTaskClick, canEdit = true, userRole }: {
  projectId: string;
  onTaskClick: (task: Task) => void;
  canEdit?: boolean;
  userRole?: TeamRole;
}) {
  const queryClient = useQueryClient()
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }))

  const boardQuery = useQuery({
    queryKey: ['tasks', 'board', projectId],
    queryFn: () => taskApi.board(projectId),
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
  const tasksByStatus = (status: TaskStatus) => tasks.filter((t) => t.status === status)

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
  const draggable = canEdit

  return draggable ? (
    <DndContext sensors={sensors} onDragEnd={handleDragEnd}>
      <div className="flex gap-4 overflow-x-auto pb-4 scrollbar-thin">
        {TASK_STATUSES.map((status) => (
          <Column
            key={status}
            status={status}
            tasks={tasksByStatus(status)}
            canDrop={getAllowedTargets(status, role).length > 0 || tasksByStatus(status).length === 0}
            onTaskClickAction={onTaskClick}
          />
        ))}
      </div>
    </DndContext>
  ) : (
    <div className="flex gap-4 overflow-x-auto pb-4 scrollbar-thin">
      {TASK_STATUSES.map((status) => (
        <Column
          key={status}
          status={status}
          tasks={tasksByStatus(status)}
          canDrop={false}
          onTaskClickAction={onTaskClick}
        />
      ))}
    </div>
  )
}
