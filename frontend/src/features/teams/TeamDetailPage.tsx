import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, Link } from 'react-router-dom'
import { useState } from 'react'
import { toast } from 'sonner'
import { Plus, Trash2, Pencil, X, UserPlus, FolderOpen } from 'lucide-react'
import { teamApi, projectApi } from '../../api/endpoints'
import { Button, Card, EmptyState, ErrorBanner, Field, Input, Spinner } from '../../components/ui'
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
  const [projectDescription, setProjectDescription] = useState('')
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
  const canManage = isOwner || isManager

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
    mutationFn: () => projectApi.create({ teamId: teamId!, name: projectName, description: projectDescription || undefined }),
    onSuccess: () => {
      toast.success('Project created')
      setProjectName('')
      setProjectDescription('')
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
            {isOwner && (
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
      {isEditingTeam && (
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
                <Field label="Description">
                  <Input value={projectDescription} onChange={(e) => setProjectDescription(e.target.value)} maxLength={2000} />
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
      {canManage && (
        <div>
          <SectionHeader
            title="Members"
            description="Manage team members and roles"
            action={
              <Button onClick={() => (document.getElementById('add-member-form') as HTMLFormElement | null)?.requestSubmit()}>
                <UserPlus className="h-4 w-4" />
                Add Member
              </Button>
            }
          />
          <Card className="p-6">
            <form
              id="add-member-form"
              className="flex flex-wrap items-end gap-3 mb-6"
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
              <div>
                <Field label="Role">
                  <select
                    className="glass-input cursor-pointer"
                    value={memberRole}
                    onChange={(e) => setMemberRole(e.target.value as TeamRole)}
                  >
                    <option value="MEMBER">Member</option>
                    {isOwner && <option value="MANAGER">Manager</option>}
                  </select>
                </Field>
              </div>
            </form>
            {membersQuery.data && membersQuery.data.length > 0 && (
              <div className="rounded-2xl border border-surface-200/60 bg-white/40 backdrop-blur-sm divide-y divide-surface-100">
                {membersQuery.data.map((member) => {
                  const canRemove = isOwner || (isManager && member.teamRole === 'MEMBER')
                  const canChangeRole = isOwner && member.teamRole !== 'OWNER'
                  return (
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
                      <div className="flex items-center gap-3">
                        <StatusBadge status={member.teamRole === 'OWNER' ? 'DONE' : member.teamRole === 'MANAGER' ? 'IN_PROGRESS' : 'TODO'} />
                        {canChangeRole && (
                          <select
                            className="glass-input cursor-pointer text-xs py-1 px-2 w-32"
                            value={member.teamRole}
                            onChange={(e) => {
                              const newRole = e.target.value as TeamRole
                              if (newRole !== member.teamRole) {
                                teamApi.updateMemberRole(teamId!, member.userId, newRole).then(() => {
                                  toast.success('Role updated')
                                  queryClient.invalidateQueries({ queryKey: ['teams', teamId, 'members'] })
                                }).catch((err) => toast.error(extractErrorMessage(err, 'Could not update role')))
                              }
                            }}
                          >
                            <option value="MEMBER">Member</option>
                            <option value="MANAGER">Manager</option>
                          </select>
                        )}
                        {canRemove && (
                          <button
                            onClick={() => removeMember.mutate(member.userId)}
                            className="text-surface-400 hover:text-rose-600 transition-colors p-1.5 rounded-lg hover:bg-rose-50"
                            title="Remove member"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </Card>
        </div>
      )}
    </div>
  )
}
