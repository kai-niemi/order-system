package se.cloudneutral.orders.client.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import se.cloudneutral.orders.client.OrderSystemRels;
import se.cloudneutral.orders.client.hypermedia.HypermediaClient;
import se.cloudneutral.orders.client.hypermedia.Representation;
import se.cloudneutral.orders.client.shell.support.Table;

@Component
public class ProductCommands {
    private final HypermediaClient client;

    public ProductCommands(HypermediaClient client) {
        this.client = client;
    }

    @Command(name = {"product", "list"},
            group = "Product Commands",
            alias = "pl",
            description = "List products, discovered from the API root and paged by following 'next' links")
    public String listProducts(
            @Option(longName = "limit", shortName = 'l', defaultValue = "10",
                    description = "Maximum number of products to list") int limit) {
        Representation firstPage = client.follow(client.root(), OrderSystemRels.PRODUCTS);

        List<Representation> products = new ArrayList<>();
        Optional<Representation> page = Optional.of(firstPage);
        while (page.isPresent() && products.size() < limit) {
            page.get().embedded(OrderSystemRels.PRODUCTS).stream()
                    .limit(limit - products.size())
                    .forEach(products::add);
            page = client.followIfPresent(page.get(), OrderSystemRels.NEXT);
        }

        Table table = new Table("SKU", "Name", "Price", "Inventory");
        products.forEach(product -> table.addRow(
                product.get("sku").asString(),
                product.get("name").asString(),
                product.get("price").asString(),
                product.get("inventory").asString()));

        return table.render()
               + "%n%d of %d products".formatted(products.size(),
                firstPage.get("page").path("totalElements").asLong(products.size()));
    }
}
