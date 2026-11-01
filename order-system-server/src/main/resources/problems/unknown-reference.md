# Unknown resource reference

## Semantics

The request is well formed, but refers to a customer or product that does not exist. The
detail names the reference that could not be resolved. Status `422 Unprocessable Content`.
Nothing was placed.

## Resolution

Refer only to customers and products the API has provided, for example by submitting the state
of an [orders-form](../rels/orders-form) as it was received. Read a new form and send it with a
new `Idempotency-Key`.
