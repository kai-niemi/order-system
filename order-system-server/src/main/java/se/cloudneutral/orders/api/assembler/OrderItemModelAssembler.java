package se.cloudneutral.orders.api.assembler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.stereotype.Component;

import se.cloudneutral.orders.api.ProfileController;
import se.cloudneutral.orders.api.LinkRelations;
import se.cloudneutral.orders.domain.Product;
import se.cloudneutral.orders.domain.PurchaseOrderItem;
import se.cloudneutral.orders.api.OrderController;
import se.cloudneutral.orders.api.ProductController;
import se.cloudneutral.orders.api.model.OrderItemModel;
import se.cloudneutral.orders.service.OrderService;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembles the HAL representation of purchase order line items.
 * <p>
 * This deliberately does not implement
 * {@link org.springframework.hateoas.server.RepresentationModelAssembler}: a
 * {@link PurchaseOrderItem} is an {@code @Embeddable} with no identity of its own, so
 * building its self link needs both the parent order id and the item position carried by
 * the {@code item_pos} order column.
 * <p>
 * Expects items read through
 * {@link OrderService#findOrderDetailById}, which join-fetches
 * the products. Items obtained any other way would trigger lazy initialization here.
 */
@Component
public class OrderItemModelAssembler {
    public OrderItemModel toModel(UUID orderId, int position, PurchaseOrderItem item) {
        Product product = item.getProduct();

        return new OrderItemModel(
                product.getId(),
                product.getSku(),
                product.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.totalCost())
                .add(linkTo(methodOn(OrderController.class)
                        .getOrderItem(orderId, position))
                        .withSelfRel())
                .add(linkTo(methodOn(ProductController.class)
                        .getProduct(product.getId()))
                        .withRel(LinkRelations.PRODUCT_REL)
                        .withTitle("Ordered product"))
                .add(ProfileController.describedBy(ProfileController.ORDER_ITEM));
    }

    public CollectionModel<OrderItemModel> toCollectionModel(UUID orderId, List<PurchaseOrderItem> items) {
        List<OrderItemModel> models = new ArrayList<>(items.size());

        for (int position = 0; position < items.size(); position++) {
            models.add(toModel(orderId, position, items.get(position)));
        }

        return CollectionModel.of(models)
                .add(linkTo(methodOn(OrderController.class)
                        .getOrderItems(orderId))
                        .withSelfRel())
                .add(linkTo(methodOn(OrderController.class)
                        .getOrder(orderId))
                        .withRel(LinkRelations.ORDER_REL)
                        .withTitle("Purchase order owning these items"));
    }
}
