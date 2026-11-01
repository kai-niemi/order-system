package se.cloudneutral.orders.api.payload;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * A single line item of a {@link CreateOrderRequest}. The unit price is deliberately
 * absent: it is resolved server-side from the current product price so that clients
 * cannot dictate what they pay.
 */
@JsonClassDescription("A product line of an order request.")
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateOrderItemRequest(
        @JsonPropertyDescription("Identifier of the product to order.")
        @NotNull UUID productId,
        @JsonPropertyDescription("Number of units to order, at least one.")
        @Positive int quantity) {
}
