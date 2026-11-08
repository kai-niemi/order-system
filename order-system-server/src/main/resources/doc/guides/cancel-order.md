# Cancelling an order

1. Request the [order](../rels/order) by its URI, as HAL-FORMS.
2. If the order has no [cancel](../rels/cancel) link, it cannot be cancelled in its current
   state. Stop there, there is nothing to submit.
3. Otherwise select the template that targets the `cancel` link, and submit it using the
   template's method and target, without a body.
4. The response is the updated order, now `cancelled`, and without a `cancel` link.

A client does not need to know when an order can be cancelled, only to look for the link. If
the order changed after it was read, the submission is rejected with
[order-not-cancellable](../problems/order-not-cancellable); read the order
again to see its current state.

## See also

- [Placing an order](create-order)
- [Order System API](index)
