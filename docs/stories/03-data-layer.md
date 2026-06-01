# Story 03: Data Layer (Room, DataStore, Repositories)

## User Story

As a user, I want my search agents, settings, and cached messages stored locally so the app works offline for already-fetched data.

## Acceptance Criteria

- [x] Room database for search agents, seen listing IDs, conversations, messages
- [x] DataStore for user preferences
- [x] Repositories: `ListingRepository`, `SearchAgentRepository`, `SettingsRepository`, `AuthRepository`, `MessageRepository`
- [x] Repository unit tests with in-memory Room

## Definition of Done

- Migrations strategy documented (version 1 schema)
- Repositories exposed to ViewModels via domain use cases
