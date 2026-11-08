# Placing an order

1. From the API root, follow [orders-form](../rels/orders-form), expanding its `products`
   variable with the number of product lines you want. Request it as HAL-FORMS.
2. The form links to the [order](../rels/order) affordance. Select the template that targets
   that link.
3. Submit the state of the form, its properties without `_links` and `_templates`, using the
   template's method, target and content type. Send an `Idempotency-Key` header with a value
   that identifies this attempt.
4. The response is the new [order](../rels/order), with its URI in the `Location` header. Keep
   that URI, it is the identity of the order.

If the response is lost, repeat the same request with the same key: the order is placed at most
once, and the repeat returns the original order with `Idempotency-Replayed: true`. Only reuse a
key for the same request; a different request under the same key is rejected with
[idempotency-key-reused](../problems/idempotency-key-reused). See
[orders](../rels/orders) for the details.

The server decides the price of each line from the current product price, so the form carries
no prices, and anything a client adds is ignored.

## See also

- [Cancelling an order](cancel-order)
- [Order System API](index)
