# Story 15: F-Droid Custom Repository on GitHub Pages

## User Story

As a user preferring F-Droid, I want to add a custom repository URL to install and update OpenKleinanzeigen.

## Acceptance Criteria

- [x] F-Droid repo at `https://t-vk.github.io/OpenKleinanzeigen/fdroid/repo`
- [x] CI updates repo index from the same APK artifact as GitHub Releases
- [x] `metadata/en-US` with `AntiFeatures: NonFreeNet`
- [x] Signing via GitHub secrets (`KEYSTORE_BASE64`, etc.)
- [x] Only one build output per pipeline run

## Definition of Done

- `index-v1.json` valid and points to release APK
- Documentation in README for adding repo in F-Droid client
