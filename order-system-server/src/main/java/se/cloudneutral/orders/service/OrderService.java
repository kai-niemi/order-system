package se.cloudneutral.orders.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import se.cloudneutral.orders.domain.Customer;
import se.cloudneutral.orders.domain.PurchaseOrder;
import se.cloudneutral.orders.domain.ShipmentStatus;
import se.cloudneutral.orders.domain.Product;

public interface OrderService {
    Page<Product> findProducts(Pageable pageable);

    List<Product> findRandomProducts(int size);

    Optional<Product> findProductById(UUID id);

    Page<Customer> findCustomers(Pageable pageable);

    Optional<Customer> findCustomerById(UUID id);

    Optional<Customer> findRandomCustomer();

    Page<PurchaseOrder> findOrders(Pageable pageable);

    Optional<PurchaseOrder> findOrderById(UUID id);

    Optional<PurchaseOrder> findOrderDetailById(UUID id);

    PurchaseOrder placeOrder(PurchaseOrder order);

    /**
     * Cancels an order, provided it is still in {@link ShipmentStatus#placed}.
     *
     * @return {@code true} if the order was cancelled, {@code false} if no such order exists or it
     * was no longer cancellable
     */
    boolean cancelOrder(UUID id);
}
