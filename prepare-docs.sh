#!/usr/bin/env bash

set -euo pipefail

SOURCE="order-system-server/src/main/resources"
TARGET=".docs-build"

# Ensure exported schemas exist before deleting the previous build
test -d docs/schemas || {
  echo "Missing docs/schemas. Run export-schemas.sh first." >&2
  exit 1
}

for profile in order order-item order-request product customer; do
  test -s "docs/schemas/$profile.json" || {
    echo "Missing schema: $profile" >&2
    exit 1
  }
done

rm -rf "$TARGET"
mkdir -p "$TARGET"

cp -R "$SOURCE/rels" "$TARGET/"
cp -R "$SOURCE/problems" "$TARGET/"
cp -R "$SOURCE/guides" "$TARGET/"

cp docs/index.md "$TARGET/index.md"
cp docs/profiles.md "$TARGET/profiles.md"
cp -R docs/schemas "$TARGET/"