# Stage 6 — Fixes after review

## Important 1 — Contact edit missing

`client.updateContact` calls `PUT /api/contacts/{id}`. Contacts table has an Edit action (name + email via prompt). Company/contact load and company rename now surface API errors. New deals default to the logged-in user when that user exists.
