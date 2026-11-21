# Architecture

## 1. System architecture

```mermaid
flowchart TB
    subgraph Client["Browser"]
        SPA["React SPA<br/>(React Router, TanStack Query, dnd-kit)"]
    end

    subgraph Backend["Spring Boot (Docker container)"]
        SEC["Security Filter Chain<br/>(JWT)"]
        CTRL["Controllers"]
        SVC["Services<br/>(+ TeamAccessGuard / ProjectAccessGuard)"]
        REPO["Spring Data JPA Repositories"]
        EXC["GlobalExceptionHandler<br/>(RFC 7807)"]
    end

    DB[("PostgreSQL<br/>(Flyway-managed schema)")]

    SPA -- "HTTPS/JSON + Bearer JWT" --> SEC
    SEC --> CTRL --> SVC --> REPO --> DB
    CTRL -.-> EXC
```

Layered monolith, package-by-feature (`auth`, `user`, `team`, `project`, `task`, `dashboard`). No microservices, no message broker, no Kubernetes — deliberately, per the agreed scope: a single well-architected service demonstrates more engineering judgment for this project's purpose than distributed-systems machinery would.

## 2. Entity-relationship diagram

```mermaid
erDiagram
    USERS ||--o{ TEAM_MEMBERS : has
    TEAMS ||--o{ TEAM_MEMBERS : has
    TEAMS ||--o{ PROJECTS : owns
    PROJECTS ||--o{ PROJECT_MEMBERS : has
    USERS ||--o{ PROJECT_MEMBERS : has
    PROJECTS ||--o{ TASKS : contains
    PROJECTS ||--o{ PROJECT_EXPENSES : tracks
    USERS ||--o{ TASKS : "assigned to"
    USERS ||--o{ TASKS : "reported by"
    TASKS }o--o{ LABELS : tagged

    USERS {
        uuid id PK
        string email
        string password_hash
        string role
    }
    TEAMS {
        uuid id PK
        uuid created_by FK
        string name
    }
    TEAM_MEMBERS {
        uuid id PK
        uuid team_id FK
        uuid user_id FK
        string team_role
    }
    PROJECTS {
        uuid id PK
        uuid team_id FK
        string code UK
        decimal budget_amount
        string currency_code
        string status
    }
    PROJECT_EXPENSES {
        uuid id PK
        uuid project_id FK
        uuid created_by FK
        decimal amount
        date spent_on
    }
    PROJECT_MEMBERS {
        uuid id PK
        uuid project_id FK
        uuid user_id FK
    }
    TASKS {
        uuid id PK
        uuid project_id FK
        uuid assignee_id FK
        uuid reporter_id FK
        string status
        string priority
        bigint version
    }
    LABELS {
        uuid id PK
        string name
    }
```

Comments, notifications, activities, project budgets and expenses are additive Flyway migrations.

## 3. Use case diagram

```mermaid
flowchart LR
    Producer([Producer])
    ContentManager([Content Manager])
    TeamMember([Team Member])

    Producer --> UC1[Manage users, teams, projects, tasks and project finances]
    Producer --> UC2[Assign tasks and review status]
    ContentManager --> UC3[Monitor projects and review tasks]
    TeamMember --> UC4[Update assigned tasks and add progress comments]
    ContentManager --> UC5[Track project progress]
    TeamMember --> UC5
```

## 4. Sequence diagrams

### Login → authenticated request

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant B as Backend
    participant DB as PostgreSQL

    U->>F: Submit email/password
    F->>B: POST /api/v1/auth/login
    B->>DB: SELECT user WHERE email = ?
    B->>B: BCrypt.matches(password, hash)
    B->>B: Issue access + refresh JWT
    B-->>F: 200 { accessToken, refreshToken, user }
    F->>F: Store tokens
    F->>B: GET /api/v1/projects?teamId=... (Authorization: Bearer ...)
    B->>B: JwtAuthenticationFilter validates token
    B-->>F: 200 [...]
```

### Create task → assign → move on board

```mermaid
sequenceDiagram
    participant F as Frontend
    participant B as Backend
    participant DB as PostgreSQL

    F->>B: POST /api/v1/tasks {projectId, title, ...}
    B->>B: ProjectAccessGuard.requireManageAccess (Producer)
    B->>DB: INSERT INTO tasks (status=TODO)
    B-->>F: 201 Task

    F->>B: PATCH /api/v1/tasks/{id}/assign {assigneeId}
    B->>B: ProjectAccessGuard.requireManageAccess
    B->>B: verify assignee is a project member
    B->>DB: UPDATE tasks SET assignee_id = ...
    B-->>F: 200 Task

    F->>B: PATCH /api/v1/tasks/{id}/status {status: IN_PROGRESS}
    B->>B: TaskStatusTransitionValidator.validate(TODO, IN_PROGRESS)
    B->>DB: UPDATE tasks SET status = ... WHERE id = ? AND version = ?
    B-->>F: 200 Task (or 409 if version mismatch / illegal transition)
```

### Task filtering

```mermaid
sequenceDiagram
    participant F as Frontend
    participant C as TaskController
    participant S as TaskService
    participant Spec as TaskSpecifications
    participant R as TaskRepository

    F->>C: GET /tasks?projectId=&status=&priority=&search=
    C->>S: listTasks(TaskFilter, currentUser, pageable)
    S->>S: ProjectAccessGuard.requireViewAccess
    S->>Spec: fromFilter(filter)
    Spec-->>S: composed Specification<Task>
    S->>R: findAll(spec, pageable)  [with @EntityGraph]
    R-->>S: Page<Task>
    S-->>C: PageResponse<TaskResponse>
```

## 5. Scope

Built modules include authentication and user management, team membership, projects with unique codes and required timelines, tasks and status reviews, comments, notifications, activity history, dashboards, project budgets and expenses.

## 6. Key decisions

- **DTOs everywhere, MapStruct for mapping** — entities never cross the service boundary.
- **`TeamAccessGuard` / `ProjectAccessGuard`** — resource-level authorization centralized once, reused by every service that touches team- or project-owned resources, instead of duplicated per service.
- **Business-role authorization**: Producers manage team/project/task records and finances; Content Managers can monitor and review tasks; Team Members update only assigned tasks and collaborate through comments. Team/project membership remains a separate resource-level requirement.
- **`@EntityGraph` + `@BatchSize`** to avoid N+1 on task listing (see `TaskRepository`).
- **`@Version` optimistic locking** on `Task` for concurrent Kanban drags.
- **Flyway, not `ddl-auto`** — the schema is explicit, versioned, and reviewable.
