import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useLocation } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { toast } from 'sonner'
import { Plus, UserPlus, BarChart3, CheckCircle, AlertTriangle, Clock, Pencil, CalendarDays } from 'lucide-react'
import { projectApi, dashboardApi, teamApi } from '../../api/endpoints'
import { Button, Card, ErrorBanner, Field, Input, Spinner, Badge } from '../../components/ui'
import { extractErrorMessage } from '../../api/client'
import { useAuth } from '../../context/AuthContext'
import { PageHeader, SectionHeader } from '../../components/PageHeader'
import { StatusBadge } from '../../components/StatusBadge'
import { Avatar } from '../../components/Avatar'
import { KanbanBoard } from '../board/KanbanBoard'
import { TaskDrawer } from '../board/TaskDrawer'
import { ActivityTimeline } from '../activities/ActivityTimeline'
import { PROJECT_STATUS_LABELS, type ProjectStatus, type Task, type TeamRole } from '../../lib/types'

const PROJECT_STATUSES: ProjectStatus[] = ['PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED']

export function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const location = useLocation()
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const [modalTask, setModalTask] = useState<Task | null>(null)
  const [showDrawer, setShowDrawer] = useState(false)
  const [memberEmail, setMemberEmail] = useState('')
  const [isEditingProject, setIsEditingProject] = useState(false)
  const [editName, setEditName] = useState('')
  const [editDescription, setEditDescription] = useState('')
  const [editStatus, setEditStatus] = useState<ProjectStatus>('PLANNING')
  const [editStartDate, setEditStartDate] = useState('')
  const [editEndDate, setEditEndDate] = useState('')

  const projectQuery = useQuery({
    queryKey: ['projects', projectId],
    queryFn: () => projectApi.getById(projectId!),
    enabled: !!projectId,
  })

  const dashboardQuery = useQuery({
    queryKey: ['projects', projectId, 'dashboard'],
    queryFn: () => dashboardApi.project(projectId!),
    enabled: !!projectId,
  })

  const membersQuery = useQuery({
    queryKey: ['projects', projectId, 'members'],
    queryFn: () => projectApi.listMembers(projectId!),
    enabled: !!projectId,
  })

  const teamMembersQuery = useQuery({
    queryKey: ['teams', projectQuery.data?.teamId, 'members'],
    queryFn: () => teamApi.listMembers(projectQuery.data!.teamId),
    enabled: !!projectQuery.data?.teamId,
  })

  const currentUserTeamRole = teamMembersQuery.data?.find(m => m.userId === user?.id)?.teamRole
  const isOwner = currentUserTeamRole === 'OWNER'
  const isManager = currentUserTeamRole === 'MANAGER'
  const canManage = isOwner || isManager

  // Open task drawer from global New Task button or ?newTask=true nav, but only if user can manage
  useEffect(() => {
    function handleOpenTaskDrawer() {
      if (projectId && !teamMembersQuery.isLoading && canManage) {
        setModalTask(null)
        setShowDrawer(true)
      }
    }
    window.addEventListener('open-task-drawer', handleOpenTaskDrawer as EventListener)
    return () => window.removeEventListener('open-task-drawer', handleOpenTaskDrawer as EventListener)
  }, [projectId, teamMembersQuery.isLoading, canManage])

  // If we were navigated to with ?newTask=true, auto-open drawer once we know permissions
  useEffect(() => {
    if (projectId && location.search.includes('newTask=true') && !teamMembersQuery.isLoading && canManage) {
      setModalTask(null)
      setShowDrawer(true)
    }
  }, [projectId, location.search, teamMembersQuery.isLoading, canManage])

  const addMember = useMutation({
    mutationFn: () => projectApi.addMember(projectId!, memberEmail),
    onSuccess: () => {
      toast.success('Member added successfully')
      setMemberEmail('')
      queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'members'] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not add that member')),
  })

  const updateProject = useMutation({
    mutationFn: () =>
      projectApi.update(projectId!, {
        name: editName.trim(),
        description: editDescription.trim() || undefined,
        status: editStatus,
        startDate: editStartDate || undefined,
        endDate: editEndDate || undefined,
      }),
    onSuccess: () => {
      toast.success('Project updated')
      setIsEditingProject(false)
      queryClient.invalidateQueries({ queryKey: ['projects'] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not update the project')),
  })

  if (projectQuery.isLoading) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    )
  }

  if (projectQuery.isError || !projectQuery.data || !projectId) {
    return <ErrorBanner message={extractErrorMessage(projectQuery.error, 'Could not load this project')} />
  }

  const project = projectQuery.data
  const dashboard = dashboardQuery.data

  function openCreateModal() {
    setModalTask(null)
    setShowDrawer(true)
  }

  function openEditModal(task: Task) {
    setModalTask(task)
    setShowDrawer(true)
  }

  return (
    <div className="space-y-6 animate-fade-in min-w-0 max-w-full">
      <PageHeader
        title={project.name}
        description={project.description || 'No description provided'}
        action={
          <div className="flex flex-wrap items-center gap-2 sm:justify-end">
            {currentUserTeamRole && (
              <Badge variant={isOwner ? 'success' : isManager ? 'warning' : 'info'}>
                {currentUserTeamRole}
              </Badge>
            )}
            <StatusBadge status={project.status} />
            {canManage && (
              <Button
                variant="secondary"
                onClick={() => {
                  setEditName(project.name)
                  setEditDescription(project.description || '')
                  setEditStatus(project.status)
                  setEditStartDate(project.startDate || '')
                  setEditEndDate(project.endDate || '')
                  setIsEditingProject((value) => !value)
                }}
              >
                <Pencil className="h-4 w-4" />
                Edit project
              </Button>
            )}
            {canManage && (
              <Button onClick={openCreateModal}>
                <Plus className="h-4 w-4" />
                New Task
              </Button>
            )}
          </div>
        }
      />

      {isEditingProject && canManage && (
        <Card className="p-5 sm:p-6">
          <div className="mb-5">
            <h2 className="text-lg font-semibold text-surface-900 dark:text-surface-100">Edit project</h2>
            <p className="mt-1 text-sm text-surface-500 dark:text-surface-400">
              Update the project details, timeline, or status.
            </p>
          </div>
          <form
            className="space-y-4"
            onSubmit={(event) => {
              event.preventDefault()
              updateProject.mutate()
            }}
          >
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Project name">
                <Input value={editName} onChange={(event) => setEditName(event.target.value)} required maxLength={150} />
              </Field>
              <Field label="Status">
                <select
                  className="glass-input"
                  value={editStatus}
                  onChange={(event) => setEditStatus(event.target.value as ProjectStatus)}
                >
                  {PROJECT_STATUSES.map((projectStatus) => (
                    <option key={projectStatus} value={projectStatus}>
                      {PROJECT_STATUS_LABELS[projectStatus]}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Description">
                <Input
                  value={editDescription}
                  onChange={(event) => setEditDescription(event.target.value)}
                  maxLength={2000}
                />
              </Field>
              <Field label="Start date">
                <Input type="date" value={editStartDate} onChange={(event) => setEditStartDate(event.target.value)} />
              </Field>
              <Field label="Due date">
                <Input
                  type="date"
                  value={editEndDate}
                  min={editStartDate || undefined}
                  onChange={(event) => setEditEndDate(event.target.value)}
                />
              </Field>
            </div>
            <div className="flex flex-wrap gap-2 pt-1">
              <Button type="submit" loading={updateProject.isPending}>Save changes</Button>
              <Button type="button" variant="secondary" onClick={() => setIsEditingProject(false)}>Cancel</Button>
            </div>
          </form>
        </Card>
      )}

      {(project.startDate || project.endDate) && (
        <Card className="flex flex-wrap items-center gap-x-6 gap-y-3 px-5 py-4">
          <div className="flex items-center gap-2 text-sm font-medium text-surface-600 dark:text-surface-300">
            <CalendarDays className="h-4 w-4 text-brand-600 dark:text-brand-400" />
            <span>Project timeline</span>
          </div>
          {project.startDate && (
            <p className="text-sm text-surface-500 dark:text-surface-400">
              Starts <span className="font-semibold text-surface-700 dark:text-surface-200">
                {new Date(`${project.startDate}T00:00:00`).toLocaleDateString()}
              </span>
            </p>
          )}
          {project.endDate && (
            <p className="text-sm text-surface-500 dark:text-surface-400">
              Due <span className="font-semibold text-surface-700 dark:text-surface-200">
                {new Date(`${project.endDate}T00:00:00`).toLocaleDateString()}
              </span>
            </p>
          )}
        </Card>
      )}

      {/* Dashboard Stats */}
      {dashboard && (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <Card className="stat-card">
            <div className="flex items-center gap-3">
              <div className="stat-card-icon">
                <BarChart3 className="h-5 w-5" />
              </div>
              <div>
                <p className="text-xs font-semibold text-surface-400 uppercase tracking-wider">Total Tasks</p>
                <p className="text-2xl font-bold text-surface-900">{dashboard.totalTasks}</p>
              </div>
            </div>
          </Card>
          <Card className="stat-card">
            <div className="flex items-center gap-3">
              <div className="stat-card-icon bg-gradient-to-br from-emerald-50 to-teal-50 text-emerald-600">
                <CheckCircle className="h-5 w-5" />
              </div>
              <div>
                <p className="text-xs font-semibold text-surface-400 uppercase tracking-wider">Completion</p>
                <p className="text-2xl font-bold text-emerald-600">{dashboard.completionPercentage}%</p>
              </div>
            </div>
          </Card>
          <Card className="stat-card">
            <div className="flex items-center gap-3">
              <div className="stat-card-icon bg-gradient-to-br from-rose-50 to-red-50 text-rose-600">
                <AlertTriangle className="h-5 w-5" />
              </div>
              <div>
                <p className="text-xs font-semibold text-surface-400 uppercase tracking-wider">Overdue</p>
                <p className={`text-2xl font-bold ${dashboard.overdueTasks > 0 ? 'text-rose-600' : 'text-surface-900'}`}>
                  {dashboard.overdueTasks}
                </p>
              </div>
            </div>
          </Card>
          <Card className="stat-card">
            <div className="flex items-center gap-3">
              <div className="stat-card-icon bg-gradient-to-br from-amber-50 to-orange-50 text-amber-600">
                <Clock className="h-5 w-5" />
              </div>
              <div>
                <p className="text-xs font-semibold text-surface-400 uppercase tracking-wider">Pending</p>
                <p className="text-2xl font-bold text-surface-900">{dashboard.pendingTasks}</p>
              </div>
            </div>
          </Card>
        </div>
      )}

      {/* Members Section */}
      {canManage && (
        <Card className="p-6">
          <SectionHeader
            title="Project Members"
            description="Manage who has access to this project"
            action={
              <Button onClick={() => (document.getElementById('add-project-member-form') as HTMLFormElement | null)?.requestSubmit()}>
                <UserPlus className="h-4 w-4" />
                Add Member
              </Button>
            }
          />
          <form
            id="add-project-member-form"
            className="flex flex-wrap items-end gap-3 mb-4"
            onSubmit={(e) => {
              e.preventDefault()
              addMember.mutate()
            }}
          >
            <div className="flex-1 min-w-[200px]">
              <Field label="User email">
                <Input
                  value={memberEmail}
                  onChange={(e) => setMemberEmail(e.target.value)}
                  placeholder="email of the user to add"
                  required
                />
              </Field>
            </div>
            <Button type="submit" loading={addMember.isPending}>Add</Button>
          </form>
          {membersQuery.data && membersQuery.data.length > 0 && (
            <div className="rounded-2xl border border-surface-200/60 bg-white/40 backdrop-blur-sm divide-y divide-surface-100">
              {membersQuery.data.map((member) => (
                <div key={member.id} className="flex items-center justify-between px-4 py-3 hover:bg-white/40 transition-colors">
                  <div className="flex items-center gap-3">
                    <Avatar name={`${member.userFirstName} ${member.userLastName}`} email={member.userEmail} size="sm" />
                    <div>
                      <p className="text-sm font-semibold text-surface-800">
                        {member.userFirstName} {member.userLastName}
                      </p>
                      <p className="text-xs text-surface-400">{member.userEmail}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      )}

      {/* Kanban Board */}
      <div>
        <SectionHeader title="Board" description="Drag and drop tasks to update their status" />
        <KanbanBoard projectId={projectId} onTaskClick={openEditModal} canEdit={canManage} userRole={(currentUserTeamRole as TeamRole) || 'MEMBER'} />
      </div>

      {/* Activity Timeline */}
      <div>
        <SectionHeader title="Activity" description="Recent activity in this project" />
        <ActivityTimeline projectId={projectId} />
      </div>

      {/* Task Drawer */}
      {showDrawer && <TaskDrawer projectId={projectId} task={modalTask} onClose={() => setShowDrawer(false)} canEdit={canManage} />}
    </div>
  )
}
