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

# fdroid expects a repo icon; ship a minimal placeholder if missing.
ICON="$FDROID_DIR/repo/icons/icon.png"
if [[ ! -f "$ICON" ]]; then
  FDROID_ROOT="$ROOT" python3 <<'PY'
import struct, zlib, pathlib
path = pathlib.Path(__import__("os").environ["FDROID_ROOT"]) / "fdroid/repo/icons/icon.png"
path.parent.mkdir(parents=True, exist_ok=True)
def chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
# 1x1 green PNG
raw = b"\x00\x00\x00\x00\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde\x01\x00\x00\x00\x0cIDATx\x9cc\x98\x05\x00\x00\x02\x00\x01\xe2!\xbc3\x00\x00\x00\x00IEND\xaeB`\x82"
path.write_bytes(
    b"\x89PNG\r\n\x1a\n"
    + chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 2, 0, 0, 0))
    + chunk(b"IDAT", zlib.compress(b"\x00\x01\x00\xfe\xff\x1b\x98\x05"))
    + chunk(b"IEND", b"")
)
PY
fi

cp "$APK_SRC" "$REPO_DIR/"

# Repo signing key (indexes the repository, NOT the APK). Cached in CI between runs.
# Optional: set FDROID_KEYSTORE_BASE64 secret to pin the same key across cache evictions.
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

cd "$FDROID_DIR"
# Debug APKs are not R8-minified; fdroidserver can scan them reliably.
fdroid update --create-metadata

if [[ ! -f "$REPO_DIR/index-v1.json" ]]; then
  echo "ERROR: fdroid update did not produce index-v1.json" >&2
  exit 1
fi

echo "F-Droid repo ready: $REPO_DIR"
ls -la "$REPO_DIR"
