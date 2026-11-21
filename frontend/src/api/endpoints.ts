import { apiClient, setTokens } from './client'
import type {
  Activity,
  AuthResponse,
  Comment,
  Label,
  MyTasksSummary,
  Notification,
  PageResponse,
  Project,
  ProjectDashboard,
  ProjectMember,
  ProjectStatus,
  Task,
  TaskHistory,
  TaskPriority,
  TaskStatus,
  Team,
  TeamMember,
  TeamRole,
  UnreadCountResponse,
  UserSummary,
} from '../lib/types'

export const authApi = {
  async register(data: { email: string; password: string; firstName: string; lastName: string }) {
    const res = await apiClient.post<AuthResponse>('/auth/register', data)
    setTokens(res.data)
    return res.data
  },
  async login(data: { email: string; password: string }) {
    const res = await apiClient.post<AuthResponse>('/auth/login', data)
    setTokens(res.data)
    return res.data
  },
  async logout() {
    try {
      await apiClient.post('/auth/logout')
    } finally {
      setTokens(null)
    }
  },
  async me() {
    const res = await apiClient.get<UserSummary>('/auth/me')
    return res.data
  },
}

export const userApi = {
  async getById(id: string) {
    return (await apiClient.get<UserSummary>(`/users/${id}`)).data
  },
  async updateProfile(id: string, data: { firstName: string; lastName: string }) {
    return (await apiClient.patch<UserSummary>(`/users/${id}`, data)).data
  },
  async list(page = 0, size = 20) {
    return (await apiClient.get<PageResponse<UserSummary>>('/users', { params: { page, size } })).data
  },
  async updateRole(id: string, role: string) {
    return (await apiClient.patch<UserSummary>(`/users/${id}/role`, { role })).data
  },
}

export const teamApi = {
  async create(data: { name: string; description?: string }) {
    return (await apiClient.post<Team>('/teams', data)).data
  },
  async list(page = 0, size = 50) {
    return (await apiClient.get<PageResponse<Team>>('/teams', { params: { page, size } })).data
  },
  async getById(id: string) {
    return (await apiClient.get<Team>(`/teams/${id}`)).data
  },
  async update(id: string, data: { name: string; description?: string }) {
    return (await apiClient.patch<Team>(`/teams/${id}`, data)).data
  },
  async remove(id: string) {
    await apiClient.delete(`/teams/${id}`)
  },
  async listMembers(id: string) {
    return (await apiClient.get<TeamMember[]>(`/teams/${id}/members`)).data
  },
  async addMember(id: string, email: string, teamRole: TeamRole) {
    return (await apiClient.post<TeamMember>(`/teams/${id}/members`, { email, teamRole })).data
  },
  async removeMember(id: string, userId: string) {
    await apiClient.delete(`/teams/${id}/members/${userId}`)
  },
  async updateMemberRole(id: string, userId: string, teamRole: TeamRole) {
    return (await apiClient.patch<TeamMember>(`/teams/${id}/members/${userId}/role`, { teamRole })).data
  },
}

export const projectApi = {
  async create(data: {
    teamId: string
    name: string
    description?: string
    status?: ProjectStatus
    startDate?: string
    endDate?: string
  }) {
    return (await apiClient.post<Project>('/projects', data)).data
  },
  async list(teamId: string, status?: ProjectStatus, page = 0, size = 50) {
    return (
      await apiClient.get<PageResponse<Project>>('/projects', { params: { teamId, status, page, size } })
    ).data
  },
  async getById(id: string) {
    return (await apiClient.get<Project>(`/projects/${id}`)).data
  },
  async update(
    id: string,
    data: { name: string; description?: string; status: ProjectStatus; startDate?: string; endDate?: string },
  ) {
    return (await apiClient.patch<Project>(`/projects/${id}`, data)).data
  },
  async remove(id: string) {
    await apiClient.delete(`/projects/${id}`)
  },
  async listMembers(id: string) {
    return (await apiClient.get<ProjectMember[]>(`/projects/${id}/members`)).data
  },
  async addMember(id: string, email: string) {
    return (await apiClient.post<ProjectMember>(`/projects/${id}/members`, { email })).data
  },
  async removeMember(id: string, userId: string) {
    await apiClient.delete(`/projects/${id}/members/${userId}`)
  },
}

