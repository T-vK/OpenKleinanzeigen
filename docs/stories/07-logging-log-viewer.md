# Story 07: Debug Logging & Log Viewer

## User Story

As a user troubleshooting issues, I want to view, copy, and clear debug logs from within the app.

## Acceptance Criteria

- [x] Ring-buffer file logger (debug builds + opt-in release via setting)
- [x] Log viewer screen: scrollable text, Copy, Clear
- [x] Navigate: Settings → Debug logs
- [x] API requests/responses logged at debug level (redact passwords)

## Definition of Done

- Logger used across API and workers
- UI tests for clear/copy actions
