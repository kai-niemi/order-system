# JSON Schema Profiles

The API exposes JSON Schema Draft 2020-12 documents describing
resource representations and request bodies.

Schemas are generated dynamically from the Java models using
[victools/jsonschema-generator](https://github.com/victools/jsonschema-generator).

| Profile | Schema |
|---|---|
| Order | [JSON Schema](schemas/order.json) |
| Order Item | [JSON Schema](schemas/order-item.json) |
| Order Request | [JSON Schema](schemas/order-request.json) |
| Product | [JSON Schema](schemas/product.json) |
| Customer | [JSON Schema](schemas/customer.json) |

## Runtime discovery

The running API advertises schemas through the `describedby`
link relation.

Clients can follow these links to retrieve the current schema
using the `application/schema+json` media type.

The published JSON files are snapshots generated during the
documentation build.