package se.cloudneutral.orders.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import se.cloudneutral.orders.TestDataGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit-test")
class PurchaseOrderStateTest {
    private static PurchaseOrder order(ShipmentStatus status) {
        PurchaseOrder order = PurchaseOrder.builder()
                .withCustomer(TestDataGenerator.randomCustomer())
                .andOrderItem()
                .withProductId(UUID.randomUUID())
                .withProductSku("sku")
                .withUnitPrice(BigDecimal.TEN)
                .withQuantity(1)
                .then()
                .build();
        order.setStatus(status);
        return order;
    }

    @Test
    void givenNewOrder_thenExpectPlaced() {
        assertThat(PurchaseOrder.builder()
                .withCustomer(TestDataGenerator.randomCustomer())
                .andOrderItem()
                .withProductId(UUID.randomUUID())
                .withProductSku("sku")
                .withUnitPrice(BigDecimal.TEN)
                .withQuantity(1)
                .then()
                .build()
                .getStatus()).isEqualTo(ShipmentStatus.PLACED);
    }

    @Test
    void givenPaymentPending_whenPaymentSucceeded_thenExpectPaid() {
        PurchaseOrder order = order(ShipmentStatus.PAYMENT_PENDING);

        order.paymentSucceeded();

        assertThat(order.getStatus()).isEqualTo(ShipmentStatus.PAID);
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, names = "PAYMENT_PENDING", mode = EnumSource.Mode.EXCLUDE)
    void givenOtherState_whenPaymentSucceeded_thenExpectInvalidTransition(ShipmentStatus status) {
        PurchaseOrder order = order(status);

        assertThatThrownBy(order::paymentSucceeded).isInstanceOf(InvalidStateTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(status);
    }

    @Test
    void givenPaymentPending_whenPaymentDeclined_thenExpectPlaced() {
        PurchaseOrder order = order(ShipmentStatus.PAYMENT_PENDING);

        order.paymentDeclined();

        assertThat(order.getStatus()).isEqualTo(ShipmentStatus.PLACED);
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, names = "PAYMENT_PENDING", mode = EnumSource.Mode.EXCLUDE)
    void givenOtherState_whenPaymentDeclined_thenExpectInvalidTransition(ShipmentStatus status) {
        PurchaseOrder order = order(status);

        assertThatThrownBy(order::paymentDeclined).isInstanceOf(InvalidStateTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(status);
    }

    @Test
    void givenPaid_whenDeliver_thenExpectDelivered() {
        PurchaseOrder order = order(ShipmentStatus.PAID);

        order.deliver();

        assertThat(order.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, names = "PAID", mode = EnumSource.Mode.EXCLUDE)
    void givenOtherState_whenDeliver_thenExpectInvalidTransition(ShipmentStatus status) {
        PurchaseOrder order = order(status);

        assertThatThrownBy(order::deliver).isInstanceOf(InvalidStateTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(status);
    }
}
