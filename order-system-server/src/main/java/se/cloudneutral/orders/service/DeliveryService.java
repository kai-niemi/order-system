package se.cloudneutral.orders.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import se.cloudneutral.orders.annotation.TransactionBoundary;
import se.cloudneutral.orders.api.ResourceNotFoundException;
import se.cloudneutral.orders.domain.PurchaseOrder;
import se.cloudneutral.orders.domain.ShipmentStatus;
import se.cloudneutral.orders.repository.OrderRepository;

@Service
public class DeliveryService {
    private final OrderRepository orderRepository;

    public DeliveryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Marks a paid order as delivered. Naturally idempotent, since the target state is the
     * outcome: repeating the call for an order that is already delivered is a no-op, so a retry
     * after a lost response succeeds without needing an idempotency key.
     *
     * @throws ResourceNotFoundException                                     if the order does not exist
     * @throws se.cloudneutral.orders.domain.InvalidStateTransitionException if the order is not paid
     */
    @TransactionBoundary
    public void deliver(UUID orderId) {
        PurchaseOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> ResourceNotFoundException.order(orderId));

        if (order.getStatus() == ShipmentStatus.DELIVERED) {
            return;
        }

        order.deliver();
    }
}
