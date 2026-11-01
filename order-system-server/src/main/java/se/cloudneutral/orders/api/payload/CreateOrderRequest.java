package se.cloudneutral.orders.api.payload;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for placing a new purchase order. The customer and products are
 * referenced by id, both discoverable by following the links from the API root.
 */
@JsonClassDescription("A request to place an order. It carries no prices: the price of each line is "
                      + "the current product price when the order is placed.")
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateOrderRequest(
        @JsonPropertyDescription("Identifier of the customer placing the order.")
        @NotNull UUID customerId,
        // @NotNull as well, since HAL-FORMS only derives required from @NotNull and @NotBlank
        @JsonPropertyDescription("The product lines of the order, at least one.")
        @NotNull @NotEmpty @Valid List<CreateOrderItemRequest> items) {
}
