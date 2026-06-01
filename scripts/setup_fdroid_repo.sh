#!/usr/bin/env bash
# Initialize fdroid repo structure for GitHub Pages deployment.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FDROID_DIR="$ROOT/fdroid"

mkdir -p "$FDROID_DIR/repo"

if [[ ! -f "$FDROID_DIR/config.yml" ]]; then
  cat > "$FDROID_DIR/config.yml" <<'EOF'
repo_name: OpenKleinanzeigen
repo_description: Custom F-Droid repository for OpenKleinanzeigen
repo_url: https://t-vk.github.io/OpenKleinanzeigen/fdroid/repo
repo_maxage: 366
archive_older: 0
EOF
fi

echo "F-Droid repo skeleton ready at $FDROID_DIR"
