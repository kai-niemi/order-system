#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8090}"
TARGET="docs/schemas"

mkdir -p "$TARGET"

for profile in \
  order \
  order-item \
  order-request \
  product \
  customer
do
  echo "Exporting $profile"

  curl -fsS \
    -H 'Accept: application/schema+json' \
    "$BASE_URL/profiles/$profile" \
    -o "$TARGET/$profile.json"

  jq -e \
    --arg profile "$profile" \
    '
      .["$schema"] == "https://json-schema.org/draft/2020-12/schema"
      and .["$id"] == (
        "https://order-system.cloudneutral.se/profiles/" + $profile
      )
    ' \
    "$TARGET/$profile.json" > /dev/null
done

echo "All schemas exported."