export const taskApi = {
  async create(data: {
    projectId: string
    title: string
    description?: string
    priority?: TaskPriority
    assigneeId?: string
    deadline?: string
  }) {
    return (await apiClient.post<Task>('/tasks', data)).data
  },
  async board(projectId: string, page = 0, size = 200) {
    return (await apiClient.get<PageResponse<Task>>('/tasks/board', { params: { projectId, page, size } })).data
  },
  async getById(id: string) {
    return (await apiClient.get<Task>(`/tasks/${id}`)).data
  },
  async update(
    id: string,
    data: { title: string; description?: string; priority: TaskPriority; deadline?: string },
  ) {
    return (await apiClient.patch<Task>(`/tasks/${id}`, data)).data
  },
  async updateStatus(id: string, status: TaskStatus) {
    return (await apiClient.patch<Task>(`/tasks/${id}/status`, { status })).data
  },
  async assign(id: string, assigneeId: string | null) {
    return (await apiClient.patch<Task>(`/tasks/${id}/assign`, { assigneeId })).data
  },
  async remove(id: string) {
    await apiClient.delete(`/tasks/${id}`)
  },
  async addLabel(id: string, labelId: string) {
    return (await apiClient.post<Task>(`/tasks/${id}/labels/${labelId}`)).data
  },
  async removeLabel(id: string, labelId: string) {
    return (await apiClient.delete<Task>(`/tasks/${id}/labels/${labelId}`)).data
  },
  async history(taskId: string) {
    return (await apiClient.get<TaskHistory[]>(`/tasks/${taskId}/history`)).data
  },
}

export const labelApi = {
  async list() {
    return (await apiClient.get<Label[]>('/labels')).data
  },
  async create(name: string, color?: string) {
    return (await apiClient.post<Label>('/labels', { name, color })).data
  },
}

export const dashboardApi = {
  async project(projectId: string) {
    return (await apiClient.get<ProjectDashboard>(`/projects/${projectId}/dashboard`)).data
  },
  async myTasks(page = 0, size = 10) {
    return (await apiClient.get<MyTasksSummary>('/dashboard/my-tasks', { params: { page, size } })).data
  },
}

export const commentApi = {
  async list(taskId: string) {
    return (await apiClient.get<Comment[]>(`/tasks/${taskId}/comments`)).data
  },
  async create(taskId: string, content: string) {
    return (await apiClient.post<Comment>(`/tasks/${taskId}/comments`, { content })).data
  },
  async update(taskId: string, commentId: string, content: string) {
    return (await apiClient.patch<Comment>(`/tasks/${taskId}/comments/${commentId}`, { content })).data
  },
  async remove(taskId: string, commentId: string) {
    await apiClient.delete(`/tasks/${taskId}/comments/${commentId}`)
  },
}

export const activityApi = {
  async listForTask(taskId: string) {
    return (await apiClient.get<Activity[]>(`/activities/tasks/${taskId}`)).data
  },
  async listForProject(projectId: string) {
    return (await apiClient.get<Activity[]>(`/activities/projects/${projectId}`)).data
  },
}

export const notificationApi = {
  async list(page = 0, size = 20) {
    return (await apiClient.get<PageResponse<Notification>>('/notifications', { params: { page, size } })).data
  },
  async unreadCount() {
    return (await apiClient.get<UnreadCountResponse>('/notifications/unread-count')).data
  },
  async markAsRead(id: string) {
    return (await apiClient.patch<Notification>(`/notifications/${id}/read`, {})).data
  },
  async markAllAsRead() {
    await apiClient.post('/notifications/mark-all-read')
  },
}
