# Idempotency key required

## Semantics

The request places an order but has no `Idempotency-Key` header. Placing an order is only
accepted with a key, so that a request repeated after a lost response cannot place the order
twice. Status `400 Bad Request`. Nothing was placed.

## Resolution

Send the request again with an `Idempotency-Key` header: a value of your choosing, such as a
random UUID, that identifies this attempt. Keep the key for as long as you might repeat the
request. See [orders](../rels/orders).
