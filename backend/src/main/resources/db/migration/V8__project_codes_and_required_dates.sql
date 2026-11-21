ALTER TABLE projects
    ADD COLUMN code VARCHAR(50);

UPDATE projects
SET code = 'PRJ-' || upper(replace(id::text, '-', ''));

UPDATE projects
SET start_date = COALESCE(start_date, end_date, created_at::date),
    end_date = COALESCE(end_date, start_date, created_at::date);

ALTER TABLE projects
    ALTER COLUMN code SET NOT NULL,
    ALTER COLUMN start_date SET NOT NULL,
    ALTER COLUMN end_date SET NOT NULL,
    ADD CONSTRAINT uq_projects_code UNIQUE (code);
