package se.cloudneutral.orders.api.model;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * Representation of a single purchase order line item, addressable as a sub-resource of
 * its order. Carries a {@code self} link keyed on the item position and a {@code product}
 * link so a client can navigate from an order to the products it refers to.
 */
@JsonClassDescription("A line of an order: one product, how many, and the price recorded when the order was placed.")
@Relation(collectionRelation = "orderItems", itemRelation = "orderItem")
public class OrderItemModel extends RepresentationModel<OrderItemModel> {
    @JsonPropertyDescription("Identifier of the ordered product. Follow the product link to reach the product rather than building its address.")
    private final UUID productId;

    @JsonPropertyDescription("Stock keeping unit of the ordered product.")
    private final String productSku;

    @JsonPropertyDescription("Name of the ordered product.")
    private final String productName;

    @JsonPropertyDescription("Number of units ordered.")
    private final int quantity;

    @JsonPropertyDescription("Price of one unit, recorded when the order was placed.")
    private final BigDecimal unitPrice;

    @JsonPropertyDescription("Quantity times unit price.")
    private final BigDecimal totalCost;

    public OrderItemModel(UUID productId, String productSku, String productName,
                          int quantity, BigDecimal unitPrice, BigDecimal totalCost) {
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalCost = totalCost;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductSku() {
        return productSku;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }
}
