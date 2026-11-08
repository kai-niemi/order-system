# Invalid order state

## Semantics

The order exists, but the operation is not legal in its current state, for example paying an
order that was cancelled or already has a payment pending, or delivering an order that has not
been paid. Status `409 Conflict`. The order is unchanged.

A request that fails this way leaves no idempotency record behind, so the same
`Idempotency-Key` can be used again once the order is in a suitable state.

## Resolution

Read the [order](../rels/order) again to see its current state and act on that. Delivering an
order that is already delivered is not an error: the request succeeds without any change.
