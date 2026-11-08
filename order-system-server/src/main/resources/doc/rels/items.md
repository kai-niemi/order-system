# items

## Semantics

Identifies the collection of line items belonging to one [order](order).

The line items are embedded in the collection under the [orderItems](orderItems) relation, and
the collection links back to its order with the [order](order) relation. The collection is not
paged.

Each item is its own resource with a `self` link, and a stable position within the order. The
quantity and unit price of an item are recorded when the order is placed, so they do not change
when the product does.

## Related link rels

- [orderItems](orderItems) — the embedded line items.
- [order](order) — the order the items belong to.
- [product](product) — the product of a line item.
