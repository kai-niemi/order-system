package se.cloudneutral.orders.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import se.cloudneutral.orders.annotation.TransactionMandatory;
import se.cloudneutral.orders.annotation.TransactionRequired;
import se.cloudneutral.orders.domain.Customer;
import se.cloudneutral.orders.domain.PurchaseOrder;
import se.cloudneutral.orders.domain.ShipmentStatus;
import se.cloudneutral.orders.repository.CustomerRepository;
import se.cloudneutral.orders.domain.Product;
import se.cloudneutral.orders.repository.ProductRepository;
import se.cloudneutral.orders.repository.OrderRepository;
import se.cloudneutral.orders.util.AssertUtils;

@Service
public class DefaultOrderService implements OrderService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public Page<Product> findProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Override
    public List<Product> findRandomProducts(int size) {
        return productRepository.findAllById(
                productRepository.findRandomProductIDs(size));
    }

    @Override
    public Page<Customer> findCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    @Override
    public Optional<Customer> findCustomerById(UUID id) {
        return customerRepository.findById(id);
    }

    @Override
    public Optional<Product> findProductById(UUID id) {
        return productRepository.findById(id);
    }

    @Override
    public Optional<Customer> findRandomCustomer() {
        return customerRepository.findById(
                customerRepository.findRandom(1).getFirst());
    }

    @Override
    public Page<PurchaseOrder> findOrders(Pageable pageable) {
        AssertUtils.assertNoTransaction();

        Page<UUID> ids = orderRepository.findOrderIds(pageable);

        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }

        List<PurchaseOrder> orders =
                orderRepository.findAllWithDetails(ids.getContent());

        Map<UUID, PurchaseOrder> byId = orders.stream()
                .collect(Collectors.toMap(PurchaseOrder::getId, Function.identity()));

        List<PurchaseOrder> sorted = ids.getContent().stream()
                .map(byId::get)
                .toList();

        return new PageImpl<>(sorted, pageable, ids.getTotalElements());
    }

    @Override
    public Optional<PurchaseOrder> findOrderById(UUID id) {
        return orderRepository.findById(id);
    }

    @Override
    public Optional<PurchaseOrder> findOrderDetailById(UUID id) {
        return orderRepository.findOrderDetailsById(id);
    }

    @Override
    @TransactionRequired
    public PurchaseOrder placeOrder(PurchaseOrder order) throws BusinessException {
        AssertUtils.assertReadWriteTransaction();

        try {
            // Update product inventories for each line item
            order.getOrderItems().forEach(orderItem -> {
                UUID productId = Objects.requireNonNull(orderItem.getProduct().getId());
                Product product = productRepository.getReferenceById(productId);
                product.addInventoryQuantity(-orderItem.getQuantity());
            });

            order.setStatus(ShipmentStatus.placed);
            order.setTotalPrice(order.subTotal());

            orderRepository.saveAndFlush(order); // flush to surface any constraint violations

            return order;
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Constraint violation in order placement", e);
        }
    }

    @Override
    @TransactionRequired
    public boolean cancelOrder(UUID id) {
        AssertUtils.assertReadWriteTransaction();

        // The preStatus guard is a compare-and-set, so a concurrent cancel or status change
        // leaves exactly one caller observing a non-zero row count.
        int rowsUpdated = orderRepository.updateOrderStatusById(
                id, ShipmentStatus.placed, ShipmentStatus.cancelled, OffsetDateTime.now());

        return rowsUpdated > 0;
    }
}
