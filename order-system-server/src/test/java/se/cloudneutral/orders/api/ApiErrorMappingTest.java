package se.cloudneutral.orders.api;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import se.cloudneutral.orders.AbstractDatabaseTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The full web stack against the reference database: headers, idempotency, problem details and
 * the status codes of state conflicts and missing resources.
 */
public class ApiErrorMappingTest extends AbstractDatabaseTest {
    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private String orderBody(int quantity) {
        return """
                {"customerId":"%s","items":[{"productId":"%s","quantity":%d}]}
                """.formatted(anyCustomer().getId(), anyProduct().getId(), quantity);
    }

    private void forceStatus(UUID orderId, String status) {
        jdbcTemplate.update("update purchase_order set status = ?::shipment_status where id = ?",
                status, orderId);
    }

    // Placing orders

    @Test
    void givenNoIdempotencyKey_whenPlaceOrder_thenExpectTypedBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ProblemTypes.IDEMPOTENCY_KEY_REQUIRED.toString()));

        assertThat(orderCount()).isZero();
    }

    @Test
    void givenSameKeyAndBody_whenPlaceOrderTwice_thenExpectReplayAndOneOrder() throws Exception {
        String body = orderBody(1);

        String location = mockMvc.perform(post("/api/orders")
                        .header(WebConstants.HEADER_IDEMPOTENCY_KEY, "order-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string(WebConstants.HEADER_IDEMPOTENCY_REPLAYED, "false"))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(post("/api/orders")
                        .header(WebConstants.HEADER_IDEMPOTENCY_KEY, "order-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string(WebConstants.HEADER_IDEMPOTENCY_REPLAYED, "true"))
                .andExpect(header().string("Location", location));

        assertThat(orderCount()).isEqualTo(1);
    }

    @Test
    void givenSameKeyAndOtherBody_whenPlaceOrder_thenExpectKeyReusedProblem() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header(WebConstants.HEADER_IDEMPOTENCY_KEY, "order-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                        .header(WebConstants.HEADER_IDEMPOTENCY_KEY, "order-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(2)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value(ProblemTypes.IDEMPOTENCY_KEY_REUSED.toString()));

        assertThat(orderCount()).isEqualTo(1);
    }

    // Paying

    @Test
    void givenNoIdempotencyKey_whenPayOrder_thenExpectTypedBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders/{id}/payments", placeOrder()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ProblemTypes.IDEMPOTENCY_KEY_REQUIRED.toString()));
    }

    @Test
    void givenSameKey_whenPayOrderTwice_thenExpectReplayAndOneAttempt() throws Exception {
        UUID orderId = placeOrder();

        mockMvc.perform(post("/api/orders/{id}/payments", orderId)
                        .header(WebConstants.HEADER_IDEMPOTENCY_KEY, "pay-1"))
                .andExpect(status().isAccepted())
                .andExpect(header().string(WebConstants.HEADER_IDEMPOTENCY_REPLAYED, "false"));

        mockMvc.perform(post("/api/orders/{id}/payments", orderId)
                        .header(WebConstants.HEADER_IDEMPOTENCY_KEY, "pay-1"))
                .andExpect(status().isAccepted())
                .andExpect(header().string(WebConstants.HEADER_IDEMPOTENCY_REPLAYED, "true"));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from payment_attempt where order_id = ?", Long.class, orderId))
                .isEqualTo(1);
    }

    @Test
    void givenSameKeyAndOtherSimulationFlag_whenPayOrder_thenExpectKeyReusedProblem() throws Exception {
        UUID orderId = placeOrder();

        mockMvc.perform(post("/api/orders/{id}/payments", orderId)
                        .header(WebConstants.HEADER_IDEMPOTENCY_KEY, "pay-1")
                        .header(WebConstants.HEADER_SIMULATE_TIMEOUT, "no"))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/orders/{id}/payments", UUID.randomUUID())
                        .header(WebConstants.HEADER_IDEMPOTENCY_KEY, "pay-1")
                        .header(WebConstants.HEADER_SIMULATE_TIMEOUT, "no"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value(ProblemTypes.IDEMPOTENCY_KEY_REUSED.toString()));
    }

    @Test
    void givenCancelledOrder_whenPayOrder_thenExpectConflictNotServerError() throws Exception {
        UUID orderId = placeOrder();
        assertThat(orderService.cancelOrder(orderId)).isTrue();

        mockMvc.perform(post("/api/orders/{id}/payments", orderId)
                        .header(WebConstants.HEADER_IDEMPOTENCY_KEY, "pay-1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemTypes.INVALID_ORDER_STATE.toString()));

        // The failed request left no record behind, so the key is not burnt
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from idempotency_record", Long.class)).isZero();
    }

    @Test
    void givenUnknownOrder_whenPayOrder_thenExpectNotFound() throws Exception {
        mockMvc.perform(post("/api/orders/{id}/payments", UUID.randomUUID())
                        .header(WebConstants.HEADER_IDEMPOTENCY_KEY, "pay-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenPayment_whenGetUnderOtherOrder_thenExpectNotFound() throws Exception {
        UUID orderId = placeOrder();
        UUID otherOrderId = placeOrder();
        mockMvc.perform(post("/api/orders/{id}/payments", orderId)
                        .header(WebConstants.HEADER_IDEMPOTENCY_KEY, "pay-1"))
                .andExpect(status().isAccepted());
        UUID paymentId = jdbcTemplate.queryForObject(
                "select id from payment_attempt where order_id = ?", UUID.class, orderId);

        mockMvc.perform(get("/api/orders/{orderId}/payments/{paymentId}", orderId, paymentId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/{orderId}/payments/{paymentId}", otherOrderId, paymentId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orders/{orderId}/payments/{paymentId}", orderId, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // Delivery

    @Test
    void givenPaidOrder_whenDeliverTwice_thenExpectNoContentBothTimes() throws Exception {
        UUID orderId = placeOrder();
        forceStatus(orderId, "PAID");

        mockMvc.perform(post("/api/orders/{id}/delivery", orderId))
                .andExpect(status().isNoContent());
        assertThat(orderStatus(orderId)).isEqualTo("DELIVERED");

        // A retry after a lost response is a no-op rather than an error
        mockMvc.perform(post("/api/orders/{id}/delivery", orderId))
                .andExpect(status().isNoContent());
        assertThat(orderStatus(orderId)).isEqualTo("DELIVERED");
    }

    @Test
    void givenUnpaidOrder_whenDeliver_thenExpectConflict() throws Exception {
        UUID orderId = placeOrder();

        mockMvc.perform(post("/api/orders/{id}/delivery", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemTypes.INVALID_ORDER_STATE.toString()));

        assertThat(orderStatus(orderId)).isEqualTo("PLACED");
    }

    @Test
    void givenUnknownOrder_whenDeliver_thenExpectNotFound() throws Exception {
        mockMvc.perform(post("/api/orders/{id}/delivery", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
