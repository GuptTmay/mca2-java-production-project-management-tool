export type Role = 'ADMIN' | 'PROJECT_MANAGER' | 'DEVELOPER'
export type TeamRole = 'OWNER' | 'MANAGER' | 'MEMBER'
export type ProjectStatus = 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED'
export const PROJECT_STATUS_LABELS: Record<ProjectStatus, string> = {
  PLANNING: 'Planning',
  ACTIVE: 'Active',
  ON_HOLD: 'On Hold',
  COMPLETED: 'Completed',
  ARCHIVED: 'Archived',
}
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export type ActivityType = 'TASK_CREATED' | 'TASK_ASSIGNED' | 'TASK_STATUS_CHANGED' | 'TASK_PRIORITY_CHANGED' | 'LABEL_ADDED' | 'LABEL_REMOVED' | 'COMMENT_ADDED' | 'TASK_UPDATED'
export type NotificationType = 'TASK_ASSIGNED' | 'COMMENT_ADDED' | 'TASK_UPDATED'

export interface UserSummary {
  id: string
  email: string
  firstName: string
  lastName: string
  role: Role
  enabled: boolean
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: UserSummary
}

export interface Team {
  id: string
  name: string
  description?: string
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface TeamMember {
  id: string
  userId: string
  userEmail: string
  userFirstName: string
  userLastName: string
  teamRole: TeamRole
  joinedAt: string
}

export interface Project {
  id: string
  teamId: string
  name: string
  description?: string
  status: ProjectStatus
  startDate?: string
  endDate?: string
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface ProjectMember {
  id: string
  userId: string
  userEmail: string
  userFirstName: string
  userLastName: string
  addedAt: string
}

export interface Label {
  id: string
  name: string
  color: string
}

export interface Task {
  id: string
  projectId: string
  title: string
  description?: string
  status: TaskStatus
  priority: TaskPriority
  assigneeId?: string
  reporterId: string
  deadline?: string
  labels: Label[]
  version: number
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ProjectDashboard {
  projectId: string
  totalTasks: number
  completedTasks: number
  pendingTasks: number
  overdueTasks: number
  tasksByStatus: Record<TaskStatus, number>
  tasksByPriority: Record<TaskPriority, number>
  completionPercentage: number
}

export interface MyTasksSummary {
  totalAssignedTasks: number
  overdueTasks: number
  tasksByStatus: Record<TaskStatus, number>
  tasks: PageResponse<Task>
}

export interface Comment {
  id: string
  taskId: string
  userId: string
  userFirstName: string
  userLastName: string
  userEmail: string
  content: string
  createdAt: string
  updatedAt: string
}

export interface Activity {
  id: string
  taskId?: string
  projectId?: string
  actorId: string
  actorFirstName: string
  actorLastName: string
  actorEmail: string
  type: ActivityType
  description: string
  metadata?: string
  createdAt: string
}

export interface TaskHistory {
  id: string
  taskId: string
  projectId: string
  actorId: string
  actorName: string
  previousStatus?: TaskStatus
  newStatus?: TaskStatus
  changeReason?: string
  createdAt: string
}

export interface Notification {
  id: string
  userId: string
  type: NotificationType
  message: string
  taskId?: string
  read: boolean
  createdAt: string
}

export interface UnreadCountResponse {
  unreadCount: number
}

export const TASK_STATUSES: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE']
export const TASK_PRIORITIES: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'To Do',
  IN_PROGRESS: 'In Progress',
  IN_REVIEW: 'In Review',
  DONE: 'Done',
}
