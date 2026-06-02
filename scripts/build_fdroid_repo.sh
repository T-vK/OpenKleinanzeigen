#!/usr/bin/env bash
# Builds the F-Droid repo directory for GitHub Pages (fdroid/repo/...).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FDROID_DIR="$ROOT/fdroid"
REPO_DIR="$FDROID_DIR/repo"
CONFIG="$FDROID_DIR/config.yml"
KEYSTORE="$FDROID_DIR/keystore.p12"
APK_SRC="${1:-}"

mkdir -p "$REPO_DIR"

if [[ -z "$APK_SRC" || ! -f "$APK_SRC" ]]; then
  echo "Usage: $0 /path/to/OpenKleinanzeigen-x.y.z.apk" >&2
  exit 1
fi

cp "$APK_SRC" "$REPO_DIR/"

# Stable repo signing key (cached in CI between runs).
if [[ ! -f "$KEYSTORE" ]]; then
  echo "Generating F-Droid repo signing keystore at $KEYSTORE"
  keytool -genkeypair -v \
    -keystore "$KEYSTORE" \
    -storetype PKCS12 \
    -storepass "${FDROID_KEYSTORE_PASS:-openkleinanzeigen}" \
    -alias "${FDROID_KEY_ALIAS:-openkleinanzeigen}" \
    -keypass "${FDROID_KEY_PASS:-openkleinanzeigen}" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=OpenKleinanzeigen F-Droid Repo"
fi

if ! grep -q "repo_keyalias" "$CONFIG" 2>/dev/null; then
  cat >> "$CONFIG" <<EOF

repo_keyalias: ${FDROID_KEY_ALIAS:-openkleinanzeigen}
keystore: keystore.p12
keystorepass: ${FDROID_KEYSTORE_PASS:-openkleinanzeigen}
keypass: ${FDROID_KEY_PASS:-openkleinanzeigen}
keydname: CN=OpenKleinanzeigen F-Droid Repo
EOF
fi

cd "$FDROID_DIR"
fdroid update --create-metadata

if [[ ! -f "$REPO_DIR/index-v1.json" ]]; then
  echo "ERROR: fdroid update did not produce index-v1.json" >&2
  exit 1
fi

echo "F-Droid repo ready: $REPO_DIR"
ls -la "$REPO_DIR"
