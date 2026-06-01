# Story 16: Internationalization (EN / DE)

## User Story

As a German or English speaker, I want the app in my language, defaulting to English unless my system language is German.

## Acceptance Criteria

- [x] `values/strings.xml` (English default)
- [x] `values-de/strings.xml` (German)
- [x] Locale follows system; defaults to English if not German
- [x] All user-visible strings externalized
- [x] No hardcoded UI strings in Compose screens

## Definition of Done

- `AppCompatDelegate` / per-app locale configured correctly
- German translations for all screens
