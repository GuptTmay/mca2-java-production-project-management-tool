import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useLocation } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { toast } from 'sonner'
import { Plus, UserPlus, BarChart3, CheckCircle, AlertTriangle, Clock } from 'lucide-react'
import { projectApi, dashboardApi, teamApi } from '../../api/endpoints'
import { Button, Card, ErrorBanner, Field, Input, Spinner } from '../../components/ui'
import { extractErrorMessage } from '../../api/client'
import { useAuth } from '../../context/AuthContext'
import { PageHeader, SectionHeader } from '../../components/PageHeader'
import { Avatar } from '../../components/Avatar'
import { KanbanBoard } from '../board/KanbanBoard'
import { TaskDrawer } from '../board/TaskDrawer'
import { ActivityTimeline } from '../activities/ActivityTimeline'
import type { Task, TeamRole } from '../../lib/types'

export function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const location = useLocation()
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const [modalTask, setModalTask] = useState<Task | null>(null)
  const [showDrawer, setShowDrawer] = useState(false)
  const [memberEmail, setMemberEmail] = useState('')

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
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title={project.name}
        description={project.description || 'No description provided'}
        action={
          canManage && (
            <Button onClick={openCreateModal}>
              <Plus className="h-4 w-4" />
              New Task
            </Button>
          )
        }
      />

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
