import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useAuth } from '../../context/AuthContext'
import { userApi } from '../../api/endpoints'
import { Button, Card, Field, Input } from '../../components/ui'
import { extractErrorMessage } from '../../api/client'
import { User } from 'lucide-react'

export function ProfilePage() {
  const { user, refreshCurrentUser } = useAuth()
  const [firstName, setFirstName] = useState(user?.firstName ?? '')
  const [lastName, setLastName] = useState(user?.lastName ?? '')

  const updateProfile = useMutation({
    mutationFn: () => userApi.updateProfile(user!.id, { firstName, lastName }),
    onSuccess: async () => {
      toast.success('Profile updated')
      await refreshCurrentUser()
    },
    onError: (err) => toast.error(extractErrorMessage(err, 'Could not update your profile')),
  })

  if (!user) return null

  return (
    <div className="max-w-2xl space-y-6 animate-fade-in">
      <div>
        <h1 className="text-3xl font-bold text-surface-900 dark:text-surface-50 tracking-tight">Profile</h1>
        <p className="mt-1 text-sm text-surface-500 dark:text-surface-400">Manage your personal information</p>
      </div>

      <Card className="p-6">
        <div className="flex items-center gap-4 mb-6">
          <div className="p-3 rounded-xl bg-gradient-to-br from-brand-50 to-indigo-50 dark:from-brand-900/30 dark:to-indigo-900/30 text-brand-600 dark:text-brand-400">
            <User className="h-6 w-6" />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-surface-900 dark:text-surface-100">Personal Information</h2>
            <p className="text-sm text-surface-500 dark:text-surface-400">Update your profile details</p>
          </div>
        </div>

        <form
          className="space-y-5"
          onSubmit={(e) => {
            e.preventDefault()
            updateProfile.mutate()
          }}
        >
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="First name">
              <Input value={firstName} onChange={(e) => setFirstName(e.target.value)} maxLength={100} required />
            </Field>
            <Field label="Last name">
              <Input value={lastName} onChange={(e) => setLastName(e.target.value)} maxLength={100} required />
            </Field>
          </div>
          <Field label="Email">
            <Input value={user.email} disabled className="bg-surface-100/50 dark:bg-surface-800/50 text-surface-400 dark:text-surface-500" />
          </Field>
          <Field label="Role">
            <Input value={user.role} disabled className="bg-surface-100/50 dark:bg-surface-800/50 text-surface-400 dark:text-surface-500" />
          </Field>
          <div className="flex justify-end">
            <Button type="submit" loading={updateProfile.isPending}>
              Save changes
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
