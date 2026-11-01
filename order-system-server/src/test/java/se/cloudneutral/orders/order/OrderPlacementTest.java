package se.cloudneutral.orders.order;

import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import tools.jackson.dataformat.yaml.YAMLMapper;

import se.cloudneutral.orders.ServerApplication;
import se.cloudneutral.orders.TestDataGenerator;
import se.cloudneutral.orders.TestDataService;
import se.cloudneutral.orders.domain.Customer;
import se.cloudneutral.orders.domain.PurchaseOrder;
import se.cloudneutral.orders.domain.PurchaseOrderItem;
import se.cloudneutral.orders.domain.ShipmentStatus;
import se.cloudneutral.orders.domain.Product;
import se.cloudneutral.orders.service.BusinessException;
import se.cloudneutral.orders.service.OrderService;
import se.cloudneutral.orders.util.MetadataUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {ServerApplication.class})
public class OrderPlacementTest extends AbstractIntegrationTest {
    private static final int PRODUCT_COUNT = 5;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TestDataService dataService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private YAMLMapper yamlObjectMapper;

    @BeforeAll
    public void beforeAll() {
        String isolation = MetadataUtils.databaseIsolation(dataSource);
        assertThat(isolation.toUpperCase()).isEqualTo("SERIALIZABLE");

        dataService.deleteAllData();
        dataService.createCustomers(100, TestDataGenerator::randomCustomer);
        dataService.createProducts(25, TestDataGenerator::randomProduct);
    }

    @Order(1)
    @Test
    public void whenPlaceOrder_thenExpectSuccess() {
        Page<Product> productPage = orderService.findProducts(PageRequest.ofSize(PRODUCT_COUNT));
        Optional<Customer> customer = orderService.findRandomCustomer();

        assertThat(productPage).as("No products").isNotEmpty();
        assertThat(customer).as("No customers").isPresent();

        PurchaseOrder.Builder pb = PurchaseOrder.builder()
                .withCustomer(customer.orElseThrow());

        // Add one order item for each product and clean out inventory except for one item
        productPage.forEach(product -> pb.andOrderItem()
                .withProductId(product.getId())
                .withProductSku(product.getSku())
                .withUnitPrice(product.getPrice())
                .withQuantity(product.getInventory() - 1)
                .then());

        PurchaseOrder purchaseOrder = orderService.placeOrder(pb.build());

        assertThat(purchaseOrder).as("Expected detached entity").isNotNull();
    }

    @Order(2)
    @Test
    public void whenReadingOrder_thenExpectStatusUpdated() {
        Page<PurchaseOrder> orderPage = orderService.findOrders(PageRequest.ofSize(10));
        assertThat(orderPage.getTotalElements()).isEqualTo(1);

        PurchaseOrder purchaseOrder = orderPage.getContent().getFirst();
        assertThat(purchaseOrder.getStatus()).isEqualTo(ShipmentStatus.placed);
    }

    @Order(3)
    @Test
    public void whenReadingOrderLineProduct_thenExpectInventoryUpdated() {
        PurchaseOrder purchaseOrder = orderService.findOrders(PageRequest.ofSize(1))
                .toList().getFirst();

        List<PurchaseOrderItem> items = purchaseOrder.getOrderItems();
        assertThat(items).hasSize(PRODUCT_COUNT);

        items.forEach(purchaseOrderItem -> {
            assertThat(purchaseOrder.getTotalPrice()).isEqualTo(purchaseOrder.subTotal());
            Product product = purchaseOrderItem.getProduct();
            assertThat(product.getInventory()).isEqualTo(1); // One item was left
        });
    }

    @Order(4)
    @Test
    public void givenExistingOrder_whenPlaceNewOrder_thenExpectFailure() {
        PurchaseOrder purchaseOrder = orderService.findOrders(PageRequest.ofSize(1))
                .toList().getFirst();
        Optional<Customer> customer = orderService.findRandomCustomer();

        assertThat(customer).as("No customers").isNotEmpty();

        purchaseOrder.getOrderItems().forEach(purchaseOrderItem -> {
            Product product = purchaseOrderItem.getProduct();

            PurchaseOrder newOrder = PurchaseOrder.builder()
                    .withCustomer(customer.orElseThrow())
                    .andOrderItem()
                    .withProductId(product.getId())
                    .withProductSku(product.getSku())
                    .withUnitPrice(product.getPrice())
                    .withQuantity(2)
                    .then()
                    .build();

            assertThatThrownBy(() -> orderService.placeOrder(newOrder))
                    .isInstanceOf(BusinessException.class)
                    .cause()
                    .isInstanceOf(DataIntegrityViolationException.class);
        });
    }

    @Order(5)
    @Test
    public void givenQueryTransformation_whenListingAllOrderDetails_thenExpectJoinHints() {
        orderService.findOrders(PageRequest.of(0, 100)).forEach(this::printOrder);
    }

    private void printOrder(PurchaseOrder order) {
        logger.info("%n%s".formatted(
                yamlObjectMapper.writerFor(PurchaseOrder.class).writeValueAsString(order)));
    }
}


