# Order not cancellable

## Semantics

The order exists, but is not in a state that can be cancelled, for example because it has
already been cancelled or delivered. Status `409 Conflict`. The order is unchanged.

This normally means the order changed after it was read: an order only offers the
[cancel](../rels/cancel) link while it can be cancelled.

## Resolution

Read the [order](../rels/order) again to see its current state, and only cancel it if it still
offers the `cancel` link.
