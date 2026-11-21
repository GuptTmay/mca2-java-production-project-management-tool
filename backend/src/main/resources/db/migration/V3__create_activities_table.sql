-- TaskFlow V3: activities
-- Additive migration; tracks task/project actions for audit history.

CREATE TABLE activities (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id     UUID REFERENCES tasks(id) ON DELETE CASCADE,
    project_id  UUID REFERENCES projects(id) ON DELETE CASCADE,
    actor_id    UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    type        VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    metadata    TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_activities_task ON activities(task_id);
CREATE INDEX idx_activities_project ON activities(project_id);
CREATE INDEX idx_activities_actor ON activities(actor_id);
