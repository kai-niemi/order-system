#!/usr/bin/env bash

set -euo pipefail

SOURCE="order-system-server/src/main/resources"
TARGET=".docs-build"

rm -rf "$TARGET"

mkdir -p "$TARGET"

cp -R "$SOURCE/rels" "$TARGET/"
cp -R "$SOURCE/problems" "$TARGET/"
cp -R "$SOURCE/guides" "$TARGET/"

cp docs/index.md "$TARGET/index.md"