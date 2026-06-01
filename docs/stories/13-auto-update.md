# Story 13: Auto-Update from Custom F-Droid Repo

## User Story

As a user, I want optional auto-update that checks for new releases, downloads APK in background, and prompts to install.

## Acceptance Criteria

- [x] Setting: enable auto-update (opt-in, F-Droid compliant)
- [x] Check interval configurable (default 5 seconds when enabled)
- [x] Fetches `index-v1.json` or GitHub Releases API from project repo
- [x] Compares `versionCode` with installed app
- [x] Downloads APK to app cache, notification to install (PackageInstaller intent)
- [x] REQUEST_INSTALL_PACKAGES permission with user consent flow

## Definition of Done

- Update checker unit tested with mock metadata
- Uses same APK URL as GitHub Releases / F-Droid repo
