import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { userApi } from '../../api/endpoints'
import { Button, ErrorBanner, Input, Spinner } from '../../components/ui'
import { extractErrorMessage } from '../../api/client'
import { useAuth } from '../../context/AuthContext'
import { useState } from 'react'
import type { Role } from '../../lib/types'

const ROLES: Role[] = ['PRODUCER', 'CONTENT_MANAGER', 'TEAM_MEMBER']
const ROLE_LABELS: Record<Role, string> = {
  PRODUCER: 'Producer',
  CONTENT_MANAGER: 'Content Manager',
  TEAM_MEMBER: 'Team Member',
}

export function AdminUsersPage() {
  const queryClient = useQueryClient()
  const { user: currentUser } = useAuth()
  const [page, setPage] = useState(0)
  const [profileEdits, setProfileEdits] = useState<Record<string, { firstName: string; lastName: string }>>({})

  const usersQuery = useQuery({
    queryKey: ['admin', 'users', page],
    queryFn: () => userApi.list(page, 50),
  })

  const updateRole = useMutation({
    mutationFn: ({ id, role }: { id: string; role: Role }) => userApi.updateRole(id, role),
    onSuccess: () => {
      toast.success('Role updated')
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not update that user\u2019s role')),
  })

  const updateEnabled = useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) => userApi.updateEnabled(id, enabled),
    onSuccess: (updatedUser) => {
      toast.success(updatedUser.enabled ? 'Account activated' : 'Account deactivated')
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not update that account')),
  })

  const updateProfile = useMutation({
    mutationFn: ({ id, firstName, lastName }: { id: string; firstName: string; lastName: string }) =>
      userApi.updateProfile(id, { firstName, lastName }),
    onSuccess: (updatedUser) => {
      toast.success('User details updated')
      setProfileEdits((edits) => {
        const next = { ...edits }
        delete next[updatedUser.id]
        return next
      })
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not update user details')),
  })

  if (usersQuery.isLoading) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    )
  }

  if (usersQuery.isError || !usersQuery.data) {
    return <ErrorBanner message={extractErrorMessage(usersQuery.error, 'Could not load users')} />
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h1 className="text-3xl font-bold text-surface-900 tracking-tight">User management</h1>
        <p className="mt-1 text-sm text-surface-500">Manage roles and account access for your workspace</p>
      </div>

      <div className="overflow-x-auto rounded-2xl border border-surface-200/60 bg-white/60 backdrop-blur-sm">
        <table className="min-w-full divide-y divide-surface-100 text-sm">
          <thead className="bg-surface-50/80 text-left text-xs font-bold uppercase text-surface-500">
            <tr>
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Email</th>
              <th className="px-4 py-3">Role</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Access</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-surface-100">
            {usersQuery.data.content.map((u) => (
              <tr key={u.id} className="hover:bg-white/40 transition-colors">
                <td className="min-w-[320px] px-4 py-3">
                  <div className="flex items-center gap-2">
                    <Input
                      aria-label={`${u.email} first name`}
                      value={(profileEdits[u.id] ?? u).firstName}
                      onChange={(event) => setProfileEdits((edits) => ({
                        ...edits,
                        [u.id]: { ...(edits[u.id] ?? { firstName: u.firstName, lastName: u.lastName }), firstName: event.target.value },
                      }))}
                      maxLength={100}
                    />
                    <Input
                      aria-label={`${u.email} last name`}
                      value={(profileEdits[u.id] ?? u).lastName}
                      onChange={(event) => setProfileEdits((edits) => ({
                        ...edits,
                        [u.id]: { ...(edits[u.id] ?? { firstName: u.firstName, lastName: u.lastName }), lastName: event.target.value },
                      }))}
                      maxLength={100}
                    />
                    <button
                      type="button"
                      disabled={updateProfile.isPending || !profileEdits[u.id]}
                      onClick={() => updateProfile.mutate({ id: u.id, ...profileEdits[u.id] })}
                      className="rounded-lg border border-surface-200 px-3 py-1.5 text-xs font-semibold text-surface-700 transition hover:bg-surface-100 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      Save
                    </button>
                  </div>
                </td>
                <td className="px-4 py-3 text-surface-500">{u.email}</td>
                <td className="px-4 py-3">
                  <select
                    className="glass-input py-1.5 text-sm font-medium w-auto"
                    value={u.role}
                    disabled={u.id === currentUser?.id || updateRole.isPending}
                    onChange={(e) => updateRole.mutate({ id: u.id, role: e.target.value as Role })}
                  >
                    {ROLES.map((role) => (
                      <option key={role} value={role}>
                        {ROLE_LABELS[role]}
                      </option>
                    ))}
                  </select>
                </td>
                <td className="px-4 py-3">
                  <span className={u.enabled ? 'text-emerald-700' : 'text-rose-600'}>
                    {u.enabled ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <button
                    type="button"
                    disabled={u.id === currentUser?.id || updateEnabled.isPending}
                    onClick={() => updateEnabled.mutate({ id: u.id, enabled: !u.enabled })}
                    className="rounded-lg border border-surface-200 px-3 py-1.5 text-xs font-semibold text-surface-700 transition hover:bg-surface-100 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {u.enabled ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {usersQuery.data.totalPages > 1 && (
        <div className="flex items-center justify-between gap-4">
          <p className="text-sm text-surface-500" aria-live="polite">
            Showing {page * usersQuery.data.size + 1}–
            {page * usersQuery.data.size + usersQuery.data.content.length} of {usersQuery.data.totalElements} users
          </p>
          <div className="flex gap-2">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              disabled={page === 0 || usersQuery.isFetching}
              onClick={() => setPage((current) => current - 1)}
            >
              Previous
            </Button>
            <Button
              type="button"
              variant="secondary"
              size="sm"
              disabled={page + 1 >= usersQuery.data.totalPages || usersQuery.isFetching}
              onClick={() => setPage((current) => current + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
