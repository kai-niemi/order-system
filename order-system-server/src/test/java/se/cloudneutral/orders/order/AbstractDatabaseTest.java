package se.cloudneutral.orders.order;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import se.cloudneutral.orders.ServerApplication;
import se.cloudneutral.orders.TestDataGenerator;
import se.cloudneutral.orders.TestDataService;
import se.cloudneutral.orders.api.payload.CreateOrderItemRequest;
import se.cloudneutral.orders.api.payload.CreateOrderRequest;
import se.cloudneutral.orders.domain.Customer;
import se.cloudneutral.orders.domain.Product;
import se.cloudneutral.orders.domain.PurchaseOrder;
import se.cloudneutral.orders.service.OrderService;

import java.util.List;

/**
 * Base class for integration tests that need the reference database (PostgreSQL). Every test
 * starts from a small, known data set: a few customers and products, no orders, no payment
 * attempts and no idempotency records.
 * <p>
 * The background jobs are slowed to once per day so that they never race the test. Their first
 * run happens when the context starts, before any test creates data. All subclasses use the same
 * properties so that they share one application context.
 */
@SpringBootTest(classes = {ServerApplication.class}, properties = {
        "app.simulation.processing-interval=PT24H",
        "app.idempotency.evict-interval=PT24H"
})
public abstract class AbstractDatabaseTest extends AbstractIntegrationTest {
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected TestDataService dataService;

    @Autowired
    protected OrderService orderService;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("delete from idempotency_record");
        dataService.deleteAllData();
        dataService.createCustomers(3, TestDataGenerator::randomCustomer);
        dataService.createProducts(3, TestDataGenerator::randomProduct);
    }

    protected Customer anyCustomer() {
        return orderService.findRandomCustomer().orElseThrow();
    }

    protected Product anyProduct() {
        return orderService.findProducts(PageRequest.ofSize(1)).getContent().getFirst();
    }

    protected CreateOrderRequest orderRequest(int quantity) {
        return new CreateOrderRequest(anyCustomer().getId(),
                List.of(new CreateOrderItemRequest(anyProduct().getId(), quantity)));
    }

    /**
     * Places an order directly through the service, bypassing the idempotency layer.
     *
     * @return the id of a new order in state PLACED
     */
    protected UUID placeOrder() {
        Product product = anyProduct();
        PurchaseOrder order = PurchaseOrder.builder()
                .withCustomer(anyCustomer())
                .andOrderItem()
                .withProductId(product.getId())
                .withProductSku(product.getSku())
                .withUnitPrice(product.getPrice())
                .withQuantity(1)
                .then()
                .build();
        return orderService.placeOrder(order).getId();
    }

    protected String orderStatus(UUID orderId) {
        return jdbcTemplate.queryForObject(
                "select status::text from purchase_order where id = ?", String.class, orderId);
    }

    protected long orderCount() {
        return jdbcTemplate.queryForObject("select count(*) from purchase_order", Long.class);
    }
}
