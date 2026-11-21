import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { CalendarDays, FolderOpen, Plus, Search, Users, X } from 'lucide-react'
import { projectApi, teamApi } from '../../api/endpoints'
import { extractErrorMessage } from '../../api/client'
import { useAuth } from '../../context/AuthContext'
import { Button, Card, ErrorBanner, Field, Input, Spinner } from '../../components/ui'
import { EmptyState, PageHeader } from '../../components/PageHeader'
import { StatusBadge } from '../../components/StatusBadge'
import { PROJECT_STATUS_LABELS, type Project, type ProjectStatus, type Team } from '../../lib/types'

const PROJECT_STATUSES: ProjectStatus[] = ['PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED']

async function getAllTeams() {
  const firstPage = await teamApi.list(0, 50)
  const remainingPages = await Promise.all(
    Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) =>
      teamApi.list(index + 1, 50),
    ),
  )
  return [firstPage, ...remainingPages].flatMap((page) => page.content)
}

async function getAllProjects(teamId: string) {
  const firstPage = await projectApi.list(teamId, undefined, 0, 50)
  const remainingPages = await Promise.all(
    Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) =>
      projectApi.list(teamId, undefined, index + 1, 50),
    ),
  )
  return [firstPage, ...remainingPages].flatMap((page) => page.content)
}

