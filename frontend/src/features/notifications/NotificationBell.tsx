import { useState, useEffect, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Bell, CheckCheck } from 'lucide-react'
import { notificationApi } from '../../api/endpoints'
import { ErrorBanner, Spinner, EmptyState } from '../../components/ui'
import { extractErrorMessage } from '../../api/client'
import { toast } from 'sonner'
import type { Notification } from '../../lib/types'

interface NotificationBellProps {
  open?: boolean
  onOpenChange?: (open: boolean) => void
}

export function NotificationBell({ open, onOpenChange }: NotificationBellProps) {
  const internalOpen = useState(false)[0]
  const isControlled = open !== undefined
  const isOpen = isControlled ? open : internalOpen
  const setOpen = isControlled ? onOpenChange! : () => {}

  const dropdownRef = useRef<HTMLDivElement>(null)
  const queryClient = useQueryClient()

  const unreadQuery = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: () => notificationApi.unreadCount(),
    refetchInterval: 30000,
  })

  const listQuery = useQuery({
    queryKey: ['notifications'],
    queryFn: () => notificationApi.list(0, 10),
    enabled: isOpen,
  })

  const markAsRead = useMutation({
    mutationFn: (id: string) => notificationApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  const markAllAsRead = useMutation({
    mutationFn: () => notificationApi.markAllAsRead(),
    onSuccess: () => {
      toast.success('All notifications marked as read')
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setOpen(false)
      }
    }
    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpen(false)
      }
    }
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
      document.addEventListener('keydown', handleEscape)
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [isOpen, setOpen])

  const unreadCount = unreadQuery.data?.unreadCount ?? 0

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setOpen(!isOpen)}
        className="relative rounded-xl p-2.5 text-surface-500 hover:bg-surface-100/60 hover:text-brand-700 transition-all duration-200"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-5 w-5 items-center justify-center rounded-full bg-rose-500 text-[10px] font-bold text-white shadow-lg">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 top-full z-50 mt-2 w-80 overflow-hidden rounded-2xl border border-surface-200/60 bg-white/95 backdrop-blur-xl shadow-glass-lg animate-scale-in">
          <div className="flex items-center justify-between border-b border-surface-200/60 px-4 py-3">
            <h3 className="text-sm font-bold text-surface-900">Notifications</h3>
            {unreadCount > 0 && (
              <button
                onClick={() => markAllAsRead.mutate()}
                className="flex items-center gap-1 text-xs text-brand-600 hover:text-brand-700 transition-colors"
              >
                <CheckCheck className="h-3.5 w-3.5" />
                Mark all read
              </button>
            )}
          </div>

          <div className="max-h-96 overflow-y-auto scrollbar-thin">
            {listQuery.isLoading ? (
              <div className="flex justify-center py-8">
                <Spinner />
              </div>
            ) : listQuery.isError ? (
              <ErrorBanner message={extractErrorMessage(listQuery.error, 'Could not load notifications')} />
            ) : listQuery.data?.content.length === 0 ? (
              <EmptyState title="No notifications" description="You're all caught up!" />
            ) : (
              <div className="divide-y divide-surface-200/60">
                {listQuery.data?.content.map((notification: Notification) => (
                  <div
                    key={notification.id}
                    className={`flex gap-3 p-3 transition-colors hover:bg-surface-50/80 ${
                      !notification.read ? 'bg-brand-50/40' : ''
                    }`}
                  >
                    <div className="flex-1 min-w-0">
                      <p className={`text-sm ${!notification.read ? 'font-semibold text-surface-900' : 'text-surface-700'}`}>
                        {notification.message}
                      </p>
                      <p className="text-xs text-surface-400 mt-1">
                        {new Date(notification.createdAt).toLocaleString()}
                      </p>
                    </div>
                    {!notification.read && (
                      <button
                        onClick={() => markAsRead.mutate(notification.id)}
                        className="flex-shrink-0 rounded-lg p-1.5 text-surface-400 hover:text-brand-700 hover:bg-brand-50 transition-colors"
                        title="Mark as read"
                      >
                        <CheckCheck className="h-4 w-4" />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
