# Idempotency record lost

## Semantics

The server could neither place the order nor find the earlier attempt that claimed the
`Idempotency-Key`. This is not expected in normal operation. Status `409 Conflict`. Whether an
earlier attempt with the same key placed the order is not known from this response.

## Resolution

Repeat the same request with the same key. The key ensures the order is placed at most once:
the repeat either places it, or returns the order an earlier attempt placed. See
[orders](../rels/orders).
