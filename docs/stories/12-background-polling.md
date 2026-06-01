# Story 12: Background Polling Workers

## User Story

As a user, I want search agents and message checks to run in the background so I receive timely notifications.

## Acceptance Criteria

- [x] WorkManager workers for local agents, messages (if logged in), backend agents
- [x] Respects poll interval from settings
- [x] Android notification channels for matches and messages
- [x] Foreground service exemption avoided (use WorkManager + expedited only when needed)

## Definition of Done

- Workers registered on boot / settings change
- Battery-aware constraints (network connected)
