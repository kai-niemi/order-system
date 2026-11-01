# customers

## Semantics

Identifies the collection of customers who can place orders.

The collection is paged. Each page embeds its customers under this same relation, `customers`,
and links to the other pages with the registered `first`, `prev`, `next` and `last` relations,
present only when such a page exists. A `page` block carries the page size and totals. Follow
the page links rather than constructing page addresses.

Each embedded customer is an individual [customer](customer), with its own `self` link.

## Related link rels

- [customer](customer) — an individual customer.
- [orders](orders) — the orders customers have placed.
