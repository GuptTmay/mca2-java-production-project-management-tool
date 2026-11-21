import { useState, useRef, useEffect } from 'react'
import { NavLink, useNavigate, useLocation, matchPath } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import { useQuery } from '@tanstack/react-query'
import { teamApi, projectApi } from '../api/endpoints'
import { Outlet } from 'react-router-dom'
import type { TeamRole } from '../lib/types'
import { NotificationBell } from '../features/notifications/NotificationBell'
import {
  LayoutDashboard,
  Users,
  CheckSquare,
  Search,
  Plus,
  ChevronDown,
  LogOut,
  User,
  Moon,
  Sun,
  Menu,
} from 'lucide-react'
import clsx from 'clsx'
import { Avatar } from './Avatar'

const NAV_ITEMS = [
  { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/teams', label: 'Teams', icon: Users },
  { path: '/my-tasks', label: 'My Tasks', icon: CheckSquare },
]

export default function AppLayout() {
  const { user, logout } = useAuth()
  const { theme, toggle } = useTheme()
  const navigate = useNavigate()
  const location = useLocation()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [showUserMenu, setShowUserMenu] = useState(false)
  const [showTeamMenu, setShowTeamMenu] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const userMenuRef = useRef<HTMLDivElement>(null)
  const teamMenuRef = useRef<HTMLDivElement>(null)

  const teamsQuery = useQuery({
    queryKey: ['teams'],
    queryFn: () => teamApi.list(),
  })

  // Determine active team from route or fallback to first team
  const activeTeamId = (() => {
    const teamMatch = matchPath('/teams/:teamId', location.pathname)
    if (teamMatch?.params.teamId) return teamMatch.params.teamId
    return teamsQuery.data?.content?.[0]?.id
  })()

  const activeTeam = teamsQuery.data?.content?.find(t => t.id === activeTeamId) ?? teamsQuery.data?.content?.[0]

  // Fetch members for the active team to check current user's role
  const membersQuery = useQuery({
    queryKey: ['teams', activeTeamId, 'members'],
    queryFn: () => teamApi.listMembers(activeTeamId!),
    enabled: !!activeTeamId,
  })

  const currentUserTeamRole: TeamRole | undefined = membersQuery.data?.find(m => m.userId === user?.id)?.teamRole
  const canCreateTask = currentUserTeamRole === 'OWNER' || currentUserTeamRole === 'MANAGER'

  const projectsQuery = useQuery({
    queryKey: ['projects', activeTeam?.id],
    queryFn: () => (activeTeam?.id ? projectApi.list(activeTeam.id) : Promise.resolve({ content: [] } as any)),
    enabled: !!activeTeam?.id,
  })

  const firstProject = projectsQuery.data?.content?.[0]

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
        setShowUserMenu(false)
      }
      if (teamMenuRef.current && !teamMenuRef.current.contains(event.target as Node)) {
        setShowTeamMenu(false)
      }
    }
    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setShowUserMenu(false)
        setShowTeamMenu(false)
        setNotificationsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleEscape)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [])

  return (
    <div className="flex h-screen overflow-hidden bg-surface-50/50 dark:bg-surface-950">
      {/* Mobile sidebar overlay */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-40 bg-surface-950/30 backdrop-blur-sm lg:hidden" onClick={() => setSidebarOpen(false)} />
      )}

      {/* Sidebar */}
      <aside
        className={clsx(
          'glass-sidebar fixed inset-y-0 left-0 z-50 flex flex-col transition-transform duration-300 ease-in-out lg:relative lg:translate-x-0',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex h-full w-64 flex-col">
          {/* Logo */}
          <div className="flex items-center gap-3 px-6 py-5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-brand-600 to-indigo-600 text-white shadow-lg shadow-brand-600/20">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5">
                <rect x="3" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="14" width="7" height="7" rx="1" />
                <rect x="3" y="14" width="7" height="7" rx="1" />
              </svg>
            </div>
            <div>
              <h1 className="text-lg font-bold text-surface-900 dark:text-surface-100">TaskFlow</h1>
              <p className="text-xs text-surface-400">Project Management</p>
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex-1 space-y-1 px-4 py-2">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                onClick={() => setSidebarOpen(false)}
                className={({ isActive }) =>
                  clsx('nav-item', isActive ? 'nav-item-active' : 'nav-item-inactive')
                }
              >
                <item.icon className="h-4.5 w-4.5 flex-shrink-0" />
                <span>{item.label}</span>
              </NavLink>
            ))}
          </nav>

          {/* User Section */}
          <div className="border-t border-surface-200/60 dark:border-surface-700/60 p-4">
            <div className="flex items-center gap-3 rounded-xl p-2 hover:bg-surface-100/40 dark:hover:bg-surface-800/40 transition-colors">
              <Avatar name={user ? `${user.firstName} ${user.lastName}` : undefined} email={user?.email} size="sm" />
              <div className="flex-1 min-w-0">
                <p className="truncate text-sm font-semibold text-surface-900 dark:text-surface-100">
                  {user?.firstName} {user?.lastName}
                </p>
                <p className="truncate text-xs text-surface-400">{user?.email}</p>
              </div>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex min-w-0 flex-1 flex-col">
        {/* Top Navigation */}
        <header className="glass border-b border-surface-200/60 dark:border-surface-700/60 px-4 py-3 relative z-20">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="rounded-lg p-2 text-surface-500 hover:bg-surface-100/60 hover:text-brand-700 transition-colors lg:hidden"
              >
                <Menu className="h-5 w-5" />
              </button>

              {/* Team Selector */}
              {teamsQuery.data && (
                <div className="relative hidden md:block" ref={teamMenuRef}>
                  <button
                    onClick={() => setShowTeamMenu(!showTeamMenu)}
                    className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-surface-700 dark:text-surface-200 hover:bg-surface-100/60 dark:hover:bg-surface-800/60 transition-colors"
                  >
                    <Users className="h-4 w-4 text-surface-400" />
                    <span className="max-w-[200px] truncate">{activeTeam?.name || 'Select Team'}</span>
                    <ChevronDown className="h-4 w-4 text-surface-400" />
                  </button>
                  {showTeamMenu && (
                    <div className="absolute left-0 top-full z-50 mt-1 w-64 rounded-xl border border-surface-200/60 dark:border-surface-700/60 bg-white/95 dark:bg-surface-900/95 backdrop-blur-xl shadow-lg py-1">
                      {teamsQuery.data.content?.map((team) => (
                        <NavLink
                          key={team.id}
                          to={`/teams/${team.id}`}
                          className={({ isActive }) =>
                            clsx(
                              'flex items-center gap-3 px-4 py-2.5 text-sm transition-colors',
                              isActive ? 'bg-brand-50 dark:bg-brand-900/30 text-brand-700 dark:text-brand-300' : 'text-surface-700 dark:text-surface-200 hover:bg-surface-100/60 dark:hover:bg-surface-800/60',
                            )
                          }
                          onClick={() => setShowTeamMenu(false)}
                        >
                          <Users className="h-4 w-4 flex-shrink-0 text-surface-400" />
                          <span className="truncate">{team.name}</span>
                        </NavLink>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>

            <div className="flex items-center gap-2">
              {/* Search */}
              <div className="relative hidden md:block">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-surface-400" />
                <input
                  type="text"
                  placeholder="Search tasks, projects..."
                  className="glass-input w-64 pl-9 py-2 text-sm"
                />
              </div>

              {/* Create Button */}
              {canCreateTask && (
                <button
                  onClick={() => {
                    const projectMatch = matchPath('/projects/:projectId', location.pathname)
                    if (projectMatch?.params.projectId) {
                      window.dispatchEvent(new Event('open-task-drawer'))
                    } else if (firstProject?.id) {
                      navigate(`/projects/${firstProject.id}?newTask=true`)
                    } else {
                      navigate('/teams')
                    }
                  }}
                  className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-brand-600 to-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-lg shadow-brand-600/20 hover:shadow-xl hover:shadow-brand-600/30 hover:from-brand-700 hover:to-indigo-700 transition-all active:scale-[0.98]"
                >
                  <Plus className="h-4 w-4" />
                  <span className="hidden sm:inline">New Task</span>
                </button>
              )}

              {/* Notifications */}
              <NotificationBell
                open={notificationsOpen}
                onOpenChange={(open) => {
                  setNotificationsOpen(open)
                  if (open) setShowUserMenu(false)
                }}
              />

              {/* Theme Toggle */}
              <button
                onClick={toggle}
                className="rounded-lg p-2 text-surface-500 hover:bg-surface-100/60 hover:text-brand-700 transition-colors"
              >
                {theme === 'dark' ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
              </button>

              {/* User Menu */}
              <div className="relative hidden sm:block" ref={userMenuRef}>
                <button
                  onClick={() => {
                    setShowUserMenu(!showUserMenu)
                    setNotificationsOpen(false)
                  }}
                  className="flex items-center gap-2 rounded-lg p-1.5 hover:bg-surface-100/60 transition-colors"
                >
                  <Avatar name={user ? `${user.firstName} ${user.lastName}` : undefined} email={user?.email} size="sm" />
                  <ChevronDown className="h-4 w-4 text-surface-400" />
                </button>
                {showUserMenu && (
                  <div className="absolute right-0 top-full z-50 mt-1 w-56 rounded-xl border border-surface-200/60 dark:border-surface-700/60 bg-white/95 dark:bg-surface-900/95 backdrop-blur-xl shadow-lg py-1">
                    <div className="px-4 py-3 border-b border-surface-200/60 dark:border-surface-700/60">
                      <p className="text-sm font-semibold text-surface-900 dark:text-surface-100">
                        {user?.firstName} {user?.lastName}
                      </p>
                      <p className="text-xs text-surface-400">{user?.email}</p>
                    </div>
                    <NavLink
                      to="/profile"
                      className="flex items-center gap-3 px-4 py-2.5 text-sm text-surface-700 dark:text-surface-200 hover:bg-surface-100/60 dark:hover:bg-surface-800/60 transition-colors"
                      onClick={() => setShowUserMenu(false)}
                    >
                      <User className="h-4 w-4 text-surface-400" />
                      Profile
                    </NavLink>
                    <div className="border-t border-surface-200/60 dark:border-surface-700/60 mt-1 pt-1">
                      <button
                        onClick={() => {
                          setShowUserMenu(false)
                          logout()
                        }}
                        className="flex w-full items-center gap-3 px-4 py-2.5 text-sm text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-900/20 transition-colors"
                      >
                        <LogOut className="h-4 w-4" />
                        Log out
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="min-w-0 flex-1 overflow-y-auto scrollbar-thin">
          <div className="mx-auto w-full min-w-0 max-w-7xl p-4 lg:p-8"><Outlet /></div>
        </main>
      </div>
    </div>
  )
}
