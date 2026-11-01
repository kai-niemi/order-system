package se.cloudneutral.orders.api.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import se.cloudneutral.orders.domain.ShipmentStatus;
import se.cloudneutral.orders.domain.PurchaseOrder;

/**
 * Representation of a {@link PurchaseOrder}. Flattens the
 * lazily associated customer to its id and display fields, which keeps the payload free
 * of Hibernate proxies.
 * <p>
 * The line items are not inlined here; they are a sub-resource collection reachable
 * through the {@code items} link.
 */
@JsonClassDescription("A purchase order. It has no identifier property: the target of its self link is its identity.")
@Relation(collectionRelation = "orders", itemRelation = "order")
public class OrderModel extends RepresentationModel<OrderModel> {
    @JsonPropertyDescription("Where the order is in its lifecycle. The transitions available in the current state are advertised on the order as links and HAL-FORMS templates.")
    private final ShipmentStatus status;

    @JsonPropertyDescription("Sum of the cost of all line items, in the currency of the product prices.")
    private final BigDecimal totalPrice;

    @JsonPropertyDescription("When the order was placed, as an instant in UTC.")
    private final OffsetDateTime datePlaced;

    @JsonPropertyDescription("When the order last changed, as an instant in UTC.")
    private final OffsetDateTime dateUpdated;

    @JsonPropertyDescription("Full name of the customer who placed the order, for display. Follow the customer link for the customer itself.")
    private final String customerName;

    @JsonPropertyDescription("Email address of the customer who placed the order, for display.")
    private final String customerEmail;

    @JsonPropertyDescription("Where the order is to be delivered.")
    private final AddressModel deliveryAddress;

    public OrderModel(ShipmentStatus status, BigDecimal totalPrice,
                      OffsetDateTime datePlaced, OffsetDateTime dateUpdated,
                      String customerName, String customerEmail,
                      AddressModel deliveryAddress) {
        this.status = status;
        this.totalPrice = totalPrice;
        this.datePlaced = datePlaced;
        this.dateUpdated = dateUpdated;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.deliveryAddress = deliveryAddress;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public OffsetDateTime getDatePlaced() {
        return datePlaced;
    }

    public OffsetDateTime getDateUpdated() {
        return dateUpdated;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public AddressModel getDeliveryAddress() {
        return deliveryAddress;
    }
}
