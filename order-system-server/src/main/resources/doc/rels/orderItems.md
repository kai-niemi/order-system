# orderItems

## Semantics

Identifies the line items embedded in an order's [items](items) collection. Each embedded item
is one product line of the order: which product, how many, the unit price recorded when the
order was placed, and the resulting cost.

Each item has a `self` link, and links to its product with the [product](product) relation.
The product describes what it is now; the item records what was ordered.

This relation only appears under `_embedded`, never as a link.

## Related link rels

- [items](items) — the collection that embeds the items.
- [product](product) — the product of an item.
