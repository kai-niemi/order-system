# Order System API

The API is a hypermedia API using [HAL](https://datatracker.ietf.org/doc/html/draft-kelly-json-hal)
and [HAL-FORMS](https://rwcbook.github.io/hal-forms/). A client knows one URI, the API root,
and reaches everything else by following the links in the representations it receives. Resource
addresses are not part of the API and may change; the relations are.

## Relations

Relations defined by this API are URIs under `https://order-system.cloudneutral.se/rels/`.
Representations abbreviate them with a curie, such as `os:orders`, declared in their `curies`
link. Expand the curie and compare the full URI rather than the prefix, which may change.
Relations without a prefix, such as `self` and `next`, are the registered relations of
[RFC 8288](https://www.iana.org/assignments/link-relations/link-relations.xhtml).

The root links to:

- [orders](../rels/orders) — the purchase orders, and where new orders are placed.
- [orders-form](../rels/orders-form) — a filled-in form for placing an order.
- [products](../rels/products) — the products available to order.
- [customers](../rels/customers) — the customers who can place orders.
- [actuators](../rels/actuators) — operational endpoints, not part of the order API.
- `help` — this guide.

## Representations

Ask for `application/hal+json` for the state and links of a resource. Ask for
`application/prs.hal-forms+json` to also receive `_templates`: the state transitions that are
legal right now, with the method, target and content type to use. Use the template rather than
assuming how to perform a transition, and treat a transition missing from the representation as
not available.

## Schemas

A representation links to a [JSON Schema](https://json-schema.org/draft/2020-12) of its state with
the registered `describedby` relation. The schema gives the name, type, format and meaning of
each property, which values it can take, and which are required. It covers the state only: the
`_links`, `_embedded` and `_templates` of a representation are defined by HAL and HAL-FORMS, so a
schema neither lists nor forbids them.

A schema is identified by its `$id`, a URI under `https://order-system.cloudneutral.se/profiles/`
that does not depend on where the server is deployed. The `describedby` link points to where the
server serves the schema, as `application/schema+json`.

The order form is described by the schema of the request it is filled in with, so a client can
check a request before submitting it.

Templates and schemas complement each other. A HAL-FORMS template says what can be done now, and
describes each top-level property: whether it is required, its pattern, and where to find the
values it can take, as `options`. HAL-FORMS properties describe single values, so structure
nested within a property, such as the line items of an order, is only described by the schema.

## Problems

Errors are returned as [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem details,
`application/problem+json`. Errors specific to this API have a `type`: a URI under
`https://order-system.cloudneutral.se/problems/`, documented on the server under `/problems/`.
Like a relation, the type does not depend on where the server is deployed, so switch on the
type rather than on the status code or the `detail` text, which is meant for people. A problem
without a `type` means no more than its status code.

- [idempotency-key-required](../problems/idempotency-key-required)
- [idempotency-key-reused](../problems/idempotency-key-reused)
- [idempotency-record-lost](../problems/idempotency-record-lost)
- [unknown-reference](../problems/unknown-reference)
- [order-rejected](../problems/order-rejected)
- [order-not-cancellable](../problems/order-not-cancellable)

## Guides

- [Placing an order](create-order)
- [Cancelling an order](cancel-order)
