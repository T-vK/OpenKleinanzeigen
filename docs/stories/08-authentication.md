# Story 08: Authentication (Login)

## User Story

As a registered Kleinanzeigen user, I want to log in so I can access messages and server-side features.

## Acceptance Criteria

- [x] Login screen (email + password)
- [x] Session token stored encrypted (Android Keystore / EncryptedSharedPreferences)
- [x] Logout clears session
- [x] Login state shown in Settings / Account section
- [x] Auth-gated features show prompt when logged out

## Definition of Done

- `AuthRepository` persists session
- Login integration test marked `@Ignore` in CI (gateway may block datacenter IPs); manual test documented

## Notes

Uses `gateway.kleinanzeigen.de` reverse-engineered login endpoint; may require real device network.
