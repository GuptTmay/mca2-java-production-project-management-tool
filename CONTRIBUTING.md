# Contributing

## Branching

- `main` is always deployable.
- Work in feature branches: `feat/<short-description>`, `fix/<short-description>`.

## Commit messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(auth): implement JWT authentication
feat(task): add task filtering
feat(board): implement Kanban drag and drop
fix(project): correct date-range validation
test(task): add task service tests
ci: add GitHub Actions workflow
docs: add architecture documentation
refactor(team): extract TeamAccessGuard
```

## Before opening a PR

```bash
cd backend && mvn clean verify   # compiles + runs the full test suite
cd frontend && npm run build      # type-checks + builds
```

CI (`.github/workflows/ci.yml`) runs both automatically and must pass before merging.

## Code style

- Backend: package-by-feature, DTOs at every controller boundary, no entities in API responses, resource-level authorization lives in a `*AccessGuard` component if it's reused across services.
- Frontend: feature-folder structure under `src/features`, TanStack Query for all server state (no manual `useEffect` fetching), Tailwind utility classes only (no inline styles except for dynamic values like label colors).

## Adding an optional feature (comments/notifications/activity)

Add a new Flyway migration (`V2__add_comments.sql`, etc.) rather than editing `V1__init_schema.sql`. See `ARCHITECTURE.md` §5.
