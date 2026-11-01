# Order rejected

## Semantics

The order is valid, but placing it would break a rule of the business, typically by ordering
more of a product than is in stock. Status `409 Conflict`. Nothing was placed.

## Resolution

Repeating the same request will be rejected again unless the situation changes, for example
when the product is restocked. Check the inventory of the [product](../rels/product), or read a
new [orders-form](../rels/orders-form), and send the new request with a new `Idempotency-Key`.
