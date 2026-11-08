# pay

## Semantics

Identifies the transition that starts payment of an [order](order). It moves the order from `placed` to
`payment_pending` and creates a payment attempt for the order's total price.

The `pay` affordance is present on an order only while payment can be initiated, which is currently while the order is
`placed`. Its absence means the transition is unavailable. Clients should follow the advertised affordance rather than
reproduce the server's eligibility rules, which may change.

Payment is asynchronous. The server responds with `202 Accepted`, returning the payment attempt representation in the
response body and its URI in the `Location` header. The attempt initially has status `pending`. Clients can follow its
`self` link to check the outcome. The attempt links back to its order using the [order](order) relation.

A payment attempt has one of these statuses:

- `pending`: Accepted and awaiting a definitive outcome.
- `succeeded`: Payment succeeded. The order becomes `paid`.
- `declined`: Payment was refused. The order returns to `placed`.
- `unresolved`: Automatic processing stopped without establishing whether the customer was charged. The order becomes
  `payment_blocked` to prevent another payment attempt. The attempt remains unresolved until the server reconciles it
  with the provider, after which it becomes `succeeded` or `declined` and the order transitions accordingly.

While the order is `payment_blocked`, the client must not initiate another payment. It can follow the payment attempt
and order links to check for a reconciled outcome.

Only one payment attempt can be pending for an order at a time. After a declined attempt, the order advertises `pay`
again, and initiating payment creates a new attempt. The affordance is not available while the order is
`payment_pending` or `payment_blocked`.

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
