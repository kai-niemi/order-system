# pay

## Semantics

Identifies the transition that starts payment of an [order](order). It moves the order from the
`placed` state to `payment_pending` and creates a payment attempt for the total price of the
order.

The link is present on an order only while the order can be paid, which is while it is `placed`.
Its absence means the transition is not available, so a client should not attempt it and does not
need to know the rule behind it. The rule is the server's, and may change.

Payment is asynchronous. The request is only accepted: the response is `202 Accepted`, with the
payment attempt as its body and its URI in the `Location` header. The attempt starts out
`pending` and is settled later by the server, so a client follows the `self` link of the attempt
to learn the outcome. The attempt links back to its order with the [order](order) relation.

A payment attempt has one of these statuses:

- `pending` — accepted, not yet settled.
- `succeeded` — the payment went through. The order becomes `paid`.
- `declined` — the payment was refused. The order returns to `placed`.
- `failed` — the server gave up on the attempt after repeated tries without a result. The order
  returns to `placed`.

Only one payment can be pending for an order at a time. After a `declined` or `failed` attempt
the order offers this transition again, and paying starts a new attempt.

## Affordances

Request the order as HAL-FORMS to receive the template for this transition, and submit it using
the template's method and target. The template has no properties, so the request has no body.

Paying is idempotent on request: it requires an `Idempotency-Key` request header, a value chosen
by the client to identify the attempt.

- Repeating a request with the same key and for the same order has at most one effect. The
  `Idempotency-Replayed` response header is `true`.
- A replay returns the payment attempt as it is now, which may have progressed since the
  original response. It is not a copy of the first response.
- Reusing a key for a different order is rejected rather than replayed.
- Keys are scoped to the caller, and are remembered for a limited retention period.

Paying can fail with these problem types, besides the plain HTTP status codes:

- [idempotency-key-required](../problems/idempotency-key-required) — the key header is missing.
- [idempotency-key-reused](../problems/idempotency-key-reused) — the key was used for a
  different request.
- [idempotency-record-lost](../problems/idempotency-record-lost) — the outcome is unknown,
  repeat the request.
- [invalid-order-state](../problems/invalid-order-state) — the order is not in a state that can
  be paid, for example because it was cancelled, or a payment is already pending. A request
  rejected this way leaves no idempotency record, so its key can be used again.

An order that does not exist is a plain `404 Not Found`. So is a payment attempt requested under
another order than its own.

## Related link rels

- [order](order) — the order that offers this transition, and that a payment attempt belongs to.
- [cancel](cancel) — the other transition offered while the order is `placed`.
- [deliver](deliver) — offered once the payment has succeeded.

See also the guide [Paying for and delivering an order](../guides/pay-and-deliver-order).
