# cancel

## Semantics

Identifies the transition that cancels an [order](order), moving it to the `cancelled` state.

The link is present on an order only while the order can be cancelled. Its absence means the
transition is not available, so a client should not attempt it and does not need to know the
rule behind it. The rule is the server's, and may change.

## Affordances

Request the order as HAL-FORMS to receive the template for this transition, and submit it using
the template's method and target. The template has no properties, so the request has no body.
The response is the updated order, which no longer offers this link.

If the order changes between reading it and submitting the template, for example because
someone else cancelled it first, the request is rejected with the problem type
[order-not-cancellable](../problems/order-not-cancellable).

## Related link rels

- [order](order) — the order that offers this transition.

See also the guide [Cancelling an order](../guides/cancel-order).
