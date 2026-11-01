# orders-form

## Semantics

Identifies a form for placing an order, filled in by the server with a customer and a number of
product lines chosen at random. It is meant for exploring and exercising the API rather than
for a customer choosing what to buy.

The link is templated. Its `products` variable is the number of product lines to include, and
defaults to one when left out. If there are no customers or products to choose from, the form
does not exist.

The state of the form is the request to submit. Prices are not part of it: the price of each
line is decided by the server from the current product price when the order is placed.

## Affordances

The form links to the [order](order) affordance for submitting it. Request the form as HAL-FORMS
to also receive the template for that affordance, and submit the state of the form using the
template's method, target and content type. Placing an order requires an `Idempotency-Key`
header, see [orders](orders).

The template describes the top-level properties of the request:

- `customerId` is required and must be a UUID, given as a `regex`. Its `options` link to the
  customers who can place the order, as a JSON array of `prompt` and `value` pairs, so a client
  can offer another customer than the one the form was filled in with. Exactly one is chosen.
- `items` is required. HAL-FORMS properties describe single values, so the template cannot say
  what a line item consists of.

The form also links with `describedby` to the JSON Schema of the request, which describes all of
it, including the line items: each refers to a product and orders at least one unit. A client
that changes the form, or builds a request of its own, can check it against the schema first.

## Related link rels

- [order](order) — the affordance for submitting the form, and the order it creates.
- [orders](orders) — the collection the new order is placed in.
- [customer](customer) — the kind of resource the form refers to as its customer.
- [product](product) — the kind of resource the form refers to on each line.

See also the guide [Placing an order](../guides/create-order).
