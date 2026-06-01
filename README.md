# OpenKleinanzeigen

Open-source Android client for [kleinanzeigen.de](https://www.kleinanzeigen.de), focused on **buyers** (search, alerts, messaging). Seller listing management may be added later.

## Features

| Without account | With login |
|-----------------|------------|
| Manual search | Messenger (read/send) |
| Local search agents + notifications | Kleinanzeigen backend search agents |
| Configurable polling | Auto-message seller on agent match |

Additional: debug log viewer, opt-in auto-update (default check every 5s when enabled), English/German UI.

## F-Droid compatibility

- No Google Play Services / Firebase / tracking SDKs
- **Anti-feature:** `NonFreeNet` (depends on kleinanzeigen.de APIs)
- Auto-update is **opt-in** and downloads only from this project's GitHub release / custom repo

### Custom repository

Add in F-Droid → Settings → Repositories:

```
https://t-vk.github.io/OpenKleinanzeigen/fdroid/repo
```

## Architecture

```
app/           → Compose UI, WorkManager, notifications
core:domain/   → models, repository interfaces
core:api/      → Kleinanzeigen HTTP client (api.kleinanzeigen.de + gateway)
core:data/     → Room, DataStore, repository implementations
core:common/   → logging
```

Public search uses the reverse-engineered mobile API (`Authorization: Basic …`, `User-Agent: okhttp/4.10.0`). Login/messaging uses `gateway.kleinanzeigen.de` and may be blocked on some networks (datacenter IPs).

## Build

```bash
./gradlew assembleDebug
```

Release APK (CI or local signing):

```bash
./gradlew assembleRelease
```

## Version bumps (CI)

Include one of these in your **commit message** on `main`:

- `[patch]` or `[release]` → 0.1.0 → 0.1.1
- `[minor]` → 0.2.0
- `[major]` → 1.0.0
- `release: 1.2.3` → exact version

## CI secrets (optional, for signed releases + F-Droid repo)

| Secret | Purpose |
|--------|---------|
| `KEYSTORE_BASE64` | Base64-encoded signing keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

Without secrets, CI still builds an unsigned release APK.

## User stories

Implementation progress: [`docs/stories/`](docs/stories/README.md)

## License

AGPL-3.0-or-later — see [LICENSE](LICENSE).
