import clsx from 'clsx'
import type { HTMLAttributes } from 'react'

export function Avatar({
  name,
  email,
  src,
  size = 'md',
  className,
}: {
  name?: string
  email?: string
  src?: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
} & HTMLAttributes<HTMLDivElement>) {
  const initials = name
    ? name
        .split(' ')
        .map((n) => n[0])
        .join('')
        .toUpperCase()
        .slice(0, 2)
    : email
      ? email.slice(0, 2).toUpperCase()
      : '?'

  const sizes = {
    sm: 'h-7 w-7 text-xs',
    md: 'h-9 w-9 text-sm',
    lg: 'h-11 w-11 text-base',
    xl: 'h-14 w-14 text-lg',
  }

  const colors = [
    'bg-brand-100 text-brand-700',
    'bg-indigo-100 text-indigo-700',
    'bg-cyan-100 text-cyan-700',
    'bg-emerald-100 text-emerald-700',
    'bg-amber-100 text-amber-700',
    'bg-rose-100 text-rose-700',
  ]

  const colorIndex = name ? name.charCodeAt(0) % colors.length : 0

  return (
    <div
      className={clsx(
        'inline-flex items-center justify-center rounded-full font-semibold ring-2 ring-white shadow-sm',
        sizes[size],
        colors[colorIndex],
        className,
      )}
      title={name || email}
    >
      {src ? (
        <img src={src} alt={name || email} className="h-full w-full rounded-full object-cover" />
      ) : (
        initials
      )}
    </div>
  )
}
