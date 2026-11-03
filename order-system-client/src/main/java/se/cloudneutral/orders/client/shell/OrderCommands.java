package se.cloudneutral.orders.client.shell;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import se.cloudneutral.orders.client.OrderSystemRels;
import se.cloudneutral.orders.client.hypermedia.HypermediaClient;
import se.cloudneutral.orders.client.hypermedia.Link;
import se.cloudneutral.orders.client.hypermedia.Representation;
import se.cloudneutral.orders.client.hypermedia.Response;
import se.cloudneutral.orders.client.hypermedia.Template;
import se.cloudneutral.orders.client.shell.support.Table;

@Component
public class OrderCommands {
    private static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final String HEADER_IDEMPOTENCY_REPLAYED = "Idempotency-Replayed";

    private final HypermediaClient client;

    // The order placed most recently in this shell session, the default for 'order cancel'
    private URI lastPlacedOrder;

    public OrderCommands(HypermediaClient client) {
        this.client = client;
    }

    @Command(name = {"order", "cancel"},
            alias = "ox",
            group = "3. Order Commands",
            description = "Cancel an order through its 'cancel' HAL-FORMS affordance, offered only while it can be cancelled")
    public String cancelOrder(@Argument(index = 0, description = "URI of the order, as printed by 'order create'. "
                                  + "Defaults to the last order placed in this session") String orderUri) {
        URI uri;
        if (StringUtils.hasText(orderUri)) {
            uri = URI.create(orderUri);
        } else if (lastPlacedOrder != null) {
            uri = lastPlacedOrder;
        } else {
            throw new IllegalArgumentException("No order URI given, and no order placed in this session");
        }

        // Ask for HAL-FORMS, so the order carries the templates for the transitions legal right now
        Representation order = client.get(uri, HypermediaClient.HAL_FORMS_JSON);

        // Whether an order can be cancelled is the server's rule: no affordance, no request
        Optional<Link> cancelLink = order.link(OrderSystemRels.CANCEL);
        if (cancelLink.isEmpty()) {
            return "Order is %s, the server offers no cancellation for it: %s"
                    .formatted(order.get("status").asString(), uri);
        }

        Template template = order.templateFor(cancelLink.get())
                .orElseThrow(() -> new IllegalStateException(
                        "Order has no template for %s".formatted(cancelLink.get().href())));

        // The template has no properties, so there is nothing to send
        Response response = client.submit(template, null, Map.of());

        Representation cancelled = response.resource();

        return String.join(System.lineSeparator(),
                "%s %s".formatted(response.status(), uri),
                "Status:          %s -> %s".formatted(order.get("status").asString(),
                        cancelled.get("status").asString()),
                "Updated:         %s".formatted(cancelled.get("dateUpdated").asString()),
                "Cancel offered:  %s".formatted(cancelled.link(OrderSystemRels.CANCEL).isPresent()));
    }

    @Command(name = {"order", "create"},
            alias = "oc",
            group = "3. Order Commands",
            description = "Place an order by filling in the order form and submitting its HAL-FORMS affordance")
    public String createOrder(
            @Option(longName = "products", shortName = 'p', defaultValue = "1",
                    description = "Number of products on the order, picked by the server") int products,
            @Option(longName = "key", shortName = 'k',
                    description = "Idempotency key, random if omitted. Each call fills in a new form, "
                                  + "so reusing a key is rejected as a different request")
            String key) {
        Link formLink = client.root().link(OrderSystemRels.ORDERS_FORM)
                .orElseThrow(() -> new IllegalStateException("API root has no order form link"));

        // Ask for HAL-FORMS, so the form carries the templates describing how to submit it
        Representation form = client.get(formLink.expand(Map.of("products", products)), HypermediaClient.HAL_FORMS_JSON);

        // The 'order' link is the documented affordance, its template says how to use it
        Link orderLink = form.link(OrderSystemRels.ORDER)
                .orElseThrow(() -> new IllegalStateException("Order form has no 'order' affordance"));
        Template template = form.templateFor(orderLink)
                .orElseThrow(() -> new IllegalStateException(
                        "Order form has no template for %s".formatted(orderLink.href())));

        String idempotencyKey = key != null ? key : UUID.randomUUID().toString();

        Response response = client.submit(template, form.properties(),
                Map.of(HEADER_IDEMPOTENCY_KEY, idempotencyKey));

        Representation order = response.resource();
        lastPlacedOrder = order.link(OrderSystemRels.SELF).map(Link::toUri).orElse(null);

        Representation items = client.follow(order, OrderSystemRels.ITEMS);

        Table table = new Table("SKU", "Name", "Quantity", "Unit price", "Total");
        items.embedded(OrderSystemRels.ORDER_ITEMS).forEach(item -> table.addRow(
                item.get("productSku").asString(),
                item.get("productName").asString(),
                item.get("quantity").asString(),
                item.get("unitPrice").asString(),
                item.get("totalCost").asString()));

        return String.join(System.lineSeparator(),
                "%s %s".formatted(response.status(), response.headers().getFirst(HttpHeaders.LOCATION)),
                "Status:          %s".formatted(order.get("status").asString()),
                "Customer:        %s".formatted(order.get("customerName").asString()),
                "Total price:     %s".formatted(order.get("totalPrice").asString()),
                "Idempotency-Key: %s (replayed: %s)".formatted(idempotencyKey,
                        response.headers().getFirst(HEADER_IDEMPOTENCY_REPLAYED)),
                "",
                table.render());
    }

    @Command(name = {"order", "list"},
            group = "3. Order Commands",
            alias = "ol",
            description = "List orders, discovered from the API root and paged by following 'next' links")
    public String listOrders(
            @Option(longName = "limit", shortName = 'l', defaultValue = "10",
                    description = "Maximum number of orders to list") int limit) {
        Representation firstPage = client.follow(client.root(), OrderSystemRels.ORDER_COLL_REL);

        List<Representation> orders = new ArrayList<>();
        Optional<Representation> page = Optional.of(firstPage);
        while (page.isPresent() && orders.size() < limit) {
            page.get().embedded(OrderSystemRels.ORDER_COLL_REL).stream()
                    .limit(limit - orders.size())
                    .forEach(orders::add);
            page = client.followIfPresent(page.get(), OrderSystemRels.NEXT);
        }

        Table table = new Table("Status", "Total Price", "Date Placed", "Date Updated",
                "Customer Name", "Customer Email");
        orders.forEach(product -> table.addRow(
                product.get("status").asString(),
                product.get("totalPrice").asString(),
                product.get("datePlaced").asString(),
                product.get("dateUpdated").asString(),
                product.get("customerName").asString(),
                product.get("customerEmail").asString()
        ));

        return table.render()
               + "%n%d of %d orders".formatted(orders.size(),
                firstPage.get("page").path("totalElements").asLong(orders.size()));
    }

}
