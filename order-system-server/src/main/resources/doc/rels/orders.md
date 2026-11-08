# orders

## Semantics

Identifies the collection of purchase orders.

The collection is paged. Each page embeds its orders under this same relation, `orders`, and
links to the other pages with the registered `first`, `prev`, `next` and `last` relations,
present only when such a page exists. A `page` block carries the page size and totals. Follow
the page links rather than constructing page addresses.

The collection is also where new orders are placed, by submitting the [orders-form](orders-form).

## Affordances

Placing an order is idempotent on request: it requires an `Idempotency-Key` request header,
a value chosen by the client to identify the attempt.

- Repeating a request with the same key and the same body has at most one effect. The original
  order is returned and the `Idempotency-Replayed` response header is `true`.
- Reusing a key with a different body is rejected rather than replayed.
- Keys are scoped to the caller, so two clients choosing the same key do not collide, and are
  remembered for a limited retention period.

A placed order is returned as an [order](order), with its URI in the `Location` header.

Placing an order can fail with these problem types, besides the plain HTTP status codes:

- [idempotency-key-required](../problems/idempotency-key-required) — the key header is missing.
- [idempotency-key-reused](../problems/idempotency-key-reused) — the key was used for a
  different request.
- [idempotency-record-lost](../problems/idempotency-record-lost) — the outcome is unknown,
  repeat the request.
- [unknown-reference](../problems/unknown-reference) — the request refers to a customer or
  product that does not exist.
- [order-rejected](../problems/order-rejected) — the order breaks a business rule, such as
  ordering more than is in stock.

## Related link rels

- [order](order) — an individual order.
- [orders-form](orders-form) — the form used to place an order.
- [customers](customers) — customers who can place orders.
- [products](products) — products that can be ordered.
