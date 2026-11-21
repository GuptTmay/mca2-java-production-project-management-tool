import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { X, Trash2, MessageSquare, Clock, History } from 'lucide-react'
import clsx from 'clsx'
import { taskApi, labelApi, projectApi } from '../../api/endpoints'
import { Button, Field, Input, Textarea, ErrorBanner, Spinner } from '../../components/ui'
import { extractErrorMessage } from '../../api/client'
import { TASK_PRIORITIES, type Task, type TaskPriority } from '../../lib/types'
import { CommentSection } from '../comments/CommentSection'
import { ActivityTimeline } from '../activities/ActivityTimeline'

interface TaskDrawerProps {
  projectId: string
  task: Task | null
  onClose: () => void
  canEdit?: boolean
}

type Tab = 'details' | 'comments' | 'activity' | 'history'

export function TaskDrawer({ projectId, task, onClose, canEdit = true }: TaskDrawerProps) {
  const queryClient = useQueryClient()
  const isEditMode = !!task
  const [activeTab, setActiveTab] = useState<Tab>('details')

  const [title, setTitle] = useState(task?.title ?? '')
  const [description, setDescription] = useState(task?.description ?? '')
  const [priority, setPriority] = useState<TaskPriority>(task?.priority ?? 'MEDIUM')
  const [deadline, setDeadline] = useState(task?.deadline ? task.deadline.slice(0, 10) : '')
  const [assigneeId, setAssigneeId] = useState(task?.assigneeId ?? '')

  useEffect(() => {
    setTitle(task?.title ?? '')
    setDescription(task?.description ?? '')
    setPriority(task?.priority ?? 'MEDIUM')
    setDeadline(task?.deadline ? task.deadline.slice(0, 10) : '')
    setAssigneeId(task?.assigneeId ?? '')
    setActiveTab('details')
  }, [task])

  const labelsQuery = useQuery({ queryKey: ['labels'], queryFn: () => labelApi.list() })
  const membersQuery = useQuery({
    queryKey: ['projects', projectId, 'members'],
    queryFn: () => projectApi.listMembers(projectId),
  })

  const historyQuery = useQuery({
    queryKey: ['tasks', task?.id, 'history'],
    queryFn: () => taskApi.history(task!.id),
    enabled: isEditMode && activeTab === 'history' && !!task?.id,
  })

  function invalidateBoard() {
    queryClient.invalidateQueries({ queryKey: ['tasks', 'board', projectId] })
  }

  const createTask = useMutation({
    mutationFn: () =>
      taskApi.create({
        projectId,
        title,
        description: description || undefined,
        priority,
        assigneeId: assigneeId || undefined,
        deadline: deadline ? new Date(deadline).toISOString() : undefined,
      }),
    onSuccess: () => {
      toast.success('Task created successfully')
      invalidateBoard()
      onClose()
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not create the task')),
  })

  const updateTask = useMutation({
    mutationFn: () =>
      taskApi.update(task!.id, {
        title,
        description: description || undefined,
        priority,
        deadline: deadline ? new Date(deadline).toISOString() : undefined,
      }),
    onSuccess: () => {
      toast.success('Task updated successfully')
      invalidateBoard()
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not update the task')),
  })

  const assignTask = useMutation({
    mutationFn: (newAssigneeId: string) => taskApi.assign(task!.id, newAssigneeId || null),
    onSuccess: () => {
      toast.success('Assignment updated')
      invalidateBoard()
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Only a team owner or manager can reassign tasks')),
  })

  const deleteTask = useMutation({
    mutationFn: () => taskApi.remove(task!.id),
    onSuccess: () => {
      toast.success('Task deleted successfully')
      invalidateBoard()
      onClose()
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not delete the task')),
  })

  const toggleLabel = useMutation({
    mutationFn: ({ labelId, attached }: { labelId: string; attached: boolean }) =>
      attached ? taskApi.removeLabel(task!.id, labelId) : taskApi.addLabel(task!.id, labelId),
    onSuccess: () => invalidateBoard(),
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not update labels')),
  })

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (isEditMode) {
      updateTask.mutate()
    } else {
      createTask.mutate()
    }
  }

  const attachedLabelIds = new Set((task?.labels ?? []).map((l) => l.id))

  const tabs: { key: Tab; label: string; icon: any }[] = isEditMode
    ? [
        { key: 'details' as Tab, label: 'Details', icon: null },
        { key: 'comments' as Tab, label: 'Comments', icon: MessageSquare },
        { key: 'activity' as Tab, label: 'Activity', icon: Clock },
        { key: 'history' as Tab, label: 'History', icon: History },
      ]
    : [{ key: 'details' as Tab, label: 'Details', icon: null }]

  return (
    <div className="drawer-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="drawer-content">
        <div className="sticky top-0 z-10 flex items-center justify-between bg-white/90 backdrop-blur-xl px-6 py-4 border-b border-surface-200/60">
          <h2 className="text-lg font-semibold text-surface-900">{isEditMode ? 'Edit Task' : 'New Task'}</h2>
          <button onClick={onClose} className="text-surface-400 hover:text-surface-600 transition-colors">
            <X className="h-5 w-5" />
          </button>
        </div>

        {isEditMode && (
          <div className="flex border-b border-surface-200/60 px-6">
            {tabs.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={clsx(
                  'flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors border-b-2',
                  activeTab === tab.key
                    ? 'border-brand-600 text-brand-700'
                    : 'border-transparent text-surface-500 hover:text-surface-700'
                )}
              >
                {tab.icon && <tab.icon className="h-4 w-4" />}
                {tab.label}
              </button>
            ))}
          </div>
        )}

        <div className="p-6">
          {activeTab === 'details' && (
            <div className="space-y-5">
              {canEdit ? (
                <form onSubmit={handleSubmit} className="space-y-5">
                  <Field label="Title">
                    <Input value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={200} />
                  </Field>
                  <Field label="Description">
                    <Textarea rows={3} value={description} onChange={(e) => setDescription(e.target.value)} maxLength={10000} />
                  </Field>
                  <div className="grid grid-cols-2 gap-4">
                    <Field label="Priority">
                      <select
                        className="glass-input cursor-pointer"
                        value={priority}
                        onChange={(e) => setPriority(e.target.value as TaskPriority)}
                      >
                        {TASK_PRIORITIES.map((p) => (
                          <option key={p} value={p}>
                            {p}
                          </option>
                        ))}
                      </select>
                    </Field>
                    <Field label="Deadline">
                      <Input type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} />
                    </Field>
                  </div>

                  {!isEditMode && (
                    <Field label="Assignee (optional)">
                      <select
                        className="glass-input cursor-pointer"
                        value={assigneeId}
                        onChange={(e) => setAssigneeId(e.target.value)}
                      >
                        <option value="">Unassigned</option>
                        {membersQuery.data?.map((m) => (
                          <option key={m.userId} value={m.userId}>
                            {m.userFirstName} {m.userLastName}
                          </option>
                        ))}
                      </select>
                    </Field>
                  )}

                  {isEditMode && (
                    <Field label="Assignee">
                      <select
                        className="glass-input cursor-pointer"
                        value={task?.assigneeId ?? ''}
                        onChange={(e) => assignTask.mutate(e.target.value)}
                      >
                        <option value="">Unassigned</option>
                        {membersQuery.data?.map((m) => (
                          <option key={m.userId} value={m.userId}>
                            {m.userFirstName} {m.userLastName}
                          </option>
                        ))}
                      </select>
                      <p className="mt-1.5 text-xs text-surface-400">
                        Reassigning requires being a team owner/manager or admin - the server enforces this.
                      </p>
                    </Field>
                  )}

                  {isEditMode && labelsQuery.data && (
                    <Field label="Labels">
                      <div className="flex flex-wrap gap-2">
                        {labelsQuery.data.map((label) => {
                          const attached = attachedLabelIds.has(label.id)
                          return (
                            <button
                              type="button"
                              key={label.id}
                              onClick={() => toggleLabel.mutate({ labelId: label.id, attached })}
                              className="rounded-full px-3 py-1.5 text-xs font-medium text-white shadow-sm transition-opacity"
                              style={{ backgroundColor: label.color, opacity: attached ? 1 : 0.4 }}
                            >
                              {label.name}
                            </button>
                          )
                        })}
                      </div>
                    </Field>
                  )}

                  {(createTask.isError || updateTask.isError) && (
                    <ErrorBanner
                      message={extractErrorMessage(createTask.error ?? updateTask.error, 'Something went wrong')}
                    />
                  )}

                  <div className="flex items-center justify-between pt-2">
                    {isEditMode ? (
                      <button
                        type="button"
                        onClick={() => deleteTask.mutate()}
                        className="flex items-center gap-1.5 text-sm text-rose-600 hover:text-rose-700 transition-colors"
                      >
                        <Trash2 className="h-4 w-4" />
                        Delete task
                      </button>
                    ) : (
                      <span />
                    )}
                    <div className="flex gap-2">
                      <Button type="button" variant="secondary" onClick={onClose}>
                        Cancel
                      </Button>
                      <Button type="submit" loading={createTask.isPending || updateTask.isPending}>
                        {isEditMode ? 'Save changes' : 'Create task'}
                      </Button>
                    </div>
                  </div>
                </form>
              ) : (
                <div className="space-y-4">
                  <div>
                    <p className="text-xs font-semibold text-surface-400 uppercase tracking-wider">Title</p>
                    <p className="text-sm font-medium text-surface-900">{task?.title}</p>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-surface-400 uppercase tracking-wider">Description</p>
                    <p className="text-sm text-surface-700 whitespace-pre-wrap">{task?.description || 'No description'}</p>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="text-xs font-semibold text-surface-400 uppercase tracking-wider">Priority</p>
                      <p className="text-sm text-surface-900">{task?.priority}</p>
                    </div>
                    <div>
                      <p className="text-xs font-semibold text-surface-400 uppercase tracking-wider">Deadline</p>
                      <p className="text-sm text-surface-900">{task?.deadline ? new Date(task.deadline).toLocaleDateString() : 'None'}</p>
                    </div>
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-surface-400 uppercase tracking-wider">Assignee</p>
                    <p className="text-sm text-surface-900">{membersQuery.data?.find(m => m.userId === task?.assigneeId) ? `${membersQuery.data.find(m => m.userId === task?.assigneeId)!.userFirstName} ${membersQuery.data.find(m => m.userId === task?.assigneeId)!.userLastName}` : 'Unassigned'}</p>
                  </div>
                  {task?.labels && task.labels.length > 0 && (
                    <div>
                      <p className="text-xs font-semibold text-surface-400 uppercase tracking-wider">Labels</p>
                      <div className="flex flex-wrap gap-2 mt-1">
                        {task.labels.map((label) => (
                          <span
                            key={label.id}
                            className="rounded-full px-3 py-1 text-xs font-medium text-white shadow-sm"
                            style={{ backgroundColor: label.color }}
                          >
                            {label.name}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                  <div className="pt-2">
                    <Button type="button" variant="secondary" onClick={onClose}>
                      Close
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}

          {activeTab === 'history' && isEditMode && task && (
            <div className="space-y-4">
              {historyQuery.isLoading ? (
                <div className="flex justify-center py-8">
                  <Spinner />
                </div>
              ) : historyQuery.isError ? (
                <ErrorBanner message={extractErrorMessage(historyQuery.error, 'Could not load task history')} />
              ) : historyQuery.data && historyQuery.data.length > 0 ? (
                <div className="space-y-3">
                  {historyQuery.data.map((entry) => (
                    <div key={entry.id} className="rounded-xl border border-surface-200/60 bg-white/60 p-4">
                      <div className="flex items-center justify-between">
                        <p className="text-sm font-semibold text-surface-900">{entry.actorName}</p>
                        <p className="text-xs text-surface-400">
                          {new Date(entry.createdAt).toLocaleString()}
                        </p>
                      </div>
                      <div className="mt-2 flex items-center gap-2 text-sm">
                        <span className="rounded-full bg-surface-100 px-2 py-0.5 text-xs font-bold text-surface-700">
                          {entry.previousStatus || 'Created'}
                        </span>
                        <span className="text-surface-400">→</span>
                        <span className="rounded-full bg-brand-50 px-2 py-0.5 text-xs font-bold text-brand-700">
                          {entry.newStatus}
                        </span>
                      </div>
                      {entry.changeReason && (
                        <p className="mt-2 text-xs text-surface-500 italic">Reason: {entry.changeReason}</p>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-surface-400 text-center py-8">No history yet.</p>
              )}
            </div>
          )}

          {activeTab === 'comments' && isEditMode && task && (
            <CommentSection taskId={task.id} />
          )}

          {activeTab === 'activity' && isEditMode && task && (
            <ActivityTimeline taskId={task.id} />
          )}
        </div>

        {(labelsQuery.isLoading || membersQuery.isLoading) && (
          <div className="px-6 pb-6 flex justify-center">
            <Spinner />
          </div>
        )}
      </div>
    </div>
  )
}
