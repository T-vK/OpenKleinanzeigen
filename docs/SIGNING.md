# Signing keys explained

OpenKleinanzeigen uses **two different kinds of keys**. They must not be confused.

## 1. APK signing key (the app package)

This is what Android checks when you install or update the app. **All APKs with the same `applicationId` must be signed with the same key** for updates to work.

| Build | Current CI setup | Secret needed? |
|-------|------------------|----------------|
| **Debug** (`assembleDebug`) | Android **debug keystore** on the runner (automatic) | **No** — fine for testing and this repo’s current releases |
| **Release** (`assembleRelease`, optional later) | Your own **release keystore** | **Yes** — add `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` |

**Why no secret today?** You asked to ship a **single debug build** for GitHub Releases and the custom F-Droid repo. Debug builds are already signed (with the debug certificate), so they install correctly. That is normal for internal/testing distribution.

**When to add `KEYSTORE_*` secrets:** Before publishing to the official F-Droid.org repo or Play Store, generate a **dedicated release keystore**, store it only in GitHub Actions secrets (never commit it), and build `assembleRelease` with that key. Keep the same key forever for that app id.

## 2. F-Droid **repository** signing key (the update index)

This signs `index-v1.json` in your **custom repo** on GitHub Pages. It does **not** sign the APK.

- Generated on first CI run and **cached** between workflow runs (`fdroid/keystore.p12`).
- Optional secret **`FDROID_KEYSTORE_BASE64`**: base64 of `fdroid/keystore.p12` so the repo index stays signed with the same key even if the cache is cleared.

```bash
# One-time, locally (after a successful CI run downloaded the keystore artifact, or after local fdroid init):
base64 -w0 fdroid/keystore.p12   # Linux
# Add as GitHub secret FDROID_KEYSTORE_BASE64
```

## Summary

| Secret | Purpose | Required now? |
|--------|---------|---------------|
| `KEYSTORE_*` | APK release signing | **No** (debug builds only) |
| `FDROID_KEYSTORE_BASE64` | Stable custom-repo index signing | **Optional** (cache usually enough) |

Not having `KEYSTORE_*` secrets is **expected** for debug-only distribution, not a misconfiguration.
