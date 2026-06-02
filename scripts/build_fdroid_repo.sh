#!/usr/bin/env bash
# Builds the F-Droid repo directory for GitHub Pages (fdroid/repo/...).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FDROID_DIR="$ROOT/fdroid"
REPO_DIR="$FDROID_DIR/repo"
CONFIG="$FDROID_DIR/config.yml"
KEYSTORE="$FDROID_DIR/keystore.p12"
APK_SRC="${1:-}"

mkdir -p "$REPO_DIR" "$FDROID_DIR/repo/icons"

if [[ -z "$APK_SRC" || ! -f "$APK_SRC" ]]; then
  echo "Usage: $0 /path/to/OpenKleinanzeigen-x.y.z.apk" >&2
  exit 1
fi

# fdroid expects a repo icon.
ICON="$FDROID_DIR/repo/icons/icon.png"
if [[ ! -f "$ICON" ]]; then
  cp "$ROOT/app/src/main/res/drawable/ic_launcher_foreground.xml" "$ICON" 2>/dev/null || true
  if [[ ! -f "$ICON" ]] || file "$ICON" | grep -q XML; then
    # Minimal valid PNG placeholder
    python3 -c "
import struct, zlib, pathlib
p = pathlib.Path('$ICON')
p.parent.mkdir(parents=True, exist_ok=True)
def chunk(t, d):
    return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
p.write_bytes(b'\\x89PNG\\r\\n\\x1a\\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 48, 48, 8, 2, 0, 0, 0))
+ chunk(b'IDAT', zlib.compress(b'\\x00' * 49)) + chunk(b'IEND', b''))
"
  fi
fi

# Repo signing key (signs index-v1.json, NOT the APK). Cached in CI between runs.
if [[ -n "${FDROID_KEYSTORE_BASE64:-}" ]]; then
  echo "$FDROID_KEYSTORE_BASE64" | base64 -d > "$KEYSTORE"
elif [[ ! -f "$KEYSTORE" ]]; then
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
repo_icon: repo/icons/icon.png
EOF
fi

chmod 600 "$CONFIG" "$KEYSTORE" 2>/dev/null || true

# Build index without androguard (broken on AGP 35 / R8 output).
pip install --quiet 'fdroidserver==2.3.3' 'androguard==3.4.0a1'
chmod +x "$ROOT/scripts/build_fdroid_index.py"
"$ROOT/scripts/build_fdroid_index.py" "$APK_SRC" "$FDROID_DIR"

if [[ ! -f "$REPO_DIR/index-v1.json" ]]; then
  echo "ERROR: index-v1.json missing after build" >&2
  exit 1
fi

echo "F-Droid repo ready: $REPO_DIR"
ls -la "$REPO_DIR" | head -20
