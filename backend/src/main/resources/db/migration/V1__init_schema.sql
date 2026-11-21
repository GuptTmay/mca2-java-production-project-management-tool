-- TaskFlow Core schema (V1).
-- Optional-feature tables (comments, notifications, activity) are added
-- in later, additive migrations: V2, V3, V4.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =========================================================
-- users
-- =========================================================
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(30)  NOT NULL
                    CHECK (role IN ('ADMIN', 'PROJECT_MANAGER', 'DEVELOPER')),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- =========================================================
-- teams
-- =========================================================
CREATE TABLE teams (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    created_by  UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE team_members (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id   UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_role VARCHAR(30) NOT NULL
                CHECK (team_role IN ('OWNER', 'MANAGER', 'MEMBER')),
    joined_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_team_members_team_user UNIQUE (team_id, user_id)
);

CREATE INDEX idx_team_members_user ON team_members(user_id);

-- =========================================================
-- projects
-- =========================================================
CREATE TABLE projects (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id     UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    status      VARCHAR(30) NOT NULL DEFAULT 'PLANNING'
                  CHECK (status IN ('PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED')),
    start_date  DATE,
    end_date    DATE,
    created_by  UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_project_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_projects_team ON projects(team_id);

CREATE TABLE project_members (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    added_at   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_project_members_project_user UNIQUE (project_id, user_id)
);

CREATE INDEX idx_project_members_user ON project_members(user_id);

-- =========================================================
-- tasks
-- =========================================================
CREATE TABLE tasks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    status      VARCHAR(30) NOT NULL DEFAULT 'TODO'
                  CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
    priority    VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
                  CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    assignee_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    deadline    TIMESTAMP,
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_project_status ON tasks(project_id, status);
CREATE INDEX idx_tasks_assignee ON tasks(assignee_id);
CREATE INDEX idx_tasks_deadline ON tasks(deadline);

-- =========================================================
-- labels
-- =========================================================
CREATE TABLE labels (
    id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name  VARCHAR(50) NOT NULL UNIQUE,
    color VARCHAR(7)  NOT NULL DEFAULT '#6B7280'
);

CREATE TABLE task_labels (
    task_id  UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    label_id UUID NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, label_id)
);
