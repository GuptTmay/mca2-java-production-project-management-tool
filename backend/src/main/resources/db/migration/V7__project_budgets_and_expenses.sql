ALTER TABLE projects
    ADD COLUMN budget_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    ADD CONSTRAINT chk_project_budget_nonnegative CHECK (budget_amount >= 0);

CREATE TABLE project_expenses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    description VARCHAR(250) NOT NULL,
    category    VARCHAR(100),
    amount      NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    spent_on    DATE NOT NULL,
    created_by  UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_project_expenses_project_spent_on
    ON project_expenses(project_id, spent_on DESC);
