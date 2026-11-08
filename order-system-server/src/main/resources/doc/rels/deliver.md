# deliver

## Semantics

Identifies the transition that marks an [order](order) as delivered, moving it to the
`delivered` state.

The link is present on an order only while the order can be delivered, which is once it is
`paid`. Its absence means the transition is not available, so a client should not attempt it and
does not need to know the rule behind it. The rule is the server's, and may change.

## Affordances

Request the order as HAL-FORMS to receive the template for this transition, and submit it using
the template's method and target. The template has no properties, so the request has no body.
The response is `204 No Content`. Read the order again to see its new state, which no longer
offers this link.

The transition needs no `Idempotency-Key`, because its outcome is a state. Delivering an order
that is already delivered succeeds without any change, so a request repeated after a lost
response gets `204 No Content` as well.

Delivering an order that is not paid, for example one that is still `placed` or is
`payment_pending`, is rejected with the problem type
[invalid-order-state](../problems/invalid-order-state). An order that does not exist is a plain
`404 Not Found`.

## Related link rels

- [order](order) — the order that offers this transition.
- [pay](pay) — the transition that makes an order deliverable.

See also the guide [Paying for and delivering an order](../guides/pay-and-deliver-order).
