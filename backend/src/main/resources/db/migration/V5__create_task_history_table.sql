-- TaskFlow V5: task history
-- Additive migration; persistent audit log for task status changes.

CREATE TABLE task_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id         UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    actor_id        UUID NOT NULL REFERENCES users(id),
    previous_status VARCHAR(50),
    new_status      VARCHAR(50) NOT NULL,
    change_reason   VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_history_task ON task_history(task_id, created_at DESC);
CREATE INDEX idx_task_history_project ON task_history(project_id, created_at DESC);
