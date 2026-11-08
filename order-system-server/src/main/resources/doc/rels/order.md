# order

## Semantics

Identifies a single purchase order.

An order has no identifier property: the target of its `self` link is its identity. Keep that
URI to come back to the order later. It is the same URI as the `Location` returned when the
order was placed, and the one other resources link to with this relation, such as the
[items](items) collection of the order.

An order moves through the states `placed`, `payment_pending`, `paid`, `delivered` and
`cancelled`. A `placed` order can be paid or cancelled. Paying makes it `payment_pending` until
the payment is settled: it then becomes `paid`, or returns to `placed` if the payment was
declined or abandoned. A `paid` order can be delivered. `delivered` and `cancelled` are final.
Which transitions are available depends on the state the order is in now, and is advertised on
the order itself rather than described here. A `payment_pending` order offers none, so it can be
neither cancelled nor paid a second time.

A payment attempt, started with [pay](pay), links back to the order it belongs to with this
relation.

On the order form, this relation has a second meaning: it identifies the affordance for
submitting the form to place a new order. See [orders-form](orders-form).

## Affordances

Request the order as HAL-FORMS to also receive templates for the transitions that are legal in
its current state. A transition missing from the representation is not available, so a client
does not need to know the rules that decide it.

## Related link rels

- [items](items) — the line items of the order.
- [customer](customer) — the customer who placed the order.
- [cancel](cancel) — cancels the order, offered only while that is possible.
- [pay](pay) — starts payment of the order, offered only while that is possible.
- [deliver](deliver) — marks the order as delivered, offered only once it is paid.
- [orders](orders) — the collection the order belongs to.
- [orders-form](orders-form) — the form whose `order` affordance places a new order.
