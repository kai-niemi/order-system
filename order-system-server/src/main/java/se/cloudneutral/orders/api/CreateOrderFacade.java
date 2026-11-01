package se.cloudneutral.orders.api;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import se.cloudneutral.orders.annotation.TransactionMandatory;
import se.cloudneutral.orders.domain.Customer;
import se.cloudneutral.orders.service.OrderService;
import se.cloudneutral.orders.domain.PurchaseOrder;
import se.cloudneutral.orders.domain.Product;
import se.cloudneutral.orders.util.AssertUtils;
import se.cloudneutral.orders.api.payload.CreateOrderRequest;

@Service
public class CreateOrderFacade {
    @Autowired
    private OrderService orderService;

    @TransactionMandatory
    public PurchaseOrder createOrder(UUID orderId, CreateOrderRequest request) {
        AssertUtils.assertReadWriteTransaction();

        Customer customer = orderService.findCustomerById(request.customerId())
                .orElseThrow(() -> NoSuchResourceException.customer(request.customerId()));

        PurchaseOrder.Builder builder = PurchaseOrder.builder()
                .withId(orderId)
                .withCustomer(customer);

        request.items().forEach(item -> {
            Product product = orderService.findProductById(item.productId())
                    .orElseThrow(() -> NoSuchResourceException.product(item.productId()));

            builder.andOrderItem()
                    .withProductId(product.getId())
                    .withProductSku(product.getSku())
                    .withUnitPrice(product.getPrice())
                    .withQuantity(item.quantity())
                    .then();
        });

        return orderService.placeOrder(builder.build());
    }
}