export function ProjectsPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuth()
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<ProjectStatus | 'ALL'>('ALL')
  const [showForm, setShowForm] = useState(false)
  const [teamId, setTeamId] = useState('')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [status, setStatus] = useState<ProjectStatus>('PLANNING')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')

  useEffect(() => {
    const navigationState = location.state as { openCreateProject?: boolean; teamId?: string } | null
    if (!navigationState?.openCreateProject) return

    setShowForm(true)
    if (navigationState.teamId) setTeamId(navigationState.teamId)
    navigate(`${location.pathname}${location.search}`, { replace: true, state: null })
  }, [location.key, location.pathname, location.search, location.state, navigate])

  const teamsQuery = useQuery({
    queryKey: ['teams', 'all'],
    queryFn: getAllTeams,
  })

  const teams = teamsQuery.data ?? []
  const teamsById = useMemo(() => new Map<string, Team>(teams.map((team) => [team.id, team])), [teams])
  const teamMembersQueries = useQueries({
    queries: teams.map((team) => ({
      queryKey: ['teams', team.id, 'members'],
      queryFn: () => teamApi.listMembers(team.id),
      enabled: teamsQuery.isSuccess,
      staleTime: 30_000,
    })),
  })
  const creatableTeams = teams.filter((_, index) =>
    user?.role === 'ADMIN' || teamMembersQueries[index]?.data?.some(
      (member) => member.userId === user?.id && (member.teamRole === 'OWNER' || member.teamRole === 'MANAGER'),
    ),
  )
  const teamAccessLoading = teamMembersQueries.some((query) => query.isLoading)
  const teamAccessError = teamMembersQueries.find((query) => query.isError)?.error

  const projectsQuery = useQuery({
    queryKey: ['projects', 'all', teams.map((team) => team.id)],
    queryFn: async () => {
      const results = await Promise.all(teams.map((team) => getAllProjects(team.id)))
      return results.flat()
    },
    enabled: teamsQuery.isSuccess,
  })

  const createProject = useMutation({
    mutationFn: () =>
      projectApi.create({
        teamId,
        name: name.trim(),
        description: description.trim() || undefined,
        status,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
      }),
    onSuccess: () => {
      toast.success('Project created')
      setName('')
      setDescription('')
      setTeamId('')
      setStatus('PLANNING')
      setStartDate('')
      setEndDate('')
      setShowForm(false)
      queryClient.invalidateQueries({ queryKey: ['projects'] })
    },
    onError: (error) => toast.error(extractErrorMessage(error, 'Could not create the project')),
  })

  const projects: Project[] = projectsQuery.data ?? []
  const normalizedSearch = search.trim().toLocaleLowerCase()
  const filteredProjects = projects.filter((project) => {
    const team = teamsById.get(project.teamId)
    const matchesSearch =
      !normalizedSearch ||
      [project.name, project.description, team?.name]
        .some((value) => value?.toLocaleLowerCase().includes(normalizedSearch) ?? false)
    return matchesSearch && (statusFilter === 'ALL' || project.status === statusFilter)
  })

  const isLoading = teamsQuery.isLoading || projectsQuery.isLoading
  const error = teamsQuery.error || projectsQuery.error

  return (
    <div className="space-y-7 animate-fade-in">
      <PageHeader
        title="Projects"
        description="A clear view of your team's work, priorities, and progress."
        action={
          (teamAccessLoading || teamAccessError || creatableTeams.length > 0) && (
            <Button onClick={() => setShowForm((value) => !value)} aria-expanded={showForm}>
              {showForm ? <X className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
              {showForm ? 'Close' : 'New project'}
            </Button>
          )
        }
      />

      {showForm && (
        <Card className="p-5 sm:p-6">
          <div className="mb-5">
            <h2 className="text-lg font-semibold text-surface-900 dark:text-surface-100">Create a project</h2>
            <p className="mt-1 text-sm text-surface-500 dark:text-surface-400">
              Choose a team and set an initial status and timeline.
            </p>
          </div>
          {teamAccessError && creatableTeams.length === 0 ? (
            <ErrorBanner message={extractErrorMessage(teamAccessError, 'Could not verify team permissions')} />
          ) : teamAccessLoading && creatableTeams.length === 0 ? (
            <Spinner />
          ) : creatableTeams.length === 0 ? (
            <EmptyState
              title="No managed teams"
              description="You need to be a team owner or manager to create a project."
              icon={<Users className="h-10 w-10" />}
            />
          ) : (
            <form
              className="space-y-4"
              onSubmit={(event) => {
                event.preventDefault()
                createProject.mutate()
              }}
            >
              <div className="grid gap-4 sm:grid-cols-2">
                <Field label="Project name">
                  <Input
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    placeholder="e.g. Website redesign"
                    maxLength={150}
                    required
                  />
                </Field>
                <Field label="Team">
                  <select
                    className="glass-input"
                    value={teamId}
                    onChange={(event) => setTeamId(event.target.value)}
                    required
                  >
                    <option value="">Select a team</option>
                    {creatableTeams.map((team) => (
                      <option key={team.id} value={team.id}>{team.name}</option>
                    ))}
                  </select>
                </Field>
                <Field label="Description">
                  <Input
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    placeholder="What are you working on?"
                    maxLength={2000}
                  />
                </Field>
                <Field label="Status">
                  <select
                    className="glass-input"
                    value={status}
                    onChange={(event) => setStatus(event.target.value as ProjectStatus)}
                  >
                    {PROJECT_STATUSES.map((projectStatus) => (
                      <option key={projectStatus} value={projectStatus}>
                        {PROJECT_STATUS_LABELS[projectStatus]}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="Start date">
                  <Input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
                </Field>
                <Field label="Due date">
                  <Input
                    type="date"
                    value={endDate}
                    min={startDate || undefined}
                    onChange={(event) => setEndDate(event.target.value)}
                  />
                </Field>
              </div>
              <div className="flex flex-wrap gap-2 pt-1">
                <Button type="submit" loading={createProject.isPending}>Create project</Button>
                <Button type="button" variant="secondary" onClick={() => setShowForm(false)}>Cancel</Button>
              </div>
            </form>
          )}
        </Card>
      )}

      <Card className="p-4 sm:p-5">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div className="relative w-full lg:max-w-sm">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-surface-400" />
            <Input
              aria-label="Search projects"
              className="pl-10"
              placeholder="Search projects or teams..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <div className="flex items-center gap-2 overflow-x-auto pb-1 lg:pb-0" aria-label="Filter projects by status">
            <button
              type="button"
              onClick={() => setStatusFilter('ALL')}
              aria-pressed={statusFilter === 'ALL'}
              className={`shrink-0 rounded-full px-3 py-2 text-xs font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 ${
                statusFilter === 'ALL'
                  ? 'bg-brand-600 text-white shadow-sm'
                  : 'bg-surface-100 text-surface-600 hover:bg-surface-200 dark:bg-surface-800 dark:text-surface-300'
              }`}
            >
              All <span className="ml-1 opacity-75">{projects.length}</span>
            </button>
            {PROJECT_STATUSES.map((projectStatus) => {
              const count = projects.filter((project) => project.status === projectStatus).length
              return (
                <button
                  key={projectStatus}
                  type="button"
                  onClick={() => setStatusFilter(projectStatus)}
                  aria-pressed={statusFilter === projectStatus}
                  className={`shrink-0 rounded-full px-3 py-2 text-xs font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 ${
                    statusFilter === projectStatus
                      ? 'bg-brand-600 text-white shadow-sm'
                      : 'bg-surface-100 text-surface-600 hover:bg-surface-200 dark:bg-surface-800 dark:text-surface-300'
                  }`}
                >
                  {PROJECT_STATUS_LABELS[projectStatus]} <span className="ml-1 opacity-75">{count}</span>
                </button>
              )
            })}
          </div>
        </div>
      </Card>

      {isLoading && <Spinner />}

      {!isLoading && error && (
        <ErrorBanner message={extractErrorMessage(error, 'Could not load projects')} />
      )}

      {!isLoading && !error && projects.length === 0 && (
        <EmptyState
          title="No projects yet"
          description={
            teams.length === 0
              ? 'Create or join a team first, then start organizing its work into projects.'
              : 'Create a project for one of your teams to start organizing work.'
          }
          icon={<FolderOpen className="h-12 w-12" />}
          action={
            teams.length === 0
              ? <Button variant="secondary" onClick={() => navigate('/teams')}>Explore teams</Button>
              : creatableTeams.length > 0
                ? <Button onClick={() => setShowForm(true)}><Plus className="h-4 w-4" />Create project</Button>
                : undefined
          }
        />
      )}

      {!isLoading && !error && projects.length > 0 && filteredProjects.length === 0 && (
        <EmptyState
          title="No matching projects"
          description="Try a different search or status filter."
          icon={<Search className="h-10 w-10" />}
          action={
            <Button variant="secondary" onClick={() => { setSearch(''); setStatusFilter('ALL') }}>
              Clear filters
            </Button>
          }
        />
      )}

      {!isLoading && !error && filteredProjects.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {filteredProjects.map((project) => {
            const team = teamsById.get(project.teamId)
            return (
              <Link key={project.id} to={`/projects/${project.id}`} className="group">
                <Card className="h-full overflow-hidden p-5 transition-all duration-200 group-hover:-translate-y-0.5 group-hover:border-brand-200/70 group-hover:shadow-md sm:p-6">
                  <div className="mb-4 flex items-start justify-between gap-3">
                    <div className="flex min-w-0 items-center gap-3">
                      <span className="rounded-xl bg-brand-50 p-2.5 text-brand-600 dark:bg-brand-900/30 dark:text-brand-300">
                        <FolderOpen className="h-5 w-5" />
                      </span>
                      <h2 className="truncate font-semibold leading-tight text-surface-900 transition-colors group-hover:text-brand-700 dark:text-surface-100 dark:group-hover:text-brand-300">
                        {project.name}
                      </h2>
                    </div>
                    <StatusBadge status={project.status} className="shrink-0" />
                  </div>
                  <p className="mb-5 line-clamp-2 min-h-10 text-sm leading-5 text-surface-500 dark:text-surface-400">
                    {project.description || 'No description yet'}
                  </p>
                  <div className="flex flex-wrap items-center justify-between gap-2 border-t border-surface-100 pt-4 text-xs text-surface-500 dark:border-surface-800 dark:text-surface-400">
                    <span className="flex min-w-0 items-center gap-1.5 truncate">
                      <Users className="h-3.5 w-3.5 shrink-0" />
                      {team?.name ?? 'Unknown team'}
                    </span>
                    {project.endDate && (
                      <span className="flex items-center gap-1.5">
                        <CalendarDays className="h-3.5 w-3.5" />
                        {new Date(`${project.endDate}T00:00:00`).toLocaleDateString(undefined, {
                          month: 'short',
                          day: 'numeric',
                          year: 'numeric',
                        })}
                      </span>
                    )}
                  </div>
                </Card>
              </Link>
            )
          })}
        </div>
      )}
    </div>
  )
}
