# Story 02: Kleinanzeigen API Client Layer

## User Story

As a user without an account, I want the app to search listings on kleinanzeigen.de so I can browse classifieds.

## Acceptance Criteria

- [x] HTTP client using OkHttp/Ktor without proprietary SDKs
- [x] Public search via `api.kleinanzeigen.de` with documented mobile Basic auth
- [x] JSON parser for Kleinanzeigen JAXB-style responses
- [x] Models: `Listing`, `SearchQuery`, `Location`, `Category`
- [x] Endpoints: search ads, ad detail, top locations, categories
- [x] Gateway client interface for authenticated calls (login, messages) with pluggable implementation
- [x] Unit tests with mocked responses
- [x] Live API integration test (search returns ≥1 result or skips gracefully)

## Definition of Done

- `KleinanzeigenApi` interface in `core:domain`, implementation in `core:api`
- Errors mapped to typed `ApiException`
- Live test runs in CI against real API
