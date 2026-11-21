import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { userApi } from '../../api/endpoints'
import { ErrorBanner, Spinner } from '../../components/ui'
import { extractErrorMessage } from '../../api/client'
import type { Role } from '../../lib/types'

const ROLES: Role[] = ['ADMIN', 'PROJECT_MANAGER', 'DEVELOPER']

export function AdminUsersPage() {
  const queryClient = useQueryClient()

  const usersQuery = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: () => userApi.list(0, 50),
  })

  const updateRole = useMutation({
    mutationFn: ({ id, role }: { id: string; role: Role }) => userApi.updateRole(id, role),
    onSuccess: () => {
      toast.success('Role updated')
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not update that user\u2019s role')),
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
        <p className="mt-1 text-sm text-surface-500">Change global roles for any user (ADMIN only)</p>
      </div>

      <div className="overflow-hidden rounded-2xl border border-surface-200/60 bg-white/60 backdrop-blur-sm">
        <table className="min-w-full divide-y divide-surface-100 text-sm">
          <thead className="bg-surface-50/80 text-left text-xs font-bold uppercase text-surface-500">
            <tr>
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Email</th>
              <th className="px-4 py-3">Role</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-surface-100">
            {usersQuery.data.content.map((u) => (
              <tr key={u.id} className="hover:bg-white/40 transition-colors">
                <td className="px-4 py-3 font-semibold text-surface-800">
                  {u.firstName} {u.lastName}
                </td>
                <td className="px-4 py-3 text-surface-500">{u.email}</td>
                <td className="px-4 py-3">
                  <select
                    className="glass-input py-1.5 text-sm font-medium w-auto"
                    value={u.role}
                    onChange={(e) => updateRole.mutate({ id: u.id, role: e.target.value as Role })}
                  >
                    {ROLES.map((role) => (
                      <option key={role} value={role}>
                        {role}
                      </option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
