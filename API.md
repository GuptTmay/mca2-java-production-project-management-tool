# API Reference

Base path: `/api/v1`. Full interactive docs (with request/response schemas) are served at `/swagger-ui.html` once the backend is running. This file is a quick-reference index.

All list endpoints return:
```json
{ "content": [...], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }
```

All errors return an RFC 7807 `ProblemDetail`:
```json
{ "type": "about:blank", "title": "...", "status": 404, "detail": "...", "instance": "/api/v1/..." }
```

## Auth (`/auth`) — no token required

| Method | Path | Notes |
|---|---|---|
| POST | `/auth/register` | Every account starts as `DEVELOPER` |
| POST | `/auth/login` | |
| POST | `/auth/refresh` | Body: `{ refreshToken }` |
| POST | `/auth/logout` | Stateless — client discards tokens |
| GET | `/auth/me` | Requires token |

## Users (`/users`)

| Method | Path | Authorization |
|---|---|---|
| GET | `/users/{id}` | Self or ADMIN |
| PATCH | `/users/{id}` | Self or ADMIN |
| GET | `/users` | ADMIN only, paginated |
| PATCH | `/users/{id}/role` | ADMIN only |

## Teams (`/teams`)

| Method | Path | Authorization |
|---|---|---|
| POST | `/teams` | Any authenticated user (creator becomes OWNER) |
| GET | `/teams` | Own teams, or all teams if ADMIN |
| GET | `/teams/{id}` | Team member or ADMIN |
| PATCH | `/teams/{id}` | OWNER/MANAGER or ADMIN |
| DELETE | `/teams/{id}` | OWNER or ADMIN |
| GET | `/teams/{id}/members` | Team member or ADMIN |
| POST | `/teams/{id}/members` | OWNER/MANAGER or ADMIN |
| DELETE | `/teams/{id}/members/{userId}` | OWNER/MANAGER or ADMIN (cannot remove the last OWNER) |
| PATCH | `/teams/{id}/members/{userId}/role` | OWNER or ADMIN (cannot demote the last OWNER) |

## Projects (`/projects`)

| Method | Path | Authorization |
|---|---|---|
| POST | `/projects` | Team OWNER/MANAGER or ADMIN |
| GET | `/projects?teamId=&status=` | Team member or ADMIN |
| GET | `/projects/{id}` | Project member, team OWNER/MANAGER, or ADMIN |
| PATCH | `/projects/{id}` | Team OWNER/MANAGER or ADMIN |
| DELETE | `/projects/{id}` | Team OWNER/MANAGER or ADMIN |
| GET | `/projects/{id}/members` | Project member, team OWNER/MANAGER, or ADMIN |
| POST | `/projects/{id}/members` | Team OWNER/MANAGER or ADMIN (target must already be a team member) |
| DELETE | `/projects/{id}/members/{userId}` | Team OWNER/MANAGER or ADMIN |

## Tasks (`/tasks`)

| Method | Path | Authorization |
|---|---|---|
| POST | `/tasks` | Project member, team manager, or ADMIN |
| GET | `/tasks?projectId=&status=&priority=&assigneeId=&deadlineFrom=&deadlineTo=&search=` | Same as above |
| GET | `/tasks/board?projectId=` | Same as above (grouped client-side by status) |
| GET | `/tasks/{id}` | Same as above |
| PATCH | `/tasks/{id}` | The task's assignee, team OWNER/MANAGER, or ADMIN |
| PATCH | `/tasks/{id}/status` | Any project member, team manager, or ADMIN — validates the column move is adjacent |
| PATCH | `/tasks/{id}/assign` | Team OWNER/MANAGER or ADMIN only (not the current assignee) |
| DELETE | `/tasks/{id}` | Team OWNER/MANAGER or ADMIN |
| POST/DELETE | `/tasks/{id}/labels/{labelId}` | Same as PATCH `/tasks/{id}` |

## Labels (`/labels`)

| Method | Path | Authorization |
|---|---|---|
| POST | `/labels` | ADMIN or PROJECT_MANAGER |
| GET | `/labels` | Any authenticated user |

## Dashboard

| Method | Path | Authorization |
|---|---|---|
| GET | `/projects/{id}/dashboard` | Project member, team manager, or ADMIN |
| GET | `/dashboard/my-tasks` | Any authenticated user (their own assigned tasks) |
