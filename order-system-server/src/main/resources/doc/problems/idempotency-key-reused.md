# Idempotency key reused

## Semantics

The `Idempotency-Key` was already used, within its retention period, for a different request.
A key identifies one attempt, so a request is only replayed when it is the same as the one the
key was first used for. Status `422 Unprocessable Content`. Nothing was placed by this request.

Two requests are the same when they have the same method, target and body. Differences that do
not change the meaning of the body, such as the order of its properties, do not count.

## Resolution

- If this is a new request, send it with a new key.
- If it was meant as a repeat of an earlier request, send exactly the body of that request.
  Note that an [orders-form](../rels/orders-form) is filled in anew each time it is read, so a
  repeat must reuse the earlier body rather than read the form again.

See [orders](../rels/orders).
