# products

## Semantics

Identifies the collection of products available to order.

The collection is paged. Each page embeds its products under this same relation, `products`,
and links to the other pages with the registered `first`, `prev`, `next` and `last` relations,
present only when such a page exists. A `page` block carries the page size and totals. Follow
the page links rather than constructing page addresses.

Each embedded product is an individual [product](product), with its own `self` link.

## Related link rels

- [product](product) — an individual product.
- [orders-form](orders-form) — a form for placing an order with products from this collection.
