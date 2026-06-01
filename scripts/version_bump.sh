#!/usr/bin/env bash
# Bumps version based on latest commit message.
# [major] / [minor] / [patch] or release: x.y.z
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROPS="$ROOT/version.properties"

current_code=$(grep VERSION_CODE "$PROPS" | cut -d= -f2)
current_name=$(grep VERSION_NAME "$PROPS" | cut -d= -f2)

msg="${1:-$(git -C "$ROOT" log -1 --pretty=%B)}"

if [[ "$msg" =~ release:\ *v?([0-9]+\.[0-9]+\.[0-9]+) ]]; then
  new_name="${BASH_REMATCH[1]}"
  IFS=. read -r major minor patch <<< "$new_name"
  new_code=$((major * 10000 + minor * 100 + patch))
elif [[ "$msg" == *"[major]"* ]]; then
  IFS=. read -r major minor patch <<< "$current_name"
  major=$((major + 1)); minor=0; patch=0
  new_name="$major.$minor.$patch"
  new_code=$((current_code + 10000))
elif [[ "$msg" == *"[minor]"* ]]; then
  IFS=. read -r major minor patch <<< "$current_name"
  minor=$((minor + 1)); patch=0
  new_name="$major.$minor.$patch"
  new_code=$((current_code + 100))
elif [[ "$msg" == *"[patch]"* ]] || [[ "$msg" == *"[release]"* ]]; then
  IFS=. read -r major minor patch <<< "$current_name"
  patch=$((patch + 1))
  new_name="$major.$minor.$patch"
  new_code=$((current_code + 1))
else
  echo "No version bump token in commit message; keeping $current_name ($current_code)"
  exit 0
fi

cat > "$PROPS" <<EOF
VERSION_CODE=$new_code
VERSION_NAME=$new_name
EOF

echo "Bumped to $new_name ($new_code)"
