import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { projectApi } from '../../api/endpoints'
import { Button, Card, ErrorBanner, Field, Input, Spinner } from '../../components/ui'
import { extractErrorMessage } from '../../api/client'
import { SectionHeader } from '../../components/PageHeader'

interface ProjectFinanceSectionProps {
  projectId: string
  canManage: boolean
}

export function ProjectFinanceSection({ projectId, canManage }: ProjectFinanceSectionProps) {
  const queryClient = useQueryClient()
  const [budgetAmount, setBudgetAmount] = useState('')
  const [currencyCode, setCurrencyCode] = useState('USD')
  const [description, setDescription] = useState('')
  const [category, setCategory] = useState('')
  const [amount, setAmount] = useState('')
  const [spentOn, setSpentOn] = useState(new Date().toISOString().slice(0, 10))

  const financeQuery = useQuery({
    queryKey: ['projects', projectId, 'finance'],
    queryFn: () => projectApi.finance(projectId),
  })

  useEffect(() => {
    if (!financeQuery.data) return
    setBudgetAmount(String(financeQuery.data.budgetAmount))
    setCurrencyCode(financeQuery.data.currencyCode)
  }, [financeQuery.data])

  function refreshFinance() {
    queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'finance'] })
  }

  const updateBudget = useMutation({
    mutationFn: () => projectApi.updateBudget(projectId, {
      budgetAmount: Number(budgetAmount),
      currencyCode: currencyCode.trim().toUpperCase(),
    }),
    onSuccess: () => {
      toast.success('Project budget updated')
      refreshFinance()
    },
    onError: (error) => toast.error(extractErrorMessage(error, 'Could not update the budget')),
  })

  const addExpense = useMutation({
    mutationFn: () => projectApi.addExpense(projectId, {
      description: description.trim(),
      category: category.trim() || undefined,
      amount: Number(amount),
      spentOn,
    }),
    onSuccess: () => {
      toast.success('Expense recorded')
      setDescription('')
      setCategory('')
      setAmount('')
      refreshFinance()
    },
    onError: (error) => toast.error(extractErrorMessage(error, 'Could not record the expense')),
  })

  const deleteExpense = useMutation({
    mutationFn: (expenseId: string) => projectApi.removeExpense(projectId, expenseId),
    onSuccess: () => {
      toast.success('Expense removed')
      refreshFinance()
    },
    onError: (error) => toast.error(extractErrorMessage(error, 'Could not remove the expense')),
  })

  if (financeQuery.isLoading) return <Spinner />
  if (financeQuery.isError || !financeQuery.data) {
    return <ErrorBanner message={extractErrorMessage(financeQuery.error, 'Could not load project finances')} />
  }

  const finance = financeQuery.data
  const formatMoney = (value: number) => new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: finance.currencyCode,
  }).format(value)

  return (
    <section className="space-y-4">
      <SectionHeader title="Budget & expenses" description="Track project spending against the approved budget." />
      <Card className="space-y-5 p-5 sm:p-6">
        <div className="grid gap-3 sm:grid-cols-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-surface-400">Budget</p>
            <p className="mt-1 text-xl font-bold text-surface-900">{formatMoney(finance.budgetAmount)}</p>
          </div>
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-surface-400">Expenses</p>
            <p className="mt-1 text-xl font-bold text-surface-900">{formatMoney(finance.totalExpenses)}</p>
          </div>
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-surface-400">Remaining</p>
            <p className={`mt-1 text-xl font-bold ${finance.remainingBudget < 0 ? 'text-rose-600' : 'text-emerald-700'}`}>
              {formatMoney(finance.remainingBudget)}
            </p>
          </div>
        </div>

        {canManage && (
          <form
            className="grid gap-3 border-t border-surface-200/60 pt-4 sm:grid-cols-[1fr_120px_auto] sm:items-end"
            onSubmit={(event) => {
              event.preventDefault()
              updateBudget.mutate()
            }}
          >
            <Field label="Project budget">
              <Input type="number" min="0" step="0.01" value={budgetAmount} onChange={(event) => setBudgetAmount(event.target.value)} required />
            </Field>
            <Field label="Currency">
              <Input value={currencyCode} onChange={(event) => setCurrencyCode(event.target.value)} maxLength={3} minLength={3} required />
            </Field>
            <Button type="submit" loading={updateBudget.isPending}>Save budget</Button>
          </form>
        )}

        {canManage && (
          <form
            className="grid gap-3 border-t border-surface-200/60 pt-4 sm:grid-cols-2"
            onSubmit={(event) => {
              event.preventDefault()
              addExpense.mutate()
            }}
          >
            <Field label="Expense description">
              <Input value={description} onChange={(event) => setDescription(event.target.value)} maxLength={250} required />
            </Field>
            <Field label="Category (optional)">
              <Input value={category} onChange={(event) => setCategory(event.target.value)} maxLength={100} />
            </Field>
            <Field label={`Amount (${finance.currencyCode})`}>
              <Input type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} required />
            </Field>
            <Field label="Expense date">
              <Input type="date" value={spentOn} onChange={(event) => setSpentOn(event.target.value)} required />
            </Field>
            <div className="sm:col-span-2">
              <Button type="submit" loading={addExpense.isPending}>Record expense</Button>
            </div>
          </form>
        )}

        <div className="border-t border-surface-200/60 pt-4">
          <h3 className="mb-3 text-sm font-semibold text-surface-800">Expense history</h3>
          {finance.expenses.length === 0 ? (
            <p className="text-sm text-surface-500">No expenses recorded for this project.</p>
          ) : (
            <ul className="divide-y divide-surface-100">
              {finance.expenses.map((expense) => (
                <li key={expense.id} className="flex items-center justify-between gap-4 py-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-surface-800">{expense.description}</p>
                    <p className="text-xs text-surface-500">
                      {[expense.category, new Date(`${expense.spentOn}T00:00:00`).toLocaleDateString()].filter(Boolean).join(' · ')}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-3">
                    <span className="text-sm font-semibold text-surface-800">{formatMoney(expense.amount)}</span>
                    {canManage && (
                      <Button
                        type="button"
                        variant="secondary"
                        disabled={deleteExpense.isPending}
                        onClick={() => deleteExpense.mutate(expense.id)}
                        aria-label={`Remove expense ${expense.description}`}
                      >
                        Remove
                      </Button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </Card>
    </section>
  )
}
