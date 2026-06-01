# Story 11: Auto-Message Seller on Agent Match

## User Story

As a logged-in user, I want to automatically send a message to the seller when a monitored search agent finds a new item.

## Acceptance Criteria

- [x] Per local agent: optional auto-message template
- [x] Requires login; disabled when logged out
- [x] Sends via gateway when new listing detected
- [x] Rate limiting / don't message same listing twice
- [x] Configurable in agent edit screen

## Definition of Done

- Unit test for duplicate prevention
- User confirmation toggle (opt-in per agent)
