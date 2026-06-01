# Story 14: CI/CD Pipeline (GitHub Actions)

## User Story

As a maintainer, I want automated builds, tests, version bumps, and releases on every push to main.

## Acceptance Criteria

- [x] Workflow: build debug + release APK on push/PR
- [x] Unit tests + live API test job
- [x] Version bump from commit message (`[major]`, `[minor]`, `[patch]` or `release: x.y.z`)
- [x] Single release APK artifact used everywhere
- [x] GitHub Release with APK named `OpenKleinanzeigen-{version}.apk`
- [x] `versionCode` / `versionName` embedded in APK

## Definition of Done

- CI green on main
- Release workflow triggers on version bump commits
