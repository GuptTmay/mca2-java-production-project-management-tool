import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { KanbanSquare, Lock, Mail } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { Button, Card, ErrorBanner, Field, Input } from '../../components/ui'
import { extractErrorMessage } from '../../api/client'

const schema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})

type FormValues = z.infer<typeof schema>

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  async function onSubmit(values: FormValues) {
    setServerError(null)
    try {
      await login(values.email, values.password)
      navigate('/dashboard')
    } catch (error) {
      setServerError(extractErrorMessage(error, 'Invalid email or password'))
    }
  }

  return (
    <div className="flex min-h-screen overflow-hidden">
      <div className="hidden lg:flex lg:w-1/2 relative overflow-hidden bg-gradient-to-br from-brand-950 via-brand-900 to-indigo-950 items-center justify-center">
        <div className="absolute inset-0">
          <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-brand-400/20 rounded-full blur-3xl animate-float" />
          <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-cyan-400/15 rounded-full blur-3xl animate-float" style={{ animationDelay: '3s' }} />
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-brand-500/10 rounded-full blur-3xl" />
        </div>

        <div className="max-w-md px-8 relative z-10">
          <div className="flex items-center gap-3 mb-12">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-white/10 backdrop-blur-md border border-white/20 shadow-glass">
              <KanbanSquare className="h-7 w-7 text-white" />
            </div>
            <span className="text-3xl font-bold text-white tracking-tight">TaskFlow</span>
          </div>
          <h2 className="text-4xl font-bold text-white mb-4 leading-tight text-balance">
            Manage work<br />with clarity
          </h2>
          <p className="text-brand-200/80 text-lg leading-relaxed text-balance">
            Organize projects, track tasks, and collaborate with your team in a workspace designed for focus.
          </p>
        </div>
      </div>

      <div className="flex flex-1 items-center justify-center bg-surface-50/50 px-4 py-12 relative">
        <div className="absolute inset-0 bg-gradient-to-br from-brand-50/30 via-transparent to-cyan-50/20 pointer-events-none" />
        <Card className="w-full max-w-md shadow-glass animate-slide-up" glass>
          <div className="p-8">
            <div className="mb-8 lg:hidden flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-brand-600 to-indigo-600 shadow-lg shadow-brand-600/20">
                <KanbanSquare className="h-5 w-5 text-white" />
              </div>
              <span className="text-2xl font-bold text-surface-900 tracking-tight gradient-text">TaskFlow</span>
            </div>

            <div className="mb-8">
              <h1 className="text-2xl font-bold text-surface-900 mb-1">Welcome back</h1>
              <p className="text-sm text-surface-500">Sign in to your account to continue</p>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
              {serverError && <ErrorBanner message={serverError} />}
              <Field label="Email" error={errors.email?.message}>
                <div className="relative">
                  <Mail className="absolute left-3.5 top-3 h-4 w-4 text-surface-400" />
                  <Input type="email" autoComplete="email" {...register('email')} className="pl-10" />
                </div>
              </Field>
              <Field label="Password" error={errors.password?.message}>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-3 h-4 w-4 text-surface-400" />
                  <Input type="password" autoComplete="current-password" {...register('password')} className="pl-10" />
                </div>
              </Field>
              <Button type="submit" className="w-full" loading={isSubmitting}>
                Sign in
              </Button>
            </form>

            <p className="mt-8 text-center text-sm text-surface-500">
              Don&apos;t have an account?{' '}
              <Link to="/register" className="font-semibold text-brand-600 hover:text-brand-700 transition-colors">
                Create one
              </Link>
            </p>
          </div>
        </Card>
      </div>
    </div>
  )
}
