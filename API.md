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
| POST | `/auth/register` | Body includes `role`: `PRODUCER`, `CONTENT_MANAGER`, or `TEAM_MEMBER` |
| POST | `/auth/login` | |
| POST | `/auth/refresh` | Body: `{ refreshToken }` |
| POST | `/auth/logout` | Stateless — client discards tokens |
| GET | `/auth/me` | Requires token |

## Users (`/users`)

| Method | Path | Authorization |
|---|---|---|
| GET | `/users/{id}` | Self or Producer |
| PATCH | `/users/{id}` | Self or Producer |
| GET | `/users` | Producer only, paginated |
| PATCH | `/users/{id}/role` | Producer only |
| PATCH | `/users/{id}/enabled` | Producer only; cannot deactivate self |

## Teams (`/teams`)

| Method | Path | Authorization |
|---|---|---|
| POST | `/teams` | Producer only (creator becomes OWNER) |
| GET | `/teams` | Teams the current user belongs to |
| GET | `/teams/{id}` | Team member |
| PATCH | `/teams/{id}` | Producer team OWNER/MANAGER |
| DELETE | `/teams/{id}` | Producer team OWNER |
| GET | `/teams/{id}/members` | Team member |
| POST | `/teams/{id}/members` | Producer team OWNER/MANAGER |
| DELETE | `/teams/{id}/members/{userId}` | Producer team OWNER/MANAGER (cannot remove the OWNER) |
| PATCH | `/teams/{id}/members/{userId}/role` | Producer team OWNER |

## Projects (`/projects`)

| Method | Path | Authorization |
|---|---|---|
| POST | `/projects` | Producer team OWNER/MANAGER; requires unique code and start/end dates |
| GET | `/projects?teamId=&status=` | Team member |
| GET | `/projects/{id}` | Project member or team member |
| PATCH | `/projects/{id}` | Producer team OWNER/MANAGER |
| DELETE | `/projects/{id}` | Producer team OWNER/MANAGER |
| GET | `/projects/{id}/members` | Project member or team member |
| POST | `/projects/{id}/members` | Producer team OWNER/MANAGER (target must already be a team member) |
| DELETE | `/projects/{id}/members/{userId}` | Producer team OWNER/MANAGER |
| GET | `/projects/{id}/finance` | Producer or Content Manager with project access |
| PATCH | `/projects/{id}/budget` | Producer team OWNER/MANAGER |
| POST | `/projects/{id}/expenses` | Producer team OWNER/MANAGER |
| DELETE | `/projects/{id}/expenses/{expenseId}` | Producer team OWNER/MANAGER |

## Tasks (`/tasks`)

| Method | Path | Authorization |
|---|---|---|
| POST | `/tasks` | Producer team OWNER/MANAGER |
| GET | `/tasks?projectId=&status=&priority=&assigneeId=&deadlineFrom=&deadlineTo=&search=` | Project or team member |
| GET | `/tasks/board?projectId=` | Project or team member (grouped client-side by status) |
| GET | `/tasks/{id}` | Project or team member |
| PATCH | `/tasks/{id}` | Producer team OWNER/MANAGER |
| PATCH | `/tasks/{id}/status` | Producer can move adjacent columns; Content Manager can review; Team Member can update assigned tasks through `IN_REVIEW` |
| PATCH | `/tasks/{id}/assign` | Producer team OWNER/MANAGER only |
| DELETE | `/tasks/{id}` | Producer team OWNER/MANAGER |
| POST/DELETE | `/tasks/{id}/labels/{labelId}` | Same as PATCH `/tasks/{id}` |

## Labels (`/labels`)

| Method | Path | Authorization |
|---|---|---|
| POST | `/labels` | Producer |
| GET | `/labels` | Any authenticated user |

## Dashboard

| Method | Path | Authorization |
|---|---|---|
| GET | `/projects/{id}/dashboard` | Project member or team member |
| GET | `/dashboard/my-tasks` | Any authenticated user (their own assigned tasks) |
