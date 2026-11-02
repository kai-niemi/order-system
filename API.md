<!-- TOC -->
* [Order System REST API](#order-system-rest-api)
  * [Introduction](#introduction)
    * [Link Relations and CURIEs](#link-relations-and-curies)
    * [Guides](#guides)
    * [Resource Schemas](#resource-schemas)
    * [Content Negotiation](#content-negotiation)
  * [Discover the API](#discover-the-api)
  * [Browse Products and Customers](#browse-products-and-customers)
  * [Create an Order](#create-an-order)
    * [1. Discover the Order Form](#1-discover-the-order-form)
    * [2. Retrieve the Form](#2-retrieve-the-form)
    * [3. Submit the Order](#3-submit-the-order)
      * [Using HAL-FORMS](#using-hal-forms)
      * [Direct HTTP Alternative](#direct-http-alternative)
    * [4. Successful Response](#4-successful-response)
  * [Idempotency](#idempotency)
    * [Retrying the Same Request](#retrying-the-same-request)
    * [Reusing a Key with a Different Request](#reusing-a-key-with-a-different-request)
    * [Idempotency Semantics](#idempotency-semantics)
  * [Navigate Orders](#navigate-orders)
    * [Order Line Items](#order-line-items)
  * [Cancel an Order](#cancel-an-order)
    * [Discover the Cancellation Affordance](#discover-the-cancellation-affordance)
    * [Perform the Cancellation](#perform-the-cancellation)
    * [State-Dependent Operations](#state-dependent-operations)
  * [Error Responses](#error-responses)
    * [Example: Missing Idempotency Key](#example-missing-idempotency-key)
    * [Application Error Types](#application-error-types)
  * [Interactive Hypermedia Client](#interactive-hypermedia-client)
<!-- TOC -->

# Order System REST API

The Order System exposes a REST API built with [Spring HATEOAS](https://docs.spring.io/spring-hateoas/docs/current/reference/html/),
using [HAL](https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-08) for resource representations and [HAL-FORMS](https://rwcbook.github.io/hal-forms/) for describing available operations.

The API demonstrates how clients can discover resources, navigate relationships, and perform state-dependent operations
through hypermedia controls rather than relying on hardcoded endpoint URLs.

## Introduction

REST's uniform interface allows clients to interact with resources through a consistent set of HTTP methods and
representations.

Hypermedia controls provide the information clients need to navigate the application and discover available operations.

The API uses several complementary mechanisms:

- **Links (`_links`)** identify related resources and navigable targets. Link relations describe the meaning of those
  relationships.
- **Affordances** describe operations available on resources, including HTTP methods, expected inputs, and submission
  targets.
- **HAL-FORMS (`_templates`)** represents those affordances in responses, allowing clients to discover how to submit
  requests.
- **CURIEs (Compact URIs)** provide shorthand identifiers for domain-specific link relations.
- **JSON Schema** describes the structure and constraints of resource representations and order submission requests.
- **Problem Details** provides machine-readable error responses using RFC 9457.

Hypermedia controls describe the operations available when a representation is generated. They do not replace
server-side validation. An operation advertised in one response may become unavailable before the client invokes it.

### Link Relations and CURIEs

Domain-specific link relations use the `os` CURIE prefix:

```text
os → https://order-system.cloudneutral.se/rels/{rel}
```

For example:

```text
os:products
```

expands to:

```text
https://order-system.cloudneutral.se/rels/products
```

The expanded URI identifies the relation independently of the server's deployment location.

Clients should match relations using their expanded URI rather than the CURIE prefix, which the server is free to
change.

The server also provides documentation for each relation at `/rels/{rel}`.

For example:

```bash
curl -H 'Accept: text/html' \
  http://localhost:8090/rels/products
```

Or retrieve the Markdown representation:

```bash
curl -H 'Accept: text/markdown' \
  http://localhost:8090/rels/products
```

The canonical relation URI identifies the relation, while the server-local endpoint provides its documentation. Clients
should not assume that the canonical URI resolves to the local documentation endpoint.

Registered link relations, such as `self`, `next`, `help`, and `describedby`, are used without a CURIE prefix.

### Guides

Workflows involving several resources and operations are documented under:

```text
/guides/{guide}
```

The API root advertises the guide index using the registered `help` relation.

### Resource Schemas

Orders, line items, products, customers, and order forms advertise a JSON Schema using the registered `describedby`
relation.

Schemas are served from:

```text
/profiles/{profile}
```

with the media type:

```text
application/schema+json
```

Each schema has a stable `$id` under:

```text
https://order-system.cloudneutral.se/profiles/
```

The order form schema describes the request representation used to submit an order, allowing clients to validate the
request before sending it.

### Content Negotiation

The API supports the following representation formats:

| Media type                       | Purpose                                                  |
|----------------------------------|----------------------------------------------------------|
| `application/hal+json`           | Resource representations and links                       |
| `application/prs.hal-forms+json` | Resource representations, links, and operation templates |
| `application/json`               | JSON representations                                     |
| `application/problem+json`       | RFC 9457 error responses                                 |

Examples below assume the server is running on `localhost:8090`.

The examples use [jq](https://jqlang.github.io/jq/) to format and extract JSON. Install it before running the commands.

---

## Discover the API

The API root is the starting point for resource discovery.

```bash
curl -s http://localhost:8090/ | jq
```

Response excerpt (unrelated properties and links omitted):

```json
{
  "_links": {
    "os:orders": {
      "href": "http://localhost:8090/api/orders"
    },
    "os:orders-form": {
      "href": "http://localhost:8090/api/orders/form{?products}",
      "templated": true
    },
    "curies": [
      {
        "name": "os",
        "href": "https://order-system.cloudneutral.se/rels/{rel}",
        "templated": true
      }
    ]
  }
}
```

The root representation advertises the resources and workflows available to clients.

A client can discover the orders collection, products, customers, order form, and API documentation without knowing
their URLs beforehand.

The following examples demonstrate both hypermedia-driven navigation and direct HTTP requests. Direct URLs are used
where they make an example easier to reproduce.

## Browse Products and Customers

Products and customers are exposed as paginated collections.

A client discovers their collection links from the API root.

For example, retrieve the products collection URL:

```bash
curl -s http://localhost:8090/ |
  jq -r '._links["os:products"].href'
```

Follow the discovered link:

```bash
PRODUCTS_URL=$(
  curl -s http://localhost:8090/ |
    jq -r '._links["os:products"].href'
)

curl -s "$PRODUCTS_URL" | jq
```

Collections advertise pagination controls using registered link relations:

- `first`
- `prev`
- `next`
- `last`

They also contain a `page` block describing pagination metadata.

For example:

```bash
curl -s 'http://localhost:8090/api/customers?page=0&size=5' | jq
curl -s 'http://localhost:8090/api/products?page=0&size=5' | jq
```

A hypermedia client follows the advertised `next` relation rather than constructing pagination URLs.

---

## Create an Order

Order creation demonstrates how a client can discover an operation and submit a request using server-provided hypermedia
controls.

The workflow consists of:

1. Discovering the order form from the API root.
2. Retrieving the form as HAL-FORMS.
3. Submitting the order using the advertised operation.
4. Following links in the created order representation.

### 1. Discover the Order Form

The API root advertises a templated `os:orders-form` relation:

```json
{
  "os:orders-form": {
    "href": "http://localhost:8090/api/orders/form{?products}",
    "templated": true,
    "title": "Purchase order form"
  }
}
```

The `{?products}` parameter specifies how many product lines the generated form should contain.

A client expands this URI template using the desired number of products. For example, requesting a form with one product
produces:

```text
http://localhost:8090/api/orders/form?products=1
```

URI template expansion follows RFC 6570. The client does not need to know how the server constructs the underlying
resource path.

### 2. Retrieve the Form

Request the form using HAL-FORMS and save the response for the next steps:

```bash
curl -sS \
  -H 'Accept: application/prs.hal-forms+json' \
  'http://localhost:8090/api/orders/form?products=1' \
  > order-form.json
```

The response contains a populated request representation, including a customer and product lines, together with
hypermedia controls describing how the form can be submitted.

Inspect the advertised templates:

```bash
jq '._templates' order-form.json
```

The `createOrder` template describes the submission operation:

```json
{
  "_templates": {
    "createOrder": {
      "method": "POST",
      "target": "http://localhost:8090/api/orders",
      "contentType": "application/json",
      "properties": [
        {"name": "customerId", "required": true},
        {"name": "items", "required": true}
      ]
    }
  }
}
```

The example above shows the essential submission details. The actual template also provides additional metadata,
including a link for discovering available customer selections.

The template tells the client which HTTP method to use, where to submit the request, which content type to send, and
which request properties are expected.

The submission endpoint does not need to be known in advance.

### 3. Submit the Order

The client constructs the request from the form's state and the advertised template.

The form representation contains hypermedia metadata that is not part of the submission request.

Extract the request state:

```bash
jq 'del(._links, ._templates)' order-form.json > order.json
```

#### Using HAL-FORMS

Read the submission details from the `createOrder` template:

```bash
METHOD=$(jq -r '._templates.createOrder.method' order-form.json)
TARGET=$(jq -r '._templates.createOrder.target' order-form.json)
CONTENT_TYPE=$(jq -r '._templates.createOrder.contentType' order-form.json)
```

Submit the request using the discovered method, target, and content type:

```bash
curl -sS \
  -X "$METHOD" \
  "$TARGET" \
  -H "Content-Type: $CONTENT_TYPE" \
  -H 'Accept: application/hal+json' \
  -H 'Idempotency-Key: example-order-001' \
  -d @order.json | jq
```

The client obtains the operation's HTTP details from the representation rather than hardcoding them.

This example assumes the known `createOrder` template and uses the form's populated state as the request body. A
general-purpose HAL-FORMS client would also interpret the template's property definitions and construct the request
accordingly.

The interactive [Order System Client](order-system-client/README.md) demonstrates the complete discovery and submission workflow.

#### Direct HTTP Alternative

The same order can be submitted using a conventional HTTP request:

```bash
curl -sS -X POST \
  'http://localhost:8090/api/orders' \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/hal+json' \
  -H 'Idempotency-Key: example-order-002' \
  -d @order.json | jq
```

This performs the same operation as the HAL-FORMS example, but the client must already know the HTTP method and
submission URL.

The direct approach is useful for testing and exploring the API with tools such as `curl`.

In both cases, the request does not include product prices.

The server resolves each line item's unit price from the current product price when the order is created. This prevents
clients from determining the price charged by submitting arbitrary values.

The two submission examples are alternatives. Running both with their different idempotency keys creates two separate
orders.

### 4. Successful Response

A successful submission returns `201 Created`.

The `Location` header identifies the created order resource, and the response body contains its HAL representation.

```http
HTTP/1.1 201 Created
Location: http://localhost:8090/api/orders/eb820abd-3b44-40bf-ba52-0f28c60e63f2
Content-Type: application/hal+json
```

Response excerpt (unrelated properties and links omitted):

```json
{
  "id": "eb820abd-3b44-40bf-ba52-0f28c60e63f2",
  "status": "placed",
  "_links": {
    "self": {
      "href": "http://localhost:8090/api/orders/eb820abd-3b44-40bf-ba52-0f28c60e63f2"
    },
    "items": {
      "href": "http://localhost:8090/api/orders/eb820abd-3b44-40bf-ba52-0f28c60e63f2/items"
    },
    "customer": {
      "href": "http://localhost:8090/api/customers/30ff5481-4b92-46b1-948f-3c7194acf50c"
    }
  }
}
```

The order representation advertises its relationships through links.

Clients can navigate to the order's line items, customer, or orders collection without constructing URLs.

The line items are not embedded in the order representation. They are exposed as a separate collection through the
`items` relation.

Timestamps are offset-aware. PostgreSQL's `timestamptz` stores instants, which are represented in UTC by this API.

---

## Idempotency

Order creation requires an `Idempotency-Key` header.

The key identifies a logical submission attempt and allows clients to retry a request without creating duplicate orders.

The server associates the key with a fingerprint of the request.

### Retrying the Same Request

Submit the same request multiple times using the same key:

```bash
curl -vsS -X POST \
  'http://localhost:8090/api/orders' \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: retry-example-001' \
  -d @order.json |
  jq
```

Repeat the command without changing the request body or key.

The first request creates an order.

Subsequent requests return the existing order rather than creating another.

The response includes an `Idempotency-Replayed` header indicating whether the request reused a previously completed
operation.

The API stores the created order's identifier, not a serialized copy of the original HTTP response.

Consequently, a replay retrieves the current representation of the existing order. It does not guarantee byte-for-byte
reproduction of the original response.

### Reusing a Key with a Different Request

A key cannot be reused for a different request while its idempotency record is active.

To demonstrate this, first submit `order.json`:

```bash
curl -sS -X POST \
  'http://localhost:8090/api/orders' \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: mismatch-example-001' \
  -d @order.json |
  jq
```

Create a modified copy of the request:

```bash
jq '.items[0].quantity += 1' order.json > modified-order.json
```

Submit the modified request with the same key:

```bash
curl -sS -X POST \
  'http://localhost:8090/api/orders' \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: mismatch-example-001' \
  -d @modified-order.json |
  jq
```

The server rejects the second request with `422 Unprocessable Content` because the fingerprint no longer matches the
original submission.

### Idempotency Semantics

The implementation uses the following rules:

- **Key scope:** Keys are scoped to the authenticated principal, or `anonymous` when no principal is available. Requests
  sharing the same principal and key refer to the same logical submission.
- **Fingerprint:** A SHA-256 fingerprint covers the HTTP method, path, and canonicalized JSON request body.
  Canonicalization sorts object keys, treats absent and null values equivalently, and normalizes numeric values.
- **Atomicity:** The order identifier is generated before the idempotency key is claimed. The identifier and fingerprint
  are stored together when the key is claimed. The idempotency claim and order creation participate in the same database
  transaction and commit or roll back together. If the initial operation fails before committing, its idempotency claim
  is rolled back and the key becomes available again. A request rejected because its fingerprint differs from an
  existing committed record does not modify that record.
- **Concurrency:** A request whose key is claimed by another request still in progress waits for that request to finish.
  If the other request commits, the waiting request replays its order, or is rejected with `422` if its body differs.
  If the other request rolls back, the waiting request claims the key and places the order itself. In either case, at
  most one order is committed for a given key within its scope and retention window.
- **Isolation:** This holds under both `READ COMMITTED` and `SERIALIZABLE` isolation. Under SERIALIZABLE, a competing
  request may encounter a serialization failure (SQLSTATE 40001) when the other transaction commits. The server retries
  the entire transaction transparently, allowing the retry to observe the committed idempotency record and replay the
  existing order.
- **Retention:** Records remain eligible for replay until their configured expiration. Once expired, a key can be reused
  for a new request.
- **Cleanup:** Expired records are removed by the scheduled `IdempotencyEvictor`, in batches that each run in a short SQL
  transaction of their own. Batches use FOR UPDATE SKIP LOCKED to avoid waiting on rows locked by concurrent requests or
  other cleanup workers.

Cleanup behavior is configurable through:

```text
app.idempotency.evict-interval
app.idempotency.evict-batch
```

PostgreSQL is the reference database. The implementation does not depend on a database-specific automatic TTL mechanism.

The retention period defines the replay eligibility window. It should not be interpreted as a guarantee about the
duration of in-flight requests.

---

## Navigate Orders

Clients can retrieve an order using its `self` relation or the `Location` header returned when the order was created.

For direct access:

```bash
curl -s \
  'http://localhost:8090/api/orders/{id}' |
  jq
```

Orders can also be discovered through the orders collection:

```bash
curl -s \
  'http://localhost:8090/api/orders?page=0&size=10' |
  jq
```

The collection provides pagination controls in the same way as the products and customers collections.

### Order Line Items

The order's `items` relation identifies its line-item collection.

For example:

```bash
curl -s \
  'http://localhost:8090/api/orders/{id}/items' |
  jq
```

Response excerpt (unrelated properties and links omitted):

```json
{
  "_embedded": {
    "orderItems": [
      {
        "quantity": 2,
        "_links": {
          "self": {
            "href": "http://localhost:8090/api/orders/{id}/items/0"
          },
          "product": {
            "href": "http://localhost:8090/api/products/{id}"
          }
        }
      }
    ]
  }
}
```

Each line item has its own `self` relation and a `product` relation identifying the associated product.

Line items are addressed by their position in the order:

```text
/api/orders/{id}/items/{position}
```

Their positions remain stable for the lifetime of the order because the order's line-item collection is not modified
after creation.

A request for a position outside the collection returns `404 Not Found`.

For example:

```bash
curl -s \
  'http://localhost:8090/api/orders/{id}/items/0' |
  jq
```

---

## Cancel an Order

Cancellation demonstrates state-dependent hypermedia affordances.

An order can be cancelled while its status is `placed`.

Rather than requiring clients to implement this lifecycle rule, the server advertises the cancellation operation only
when it is currently available.

### Discover the Cancellation Affordance

Retrieve the order using HAL-FORMS:

```bash
curl -s \
  -H 'Accept: application/prs.hal-forms+json' \
  'http://localhost:8090/api/orders/{id}' |
  jq
```

When cancellation is available, the representation includes a `cancel` relation and an associated HAL-FORMS template.

For example:

```json
{
  "status": "placed",
  "_links": {
    "cancel": {
      "href": "http://localhost:8090/api/orders/eb820abd-3b44-40bf-ba52-0f28c60e63f2/cancel"
    }
  },
  "_templates": {
    "cancel": {
      "method": "POST",
      "properties": [],
      "target": "http://localhost:8090/api/orders/eb820abd-3b44-40bf-ba52-0f28c60e63f2/cancel"
    }
  }
}
```

The template identifies the HTTP method and submission target.

Because the template contains no properties, the operation requires no request body.

### Perform the Cancellation

The client submits the operation using the advertised method and target.

The client retrieves the order representation and executes the operation advertised by its HAL-FORMS template:

```bash
curl -sS \
  -H 'Accept: application/prs.hal-forms+json' \
  'http://localhost:8090/api/orders/{id}' \
  > order-response.json

jq -e '._templates.cancel' order-response.json > /dev/null ||
  { echo "Cancellation is not available"; exit 1; }
  
METHOD=$(jq -r '._templates.cancel.method' order-response.json)
TARGET=$(jq -r '._templates.cancel.target' order-response.json)

curl -sS -X "$METHOD" "$TARGET" | jq
```

The server responds with `200 OK` and the updated order representation.

The order's status changes from `placed` to `cancelled`, and `dateUpdated` advances.

### State-Dependent Operations

Retrieving the cancelled order again using HAL-FORMS shows that the `cancel` affordance is no longer available.

The client can determine that cancellation is unavailable without knowing the order's lifecycle rules.

However, hypermedia controls represent the state observed when the response was generated. They do not guarantee that an
operation will remain available.

The server validates the cancellation request again when it is processed.

Consequently:

- Cancelling an eligible order returns `200 OK`.
- Cancelling an order that is no longer eligible returns `409 Conflict`.
- Cancelling an unknown order returns `404 Not Found`.

This protects against concurrent state changes between retrieving the representation and invoking the operation.

Clients requesting `application/hal+json` receive links without HAL-FORMS templates.

---

## Error Responses

The API uses [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457.html) for error responses.

Errors are returned using:

```text
application/problem+json
```

Application-specific errors have stable type URIs under:

```text
https://order-system.cloudneutral.se/problems/
```

For example:

```text
https://order-system.cloudneutral.se/problems/idempotency-key-required
```

The type URI identifies the error independently of the server's deployment location.

Clients can use it to distinguish error conditions without parsing human-readable `detail` messages.

The server provides documentation for each problem type at:

```text
/problems/{type}
```

For example:

```bash
curl \
  http://localhost:8090/problems/idempotency-key-reused
```

When a problem's `type` member is omitted, RFC 9457 defines its effective value as `about:blank`. This indicates a
generic problem whose semantics are conveyed by the HTTP status code.

### Example: Missing Idempotency Key

Submit an order without the required header:

```bash
curl -sS -X POST \
  'http://localhost:8090/api/orders' \
  -H 'Content-Type: application/json' \
  -d @order.json |
  jq
```

Example response:

```json
{
  "type": "https://order-system.cloudneutral.se/problems/idempotency-key-required",
  "title": "Idempotency key required",
  "status": 400,
  "detail": "Placing an order requires an 'Idempotency-Key' header identifying the attempt",
  "instance": "/api/orders"
}
```

### Application Error Types

| Type                       | HTTP status                 | Condition                                                                       |
|----------------------------|-----------------------------|---------------------------------------------------------------------------------|
| `idempotency-key-required` | `400 Bad Request`           | An order is submitted without an `Idempotency-Key` header.                      |
| `idempotency-key-reused`   | `422 Unprocessable Content` | The key was previously used for a different request.                            |
| `idempotency-record-lost`  | `409 Conflict`              | The outcome is unknown. The client should repeat the request with the same key. |
| `unknown-reference`        | `422 Unprocessable Content` | The request references a customer or product that does not exist.               |
| `order-rejected`           | `409 Conflict`              | The order violates a business rule, such as exceeding available stock.          |
| `order-not-cancellable`    | `409 Conflict`              | The order is no longer in a state that permits cancellation.                    |

Clients should treat the problem type URI as the machine-readable identifier and the `detail` member as diagnostic
information intended for humans.

---

## Interactive Hypermedia Client

The project includes an interactive client built with Spring Shell.

Unlike the direct HTTP examples in this document, the client discovers resources and operations through links and
HAL-FORMS templates.

It supports browsing products, creating orders, and cancelling orders without constructing resource URLs.

Start it from the project root:

```bash
./run-client.sh
```

See the [client documentation](order-system-client/README.md) for command examples and implementation details.

---