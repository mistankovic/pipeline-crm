# Stage 6 — Frontend

## Builder hand-off

- Login, Kanban board with HTML5 drag-and-drop, deal detail + timeline
- Company / contact / deal / activity forms
- Forecast by stage and owner (Recharts)
- No close-won logic in the UI — failed moves toast the domain message and revert

## Adversarial review

**Finding 6.1** — Optimistic drag could leave a won deal in the won column when the server rejects.

**Fix** — On error, invalidate and restore from the server.

**Finding 6.2** — `useMemo` inside a render callback on Contacts.

**Fix** — Extracted `ContactList`.

## STAGE 6 APPROVED
