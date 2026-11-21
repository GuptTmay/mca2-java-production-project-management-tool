import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, Link } from 'react-router-dom'
import { useState } from 'react'
import { toast } from 'sonner'
import { Plus, Trash2, Pencil, X, UserPlus, FolderOpen } from 'lucide-react'
import { teamApi, projectApi } from '../../api/endpoints'
import { Badge, Button, Card, EmptyState, ErrorBanner, Field, Input, Spinner } from '../../components/ui'
import { extractErrorMessage } from '../../api/client'
import { useAuth } from '../../context/AuthContext'
import { PageHeader, SectionHeader } from '../../components/PageHeader'
import { Avatar } from '../../components/Avatar'
import { StatusBadge } from '../../components/StatusBadge'
import type { TeamRole } from '../../lib/types'

export function TeamDetailPage() {
  const { teamId } = useParams<{ teamId: string }>()
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const [memberEmail, setMemberEmail] = useState('')
  const [memberRole, setMemberRole] = useState<TeamRole>('MEMBER')
  const [showProjectForm, setShowProjectForm] = useState(false)
  const [projectName, setProjectName] = useState('')
  const [projectCode, setProjectCode] = useState('')
  const [projectDescription, setProjectDescription] = useState('')
  const [projectStartDate, setProjectStartDate] = useState('')
  const [projectEndDate, setProjectEndDate] = useState('')
  const [isEditingTeam, setIsEditingTeam] = useState(false)
  const [editName, setEditName] = useState('')
  const [editDescription, setEditDescription] = useState('')

  const teamQuery = useQuery({
    queryKey: ['teams', teamId],
    queryFn: () => teamApi.getById(teamId!),
    enabled: !!teamId,
  })

  const membersQuery = useQuery({
    queryKey: ['teams', teamId, 'members'],
    queryFn: () => teamApi.listMembers(teamId!),
    enabled: !!teamId,
  })

  const projectsQuery = useQuery({
    queryKey: ['projects', { teamId }],
    queryFn: () => projectApi.list(teamId!),
    enabled: !!teamId,
  })

  const currentUserTeamRole = membersQuery.data?.find(m => m.userId === user?.id)?.teamRole
  const isOwner = currentUserTeamRole === 'OWNER'
  const isManager = currentUserTeamRole === 'MANAGER'
  const canManage = user?.role === 'PRODUCER' && (isOwner || isManager)

  const addMember = useMutation({
    mutationFn: () => teamApi.addMember(teamId!, memberEmail, memberRole),
    onSuccess: () => {
      toast.success('Member added successfully')
      setMemberEmail('')
      queryClient.invalidateQueries({ queryKey: ['teams', teamId, 'members'] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not add that member')),
  })

  const removeMember = useMutation({
    mutationFn: (userId: string) => teamApi.removeMember(teamId!, userId),
    onSuccess: () => {
      toast.success('Member removed')
      queryClient.invalidateQueries({ queryKey: ['teams', teamId, 'members'] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not remove that member')),
  })

  const updateMemberRole = useMutation({
    mutationFn: ({ userId, teamRole }: { userId: string; teamRole: TeamRole }) =>
      teamApi.updateMemberRole(teamId!, userId, teamRole),
    onSuccess: () => {
      toast.success('Member role updated')
      queryClient.invalidateQueries({ queryKey: ['teams', teamId, 'members'] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not update member role')),
  })

  const updateTeam = useMutation({
    mutationFn: () => teamApi.update(teamId!, { name: editName, description: editDescription }),
    onSuccess: () => {
      toast.success('Team updated')
      setIsEditingTeam(false)
      queryClient.invalidateQueries({ queryKey: ['teams', teamId] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not update team')),
  })

  const deleteTeam = useMutation({
    mutationFn: () => teamApi.remove(teamId!),
    onSuccess: () => {
      toast.success('Team deleted')
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not delete team')),
  })

  const createProject = useMutation({
    mutationFn: () => projectApi.create({
      teamId: teamId!,
      name: projectName,
      code: projectCode.trim().toUpperCase(),
      description: projectDescription || undefined,
      startDate: projectStartDate,
      endDate: projectEndDate,
    }),
    onSuccess: () => {
      toast.success('Project created')
      setProjectName('')
      setProjectCode('')
      setProjectDescription('')
      setProjectStartDate('')
      setProjectEndDate('')
      setShowProjectForm(false)
      queryClient.invalidateQueries({ queryKey: ['projects', { teamId }] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not create the project')),
  })

  if (teamQuery.isLoading) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    )
  }

  if (teamQuery.isError || !teamQuery.data) {
    return <ErrorBanner message={extractErrorMessage(teamQuery.error, 'Could not load this team')} />
  }

  const team = teamQuery.data

  return (
    <div className="space-y-8 animate-fade-in">
      <PageHeader
        title={team.name}
        description={team.description || 'No description provided'}
        action={
          <div className="flex items-center gap-2">
            {user?.role === 'PRODUCER' && isOwner && (
              <>
                <Button variant="secondary" onClick={() => { setEditName(team.name); setEditDescription(team.description || ''); setIsEditingTeam(true) }}>
                  <Pencil className="h-4 w-4" /> Edit
                </Button>
                <Button variant="danger" onClick={() => { if (confirm('Delete this team and all its projects?')) deleteTeam.mutate() }}>
                  <X className="h-4 w-4" /> Delete
                </Button>
              </>
            )}
          </div>
        }
      />

      {/* Edit Team Form */}
      {isEditingTeam && user?.role === 'PRODUCER' && isOwner && (
        <Card className="p-6">
          <h3 className="text-lg font-semibold text-surface-900 mb-4">Edit Team</h3>
          <div className="space-y-4">
            <Field label="Team Name">
              <Input value={editName} onChange={(e) => setEditName(e.target.value)} required maxLength={150} />
            </Field>
            <Field label="Description">
              <Input value={editDescription} onChange={(e) => setEditDescription(e.target.value)} maxLength={2000} />
            </Field>
            <div className="flex gap-2">
              <Button onClick={() => updateTeam.mutate()} loading={updateTeam.isPending}>Save Changes</Button>
              <Button variant="secondary" onClick={() => setIsEditingTeam(false)}>Cancel</Button>
            </div>
          </div>
        </Card>
      )}

      {/* Projects Section */}
      <div>
        <SectionHeader
          title="Projects"
          description="Manage your team's projects"
          action={
            canManage && (
              <Button onClick={() => setShowProjectForm(!showProjectForm)}>
                <Plus className="h-4 w-4" />
                New Project
              </Button>
            )
          }
        />
        {showProjectForm && canManage && (
          <Card className="p-6 mb-4">
            <form onSubmit={(e) => { e.preventDefault(); createProject.mutate() }} className="space-y-4">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Field label="Project Name">
                  <Input value={projectName} onChange={(e) => setProjectName(e.target.value)} required maxLength={150} />
                </Field>
                <Field label="Project Code">
                  <Input value={projectCode} onChange={(e) => setProjectCode(e.target.value.toUpperCase())} required maxLength={50} />
                </Field>
                <Field label="Description">
                  <Input value={projectDescription} onChange={(e) => setProjectDescription(e.target.value)} maxLength={2000} />
                </Field>
                <Field label="Start Date">
                  <Input type="date" value={projectStartDate} onChange={(e) => setProjectStartDate(e.target.value)} required />
                </Field>
                <Field label="Target End Date">
                  <Input type="date" value={projectEndDate} min={projectStartDate || undefined} onChange={(e) => setProjectEndDate(e.target.value)} required />
                </Field>
              </div>
              <div className="flex gap-2">
                <Button type="submit" loading={createProject.isPending}>Create Project</Button>
                <Button type="button" variant="secondary" onClick={() => setShowProjectForm(false)}>Cancel</Button>
              </div>
            </form>
          </Card>
        )}
        {projectsQuery.data && projectsQuery.data.content.length === 0 && (
          <EmptyState title="No projects yet" description="Create the team's first project to get started." icon={<FolderOpen className="h-12 w-12" />} />
        )}
        {projectsQuery.data && projectsQuery.data.content.length > 0 && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {projectsQuery.data.content.map((project) => (
              <Link key={project.id} to={`/projects/${project.id}`}>
                <Card className="h-full p-6 hover:shadow-md hover:border-surface-300/60 hover:-translate-y-0.5 transition-all duration-200 group">
                  <div className="flex items-start justify-between mb-3">
                    <h3 className="font-semibold text-surface-900 group-hover:text-brand-700 transition-colors">{project.name}</h3>
                    <StatusBadge status={project.status} />
                  </div>
                  <p className="text-sm text-surface-500 line-clamp-2 mb-4">{project.description || 'No description'}</p>
                  <div className="flex items-center justify-between text-xs text-surface-400">
                    <span>{new Date(project.createdAt).toLocaleDateString()}</span>
                  </div>
                </Card>
              </Link>
            ))}
          </div>
        )}
      </div>

      {/* Members Section */}
      <div>
        <SectionHeader
          title="Members"
          description={membersQuery.data
            ? `${membersQuery.data.length} ${membersQuery.data.length === 1 ? 'person' : 'people'} in this team`
            : 'People who have access to this team'}
        />
        <Card className="mt-4 p-4 sm:p-6">
          {canManage && (
            <form
              className="mb-5 grid gap-3 border-b border-surface-200/60 pb-5 sm:grid-cols-[minmax(0,1fr)_auto_auto] sm:items-end"
              onSubmit={(event) => {
                event.preventDefault()
                addMember.mutate()
              }}
            >
              <Field label="Invite by email">
                <Input
                  type="email"
                  autoComplete="email"
                  value={memberEmail}
                  onChange={(event) => setMemberEmail(event.target.value)}
                  placeholder="name@example.com"
                  required
                />
              </Field>
              <Field label="Team role">
                <select
                  className="glass-input cursor-pointer"
                  value={memberRole}
                  onChange={(event) => setMemberRole(event.target.value as TeamRole)}
                >
                  <option value="MEMBER">Member</option>
                  {isOwner && <option value="MANAGER">Manager</option>}
                </select>
              </Field>
              <Button type="submit" loading={addMember.isPending}>
                <UserPlus className="h-4 w-4" />
                Add member
              </Button>
            </form>
          )}

          {membersQuery.isLoading ? (
            <Spinner />
          ) : membersQuery.isError ? (
            <div className="space-y-3">
              <ErrorBanner message={extractErrorMessage(membersQuery.error, 'Could not load team members')} />
              <Button type="button" variant="secondary" size="sm" onClick={() => membersQuery.refetch()}>
                Try again
              </Button>
            </div>
          ) : !membersQuery.data?.length ? (
            <EmptyState
              title="No team members"
              description={canManage ? 'Invite people by email to collaborate with your team.' : 'No members are currently listed.'}
            />
          ) : (
            <ul className="divide-y divide-surface-100 dark:divide-surface-800" aria-label="Team members">
              {membersQuery.data.map((member) => {
                const canRemove = isOwner
                  ? member.teamRole !== 'OWNER'
                  : isManager && member.teamRole === 'MEMBER'
                const canChangeRole = isOwner && member.teamRole !== 'OWNER'
                const roleVariant = member.teamRole === 'OWNER'
                  ? 'warning'
                  : member.teamRole === 'MANAGER' ? 'info' : 'default'

                return (
                  <li
                    key={member.id}
                    className="flex flex-col gap-3 py-4 first:pt-0 last:pb-0 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div className="flex min-w-0 items-center gap-3">
                      <Avatar
                        name={`${member.userFirstName} ${member.userLastName}`}
                        email={member.userEmail}
                        size="md"
                      />
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold text-surface-800 dark:text-surface-100">
                          {member.userFirstName} {member.userLastName}
                          {member.userId === user?.id && <span className="ml-2 font-normal text-surface-400">(you)</span>}
                        </p>
                        <p className="truncate text-xs text-surface-500 dark:text-surface-400">{member.userEmail}</p>
                      </div>
                    </div>
                    <div className="flex flex-wrap items-center gap-2 pl-11 sm:pl-0">
                      <Badge variant={roleVariant}>{member.teamRole}</Badge>
                      {canChangeRole && (
                        <label className="sr-only" htmlFor={`team-role-${member.userId}`}>
                          Change role for {member.userFirstName} {member.userLastName}
                        </label>
                      )}
                      {canChangeRole && (
                        <select
                          id={`team-role-${member.userId}`}
                          className="glass-input cursor-pointer py-1.5 text-xs"
                          value={member.teamRole}
                          disabled={updateMemberRole.isPending}
                          onChange={(event) => updateMemberRole.mutate({
                            userId: member.userId,
                            teamRole: event.target.value as TeamRole,
                          })}
                        >
                          <option value="MEMBER">Member</option>
                          <option value="MANAGER">Manager</option>
                        </select>
                      )}
                      {canRemove && (
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          disabled={removeMember.isPending}
                          onClick={() => removeMember.mutate(member.userId)}
                          aria-label={`Remove ${member.userFirstName} ${member.userLastName} from team`}
                          title="Remove from team"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </Card>
      </div>
    </div>
  )
}
