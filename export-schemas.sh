#!/usr/bin/env bash
#
# Exports the JSON Schema of the API resource profiles from a
# running order-system server into docs/schemas/<profile>.json.
#
# For each profile (order, order-item, order-request, product, customer) it
# fetches /profiles/<profile> with 'Accept: application/schema+json', then
# validates with jq that the document declares the expected "$schema" and a
# "$id" of https://order-system.cloudneutral.se/schemas/<profile>.json.
# The script fails on any HTTP error or validation mismatch.
#
# The exported files are consumed by prepare-docs.sh when building the docs.
#
# Usage:
#   ./export-schemas.sh                              # server at http://localhost:8090
#   BASE_URL=http://host:port ./export-schemas.sh    # server elsewhere
#
# Requires: curl, jq, and a running server (see run-server.sh / start-server.sh).
# Run from the project root, since the output path is relative.

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
        "https://order-system.cloudneutral.se/schemas/" + $profile + ".json"
      )
    ' \
    "$TARGET/$profile.json" > /dev/null
done

echo "All schemas exported."