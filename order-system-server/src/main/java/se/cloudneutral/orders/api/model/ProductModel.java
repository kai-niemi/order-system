package se.cloudneutral.orders.api.model;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import se.cloudneutral.orders.domain.Product;

/**
 * Representation of a {@link Product}.
 */
@JsonClassDescription("A product that can be ordered, as it is now.")
@Relation(collectionRelation = "products", itemRelation = "product")
public class ProductModel extends RepresentationModel<ProductModel> {
    @JsonPropertyDescription("Identifier of the product, used to refer to it in an order request.")
    private final UUID id;

    @JsonPropertyDescription("Name of the product.")
    private final String name;

    @JsonPropertyDescription("Stock keeping unit of the product.")
    private final String sku;

    @JsonPropertyDescription("Current price of one unit. An order records the price at the time it is placed.")
    private final BigDecimal price;

    @JsonPropertyDescription("Units in stock. An order for more than this is rejected.")
    private final int inventory;

    public ProductModel(UUID id, String name, String sku, BigDecimal price, int inventory) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.inventory = inventory;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSku() {
        return sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getInventory() {
        return inventory;
    }
}
