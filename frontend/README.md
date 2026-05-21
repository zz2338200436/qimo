# Frontend Extraction

This directory is the stage-2 extraction target for the legacy static frontend that previously lived only under
`major_assignment/src/main/resources/static/`.

## Deployment artifact

The current deployable frontend root is:

`frontend/dist`

Nginx deployment config now lives at:

`deploy/nginx/nginx.conf`

That config points the static root at `/usr/share/nginx/html` and proxies `/api/**` traffic to the Gateway upstream.

## Current transition state

The files in `frontend/dist` are a byte-for-byte mirror of the legacy static assets at the time of extraction.

To avoid breaking the monolith during the transition:

- the original files are still kept in `major_assignment/src/main/resources/static/`
- `frontend/dist` is now the externalized frontend deployment target
- Spring Boot static resource mapping is now disabled with `spring.web.resources.add-mappings=false`
- the monolith-side static files remain only as a temporary sync source until jar resource cleanup lands

## Included entry pages

Public entry:

- `index.html`
- `student-login.html`
- `teacher-login.html`

Student pages:

- `student-dashboard.html`
- `student-courses.html`
- `student-assignments.html`
- `student-stats.html`
- `student-notifications.html`
- `student-ai-assistant.html`
- `student-settings.html`

Teacher pages:

- `teacher-dashboard.html`
- `teacher-courses.html`
- `teacher-assignments.html`
- `teacher-student-dashboard.html`
- `teacher-warning.html`
- `teacher-knowledge.html`
- `teacher-ai-tools.html`
- `teacher-notifications.html`
- `teacher-settings.html`

Shared assets:

- `api.js`
- `i18n.js`
- `styles.css`
- `components/**`
- `i18n/**`
- `lib/**`

## Sync note

If we need to refresh the extracted frontend before the monolith static mapping is disabled, copy the contents of
`major_assignment/src/main/resources/static/` into `frontend/dist/` again so both locations stay aligned.
