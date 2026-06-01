# Story 01: Project Foundation & Architecture

## User Story

As a developer, I want a well-structured Android project with modular architecture so that features can grow (including future seller tools) without rewriting the codebase.

## Acceptance Criteria

- [x] Multi-module Gradle project (`app`, `core:api`, `core:data`, `core:domain`, `core:common`)
- [x] Kotlin, Jetpack Compose, Material 3
- [x] AGPL-3.0 license and F-Droid metadata stub (`metadata/en-US`)
- [x] Application ID `de.openkleinanzeigen` with debug/release build types
- [x] Navigation shell (bottom bar) reachable from cold start
- [x] README with architecture overview and F-Droid anti-feature note (`NonFreeNet`)

## Definition of Done

- Project builds with `./gradlew assembleDebug`
- Module dependency graph documented in README
- No proprietary Google Play Services / Firebase dependencies

## Notes

- Seller listing CRUD is out of scope but `core:domain` uses interfaces extensible for seller use cases later.
