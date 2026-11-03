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
public class CustomerCommands {
    private final HypermediaClient client;

    public CustomerCommands(HypermediaClient client) {
        this.client = client;
    }

    @Command(name = {"customer", "list"},
            group = "1. Customer Commands",
            alias = "cl",
            description = "List customers, discovered from the API root and paged by following 'next' links")
    public String listCustomers(
            @Option(longName = "limit", shortName = 'l', defaultValue = "10",
                    description = "Maximum number of customers to list") int limit) {
        Representation firstPage = client.follow(client.root(), OrderSystemRels.CUSTOMERS);

        List<Representation> customers = new ArrayList<>();
        Optional<Representation> page = Optional.of(firstPage);
        while (page.isPresent() && customers.size() < limit) {
            page.get().embedded(OrderSystemRels.CUSTOMERS).stream()
                    .limit(limit - customers.size())
                    .forEach(customers::add);
            page = client.followIfPresent(page.get(), OrderSystemRels.NEXT);
        }

        Table table = new Table("First Name", "Last Name", "E-mail",
                "Address 1", "Address 2", "Postcode", "City");
        customers.forEach(customer -> table.addRow(
                customer.get("firstName").asString(),
                customer.get("lastName").asString(),
                customer.get("email").asString(),
                customer.get("address").get("address1").asString(),
                customer.get("address").get("address1").asString(),
                customer.get("address").get("postcode").asString(),
                customer.get("address").get("city").asString()
        ));

        return table.render()
               + "%n%d of %d customers".formatted(customers.size(),
                firstPage.get("page").path("totalElements").asLong(customers.size()));
    }
}

