# Story 09: Messenger UI

## User Story

As a logged-in user, I want to read and send messages to sellers with a modern chat UI.

## Acceptance Criteria

- [x] Conversations list (title, preview, timestamp, unread badge)
- [x] Chat screen with message bubbles, send field
- [x] Pull to refresh / background sync when logged in
- [x] Navigate: Messages tab (login required gate)
- [x] Empty state when no conversations

## Definition of Done

- Messages cached in Room
- Send message calls gateway API when session valid
