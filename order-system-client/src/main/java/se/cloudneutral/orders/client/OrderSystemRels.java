package se.cloudneutral.orders.client;

/**
 * The link relations this client understands. Extension relations are identified by their full
 * URI, which is part of the API contract and must match the server's curie template.
 */
public abstract class OrderSystemRels {
    private OrderSystemRels() {
    }

    public static final String REL_URI_TEMPLATE = "https://order-system.cloudneutral.se/rels/{rel}";

    public static final String PRODUCTS = of("products");

    public static final String ORDERS_FORM = of("orders-form");

    // On the order form, the affordance for submitting it. On anything else, a single order.
    public static final String ORDER = of("order");

    public static final String ITEMS = of("items");

    public static final String ORDER_ITEMS = of("orderItems");

    // Offered on an order only while it can be cancelled
    public static final String CANCEL = of("cancel");

    // IANA registered relations, used without a prefix

    public static final String SELF = "self";

    public static final String NEXT = "next";

    private static String of(String name) {
        return REL_URI_TEMPLATE.replace("{rel}", name);
    }
}
