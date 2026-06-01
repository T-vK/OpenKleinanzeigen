# Story 10: Backend Search Agents (Logged In)

## User Story

As a logged-in user, I want to see and sync search agents configured on Kleinanzeigen so I get notifications for server-side agents too.

## Acceptance Criteria

- [x] List backend search agents when logged in
- [x] Sync from Kleinanzeigen API
- [x] Show combined view or separate section from local agents
- [x] Notifications for backend agent matches (setting toggle)

## Definition of Done

- Sync worker runs when logged in and setting enabled
- Graceful degradation when API unavailable
