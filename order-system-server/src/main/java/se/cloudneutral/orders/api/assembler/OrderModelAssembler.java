package se.cloudneutral.orders.api.assembler;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import se.cloudneutral.orders.api.LinkRelations;
import se.cloudneutral.orders.api.ProfileController;
import se.cloudneutral.orders.domain.Customer;
import se.cloudneutral.orders.domain.PurchaseOrder;
import se.cloudneutral.orders.domain.ShipmentStatus;
import se.cloudneutral.orders.api.CustomerController;
import se.cloudneutral.orders.api.OrderController;
import se.cloudneutral.orders.api.model.AddressModel;
import se.cloudneutral.orders.api.model.OrderModel;
import se.cloudneutral.orders.service.OrderService;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembles the HAL representation of a purchase order.
 * <p>
 * Expects an order read through {@link OrderService#findOrderDetailById}
 * or {@link OrderService#findOrders}, both of which join-fetch the
 * customer, the line items and their products. An order obtained any other way would trigger lazy
 * initialization here.
 * <p>
 * Line items are not inlined but exposed through an {@code items} link, see
 * {@link OrderItemModelAssembler}. The {@code cancel} affordance is attached to the self link only
 * while the order is in a state where cancelling is legal, so a HAL-FORMS client never sees a
 * template for a transition the server would reject.
 */
@Component
public class OrderModelAssembler implements RepresentationModelAssembler<PurchaseOrder, OrderModel> {
    @Override
    public OrderModel toModel(PurchaseOrder order) {
        Customer customer = order.getCustomer();

        OrderModel model = new OrderModel(
                order.getStatus(),
                order.getTotalPrice(),
                order.getDatePlaced(),
                order.getDateUpdated(),
                "%s %s".formatted(customer.getFirstName(), customer.getLastName()),
                customer.getEmail(),
                AddressModel.from(order.getDeliveryAddress()))
                .add(linkTo(methodOn(OrderController.class)
                        .getOrder(order.getId())).withSelfRel())
                .add(linkTo(methodOn(OrderController.class)
                        .getOrderItems(order.getId()))
                        .withRel(LinkRelations.ITEM_COLL_REL)
                        .withTitle("Order line items"))
                .add(linkTo(methodOn(CustomerController.class)
                        .getCustomer(customer.getId()))
                        .withRel(LinkRelations.CUSTOMER_REL)
                        .withTitle("Customer that placed the order"))
                .add(ProfileController.describedBy(ProfileController.ORDER));

        if (order.getStatus() == ShipmentStatus.placed) {
            model.add(cancelLink(order));
        }

        return model;
    }

    private Link cancelLink(PurchaseOrder order) {
        return linkTo(methodOn(OrderController.class)
                .cancelOrder(order.getId()))
                .withRel(LinkRelations.CANCEL_REL)
                .andAffordance(afford(methodOn(OrderController.class)
                        .cancelOrder(order.getId())));
    }
}
