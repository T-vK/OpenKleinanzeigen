#!/usr/bin/env bash
# Builds a complete F-Droid repo (index.html, QR code, icons, signed index-v1.jar).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FDROID_DIR="$ROOT/fdroid"
KEYSTORE="$FDROID_DIR/keystore.p12"
APK_SRC="${1:-}"

if [[ -z "$APK_SRC" || ! -f "$APK_SRC" ]]; then
  echo "Usage: $0 /path/to/OpenKleinanzeigen-x.y.z.apk" >&2
  exit 1
fi

mkdir -p "$FDROID_DIR/metadata" "$FDROID_DIR/repo"

# Copy app metadata into fdroid tree if needed.
if [[ -f "$ROOT/metadata/de.openkleinanzeigen.yml" ]]; then
  cp "$ROOT/metadata/de.openkleinanzeigen.yml" "$FDROID_DIR/metadata/"
fi

# Repo signing key (signs the index, not the APK).
if [[ -n "${FDROID_KEYSTORE_BASE64:-}" ]]; then
  echo "$FDROID_KEYSTORE_BASE64" | base64 -d > "$KEYSTORE"
elif [[ ! -f "$KEYSTORE" ]]; then
  echo "Generating F-Droid repo signing keystore"
  keytool -genkeypair -v \
    -keystore "$KEYSTORE" \
    -storetype PKCS12 \
    -storepass "${FDROID_KEYSTORE_PASS:-openkleinanzeigen}" \
    -alias "${FDROID_KEY_ALIAS:-openkleinanzeigen}" \
    -keypass "${FDROID_KEY_PASS:-openkleinanzeigen}" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=OpenKleinanzeigen F-Droid Repo"
fi

CONFIG="$FDROID_DIR/config.yml"
if [[ ! -f "$CONFIG" ]]; then
  "$ROOT/scripts/setup_fdroid_repo.sh"
fi
if ! grep -q "repo_keyalias" "$CONFIG"; then
  cat >> "$CONFIG" <<EOF

repo_keyalias: ${FDROID_KEY_ALIAS:-openkleinanzeigen}
keystore: keystore.p12
keystorepass: ${FDROID_KEYSTORE_PASS:-openkleinanzeigen}
keypass: ${FDROID_KEY_PASS:-openkleinanzeigen}
keydname: CN=OpenKleinanzeigen F-Droid Repo
repo_icon: icon.png
EOF
fi

# Placeholder repo icon until fdroid update generates the final set.
mkdir -p "$FDROID_DIR/repo/icons"
if [[ -f "$ROOT/metadata/repo-icon.png" ]]; then
  cp "$ROOT/metadata/repo-icon.png" "$FDROID_DIR/repo/icons/icon.png"
elif [[ ! -f "$FDROID_DIR/repo/icons/icon.png" ]]; then
  echo "Missing metadata/repo-icon.png" >&2
  exit 1
fi
chmod 600 "$CONFIG" "$KEYSTORE" 2>/dev/null || true

pip install --quiet 'fdroidserver==2.3.3' 'androguard==3.4.0a1' 'qrcode[pil]'

chmod +x "$ROOT/scripts/build_fdroid_repo.py"
"$ROOT/scripts/build_fdroid_repo.py" "$APK_SRC" "$FDROID_DIR"

echo "Repo listing:"
ls -la "$FDROID_DIR/repo" | head -25
