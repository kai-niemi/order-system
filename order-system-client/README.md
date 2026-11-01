# Order System Client

An interactive shell built with Spring Shell that demonstrates hypermedia-driven interaction with the 
Order System REST API.

The client knows only the API root URI (`app.api.root-uri`, default `http://localhost:8090/`). It discovers resources
and available operations by following links and affordances advertised by the server, without constructing resource URLs
or encoding order lifecycle rules.

The client uses HAL for resource navigation and HAL-FORMS for discovering how to perform operations.

## Getting Started

Start the server, then launch the client from the project root:

```bash
./run-client.sh
```

To connect to another server:

```bash
./run-client.sh --app.api.root-uri=http://otherhost:8090/
```

Type `help` to see the available commands.

## Link Relations

The client identifies links by their relation URI rather than their CURIE prefix.

The server declares the following CURIE:

```text
os → https://order-system.cloudneutral.se/rels/{rel}
```

For example, `os:products` expands to:

```text
https://order-system.cloudneutral.se/rels/products
```

The client matches the expanded relation URI, as defined in `OrderSystemRels`. The server can change the CURIE prefix
without affecting the client.

Registered relations, such as `next`, are used directly.

## Commands

### product list

Lists products by discovering the `products` relation from the API root.

The client follows `next` links across pages until the requested number of products has been collected. It never
constructs pagination URLs.

The `--limit` option controls the maximum number of products returned (default 10).

```text
order-system $ product list --limit 3

SKU              | Name                                   | Price  | Inventory
-----------------+----------------------------------------+--------+----------
SKU-250E649E-010 | Databases Unleashed - First Edition    | 150.00 | 10
SKU-C09A5173-E78 | The Complete Guide to SQL Transactions | 150.00 | 10
SKU-3434AE10-827 | Reliable Systems at Scale              | 150.00 | 10

3 of 100 products
```

### order create

Creates an order using a form and submission instructions discovered from the API.

The client performs the following steps:

1. Discovers the templated `orders-form` relation from the API root and expands its `{?products}` parameter using `--products` (default 1).
2. Retrieves the form using `application/prs.hal-forms+json`. The server provides a populated request containing a customer and product lines, together with HAL-FORMS templates describing available operations.
3. Discovers the `order` relation and selects the template associated with the order submission affordance.
4. Constructs the request body from the form's state and the template's properties, then submits it using the advertised HTTP method and content type.
5. Includes an `Idempotency-Key` header. The `--key` option specifies the key; otherwise, the client generates a random UUID.
6. Follows the created order's `items` relation to retrieve its line items.

```text
order-system $ order create --products 2

201 CREATED http://localhost:8090/api/orders/71b3e82d-75fe-4140-a2fe-ba378eaf4ab6
Status:          placed
Customer:        Sterling McMillan
Total price:     3000.00
Idempotency-Key: 9c73c83a-d071-4438-a3a5-6497da9b6b25 (replayed: false)

SKU              | Name                                    | Quantity | Unit price | Total
-----------------+-----------------------------------------+----------+------------+--------
SKU-5735478A-302 | Mastering Modern Java                   | 10       | 150.00     | 1500.00
SKU-445D25EC-EDF | Designing APIs for Long-Term Evolution | 10       | 150.00     | 1500.00
```

#### Idempotency

Each invocation retrieves a newly populated order form. Consequently, two invocations may produce different requests
even when they use the same `--products` value.

Reusing an idempotency key with a different request fingerprint results in `422 Unprocessable Content`.

Reusing a key with an identical request returns the previously created order without creating another one.

Because the shell retrieves a fresh form for each invocation, specifying the same `--key` does not necessarily reproduce
the original request.

See [Idempotency Semantics](../API.md#idempotency-semantics) for details.

### order cancel

Cancels an order using the cancellation operation advertised by the server.

An order can be identified by its URI, as printed by `order create`. If no URI is provided, the client uses the last
order created during the current shell session.

The client does not implement the order cancellation rules. Instead, it:

1. Retrieves the order representation using HAL-FORMS.
2. Checks whether the server advertises the `cancel` relation and its associated template.
3. If cancellation is available, submits the operation using the method and target specified by the template.
4. If cancellation is unavailable, reports the current order status without sending a cancellation request.

The cancellation template has no input properties, so the client submits a `POST` without a request body.

```text
order-system $ order cancel

200 OK http://localhost:8090/api/orders/a2f676e4-79bf-44a6-a4bc-2ad41f0f9e5c
Status:          placed -> cancelled
Updated:         2026-10-31T15:01:54.751575Z
Cancel offered:  false

order-system $ order cancel

Order is cancelled, the server offers no cancellation for it:
http://localhost:8090/api/orders/a2f676e4-79bf-44a6-a4bc-2ad41f0f9e5c
```

The absence of the cancellation affordance indicates that the operation is no longer available. The client does not need
to know which order states permit cancellation.

## Error Handling

The client reports API errors using the RFC 9457 Problem Details representations returned by the server.

See [API.md](../API.md) for the API's error responses and HTTP semantics.