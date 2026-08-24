# Stage 6 — Builder hand-off

## Delivered

Svelte 5 UI consuming `/api` only:

- Login (`sales@pipelinecrm.demo` / `password`)
- Kanban pipeline board with HTML5 drag-and-drop stage changes
- Deal detail (edit, stage buttons, activity timeline)
- Company and contact list/create
- Forecast by owner and by stage

Stage guards stay on the server: a non-owner stage change shows the API error; closed-won still requires a Call/Meeting on the deal.

Vite proxies `/api` to `http://localhost:8080`. CORS allows localhost and 127.0.0.1 on 5173/4173.

## Browser verification (this pass)

Logged in, created Acme + Pat Lee, created deals, recorded a Meeting, QUALIFIED then CLOSED_WON (probability forced to 100). Forecast showed only the remaining open LEAD (250.00 USD). Non-owner stage change was rejected by the API.
