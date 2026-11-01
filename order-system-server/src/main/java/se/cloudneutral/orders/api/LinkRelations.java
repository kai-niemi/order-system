package se.cloudneutral.orders.api;

public abstract class LinkRelations {
    private LinkRelations() {
    }


    public static final String ACTUATORS_REL = "actuators";

    public static final String PRODUCT_REL = "product";
    public static final String PRODUCT_COLL_REL = "products";

    public static final String ORDER_REL = "order";
    public static final String ORDER_COLL_REL = "orders";

    public static final String ITEM_COLL_REL = "items";

    public static final String CUSTOMER_REL = "customer";
    public static final String CUSTOMER_COLL_REL = "customers";

    public static final String CANCEL_REL = "cancel";

    public static final String CURIE_NAMESPACE = "os";

    // Identity of the extension relations, independent of where the server is deployed.
    // Clients match against the expanded URI, so this must never change. Docs are served at /rels/{rel}.
    public static final String REL_URI_TEMPLATE = "https://order-system.cloudneutral.se/rels/{rel}";
}
