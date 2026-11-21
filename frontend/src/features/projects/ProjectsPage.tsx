import { useQuery } from '@tanstack/react-query'
import { teamApi, projectApi } from '../../api/endpoints'
import { Card, Spinner, ErrorBanner } from '../../components/ui'
import { PageHeader, EmptyState } from '../../components/PageHeader'
import { StatusBadge } from '../../components/StatusBadge'
import { Link } from 'react-router-dom'
import { FolderOpen } from 'lucide-react'

export function ProjectsPage() {
  const teamsQuery = useQuery({
    queryKey: ['teams'],
    queryFn: () => teamApi.list(),
  })

  const { data: projectsData, isLoading: projectsLoading, error: projectsError } = useQuery({
    queryKey: ['projects', 'all'],
    queryFn: async () => {
      const teams = teamsQuery.data?.content || []
      const projectPromises = teams.map(team => projectApi.list(team.id))
      const results = await Promise.all(projectPromises)
      return results.flatMap(page => page.content || [])
    },
    enabled: !!teamsQuery.data,
  })

  if (teamsQuery.isLoading || projectsLoading) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    )
  }

  if (teamsQuery.isError || projectsError) {
    return <ErrorBanner message="Could not load projects" />
  }

  const projects = projectsData || []

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader title="Projects" description="Browse and manage all your projects" />

      {projects.length === 0 ? (
        <EmptyState
          title="No projects yet"
          description="Join or create a team to start managing projects."
          icon={<FolderOpen className="h-12 w-12" />}
        />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((project) => (
            <Link key={project.id} to={`/projects/${project.id}`}>
              <Card className="h-full p-6 hover:shadow-md hover:border-surface-300/60 hover:-translate-y-0.5 transition-all duration-200 group">
                <div className="flex items-start justify-between mb-3">
                  <h3 className="font-semibold text-surface-900 group-hover:text-brand-700 transition-colors">{project.name}</h3>
                  <StatusBadge status={project.status} />
                </div>
                <p className="text-sm text-surface-500 line-clamp-2 mb-4">{project.description || 'No description'}</p>
                <div className="flex items-center justify-between text-xs text-surface-400">
                  <span>Team: {project.teamId}</span>
                  {project.endDate && (
                    <span>Due {new Date(project.endDate).toLocaleDateString()}</span>
                  )}
                </div>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
