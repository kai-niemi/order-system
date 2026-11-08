#!/usr/bin/env bash
#
# Assembles the documentation sources into the .docs-build directory.
#
# It first checks that the schemas exported by export-schemas.sh exist in
# docs/schemas (one non-empty JSON file per profile) and aborts otherwise,
# before touching the previous build. It then recreates .docs-build and copies
# in the server's rels, problems and guides resources, docs/index.md,
# docs/profiles.md and docs/schemas.
#
# Usage:
#   ./export-schemas.sh && ./prepare-docs.sh
#
# Run from the project root, since all paths are relative.

set -euo pipefail

SOURCE="order-system-server/src/main/resources/doc"
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