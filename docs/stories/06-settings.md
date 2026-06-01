# Story 06: Settings & Preferences

## User Story

As a user, I want to configure polling intervals, notifications, language, and auto-update so the app behaves how I prefer.

## Acceptance Criteria

- [x] Settings screen reachable from navigation
- [x] Poll interval for search agents (seconds, minimum enforced)
- [x] Per-category notification toggles (new listings, messages, agent matches)
- [x] Auto-update enable/disable
- [x] Update check interval (default 5 seconds when enabled, as specified)
- [x] Theme follows system (optional light/dark if time permits)

## Definition of Done

- All settings persist via DataStore
- Settings affect WorkManager schedules immediately
