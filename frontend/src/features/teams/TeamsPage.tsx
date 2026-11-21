import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { teamApi } from '../../api/endpoints'
import { Button, Card, EmptyState, ErrorBanner, Field, Input, Spinner } from '../../components/ui'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus, Users } from 'lucide-react'
import { toast } from 'sonner'
import { extractErrorMessage } from '../../api/client'

export function TeamsPage() {
  const queryClient = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['teams'],
    queryFn: () => teamApi.list(),
  })

  const createTeam = useMutation({
    mutationFn: () => teamApi.create({ name, description: description || undefined }),
    onSuccess: () => {
      toast.success('Team created')
      setName('')
      setDescription('')
      setShowForm(false)
      queryClient.invalidateQueries({ queryKey: ['teams'] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not create the team')),
  })

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-surface-900 tracking-tight">Teams</h1>
          <p className="mt-1 text-sm text-surface-500">Teams you belong to</p>
        </div>
        <Button onClick={() => setShowForm((v) => !v)}>
          <Plus className="h-4 w-4" />
          New team
        </Button>
      </div>

      {showForm && (
        <Card className="p-6 glass-card" glass>
          <form
            className="space-y-4"
            onSubmit={(e) => {
              e.preventDefault()
              createTeam.mutate()
            }}
          >
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Team name">
                <Input value={name} onChange={(e) => setName(e.target.value)} required maxLength={150} />
              </Field>
              <Field label="Description">
                <Input value={description} onChange={(e) => setDescription(e.target.value)} maxLength={2000} />
              </Field>
            </div>
            <div className="flex gap-2">
              <Button type="submit" loading={createTeam.isPending}>
                Create team
              </Button>
              <Button type="button" variant="secondary" onClick={() => setShowForm(false)}>
                Cancel
              </Button>
            </div>
          </form>
        </Card>
      )}

      {isLoading && (
        <div className="flex justify-center py-16">
          <Spinner />
        </div>
      )}

      {isError && <ErrorBanner message={extractErrorMessage(error, 'Could not load teams')} />}

      {data && data.content.length === 0 && (
        <EmptyState title="No teams yet" description="Create a team to start organizing projects and tasks." />
      )}

      {data && data.content.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.content.map((team) => (
            <Link key={team.id} to={`/teams/${team.id}`}>
              <Card className="h-full p-6 glass-card-hover group" glass>
                <div className="flex items-start gap-3">
                  <div className="p-2.5 rounded-xl bg-gradient-to-br from-brand-50 to-indigo-50 text-brand-600 group-hover:from-brand-100 group-hover:to-indigo-100 transition-colors">
                    <Users className="h-5 w-5" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-surface-900 truncate group-hover:text-brand-700 transition-colors">{team.name}</p>
                    <p className="mt-1 line-clamp-2 text-sm text-surface-500">{team.description || 'No description'}</p>
                  </div>
                </div>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
