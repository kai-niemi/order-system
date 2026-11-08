# Paying for and delivering an order

1. Request the [order](../rels/order) by its URI, as HAL-FORMS.
2. If the order has no [pay](../rels/pay) link, it cannot be paid in its current state. Stop
   there, there is nothing to submit.
3. Otherwise select the template that targets the `pay` link, and submit it using the
   template's method and target, without a body. Choose an `Idempotency-Key` for the attempt, and
   send the same key if the request has to be repeated.
4. The response is `202 Accepted` with a payment attempt, `pending`, and its URI in the
   `Location` header. The order is now `payment_pending`, and offers neither `pay` nor `cancel`.
5. Poll the payment attempt through its `self` link until its status is no longer `pending`.
6. Read the order again.
   - If the payment `succeeded`, the order is `paid` and has a [deliver](../rels/deliver) link.
   - If the payment was `declined`, the order is `placed` again with `pay` and `cancel` links,
     and can be paid with a new key.
   - If the payment is `unresolved`, the order is `payment_blocked` and offers no links. The
     outcome is not known, so do not pay again with a new key. Keep reading the attempt and the
     order: once the server has reconciled the payment, the order becomes `paid` or `placed`.
7. To deliver a `paid` order, select the template that targets the `deliver` link and submit it
   without a body. The response is `204 No Content`, and the order is now `delivered`.

A client does not need to know when an order can be paid or delivered, only to look for the
links. Repeating a payment with the same key does not start a second payment, and delivering an
order that is already delivered succeeds without any change, so both can be retried safely after
a lost response.

If the order is not in a state that allows the transition, the request is rejected with
[invalid-order-state](../problems/invalid-order-state); read the order again to see its current
state.

## See also

- [Placing an order](create-order)
- [Cancelling an order](cancel-order)
- [Order System API](index)
