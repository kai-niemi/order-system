# product

## Semantics

Identifies a single product that can be ordered, as it is now: its name, stock keeping unit,
current price and inventory.

Order line items link to their product with this relation. The line item records the price
paid; the product shows the current price, which may since have changed.

## Related link rels

- [products](products) — the collection of products.
- [orderItems](orderItems) — line items that link to their product.
