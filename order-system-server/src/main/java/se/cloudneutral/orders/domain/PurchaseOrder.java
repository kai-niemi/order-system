package se.cloudneutral.orders.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;

@Entity
@Table(name = "purchase_order")
@NamedQuery(name = "findOrderById",
        query = "select po from PurchaseOrder po where po.id=:id")
@NamedQuery(name = "updateOrderStatusById",
        query = "update PurchaseOrder po set po.status=:postStatus, po.dateUpdated=:now where po.id=:id and po.status=:preStatus")
public class PurchaseOrder extends AbstractEntity<UUID> {
    // Assigned rather than generated: callers may supply the id up front (see Builder#withId),
    // and JPA would treat a preset id on a generated-id entity as detached.
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "total_price", nullable = false, updatable = false)
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @ElementCollection(fetch = FetchType.LAZY)
    @JoinTable(name = "purchase_order_item", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "item_pos")
    private List<PurchaseOrderItem> orderItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    // The column is the native postgres enum type shipment_status. Without this, hibernate binds
    // the parameter as varchar, which the implicit cast covers in a SET clause but not in a
    // comparison - see the preStatus guard of the updateOrderStatusById named query.
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", length = 25, nullable = false)
    private ShipmentStatus status = ShipmentStatus.placed;

    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, updatable = false, name = "date_placed")
    private OffsetDateTime datePlaced;

    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, name = "date_updated")
    private OffsetDateTime dateUpdated;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address1",
                    column = @Column(name = "deliv_address1")),
            @AttributeOverride(name = "address2",
                    column = @Column(name = "deliv_address2")),
            @AttributeOverride(name = "city",
                    column = @Column(name = "deliv_city")),
            @AttributeOverride(name = "postcode",
                    column = @Column(name = "deliv_postcode")),
            @AttributeOverride(name = "country",
                    column = @Column(name = "deliv_country"))
    })
    private Address deliveryAddress;

    @Override
    public UUID getId() {
        return id;
    }

    @PrePersist
    public void preCreate() {
        if (datePlaced == null) {
            datePlaced = OffsetDateTime.now();
        }
        if (dateUpdated == null) {
            dateUpdated = OffsetDateTime.now();
        }
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public void setDateUpdated(OffsetDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public OffsetDateTime getDatePlaced() {
        return datePlaced;
    }

    public OffsetDateTime getDateUpdated() {
        return dateUpdated;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public Customer getCustomer() {
        return customer;
    }

    public List<PurchaseOrderItem> getOrderItems() {
        return Collections.unmodifiableList(orderItems);
    }

    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public BigDecimal subTotal() {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (PurchaseOrderItem oi : orderItems) {
            subTotal = subTotal.add(oi.totalCost());
        }
        return subTotal;
    }

    @Override
    public String toString() {
        return "PurchaseOrder{" +
               "id=" + id +
               ", totalPrice=" + totalPrice +
               ", customer=" + customer +
               ", orderItems=" + orderItems +
               ", status=" + status +
               ", datePlaced=" + datePlaced +
               ", dateUpdated=" + dateUpdated +
               ", deliveryAddress=" + deliveryAddress +
               '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;

        private Customer customer;

        private final List<PurchaseOrderItem> orderItems = new ArrayList<>();

        private Builder() {
        }

        /**
         * Optional caller-supplied id. A random id is assigned if none is given.
         */
        public Builder withId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder withCustomer(Customer customer) {
            this.customer = customer;
            return this;
        }

        public Builder andOrderItems(List<PurchaseOrderItem> items) {
            this.orderItems.addAll(items);
            return this;
        }

        public PurchaseOrderItem.NestedBuilder andOrderItem() {
            return new PurchaseOrderItem.NestedBuilder(this, orderItems::add);
        }

        public PurchaseOrder build() {
            if (this.customer == null) {
                throw new IllegalStateException("Missing customer");
            }
            if (this.orderItems.isEmpty()) {
                throw new IllegalStateException("Empty order");
            }

            PurchaseOrder order = new PurchaseOrder();
            order.id = id != null ? id : UUID.randomUUID();
            order.customer = this.customer;
            order.deliveryAddress = customer.getAddress();
            order.orderItems.addAll(this.orderItems);

            return order;
        }
    }
}
