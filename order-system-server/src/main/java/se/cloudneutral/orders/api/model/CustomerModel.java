package se.cloudneutral.orders.api.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import se.cloudneutral.orders.domain.Customer;

/**
 * Representation of a {@link Customer}.
 */
@JsonClassDescription("A customer who can place orders.")
@Relation(collectionRelation = "customers", itemRelation = "customer")
public class CustomerModel extends RepresentationModel<CustomerModel> {
    @JsonPropertyDescription("Identifier of the customer, used to refer to them in an order request.")
    private final UUID id;

    @JsonPropertyDescription("First name of the customer.")
    private final String firstName;

    @JsonPropertyDescription("Last name of the customer.")
    private final String lastName;

    @JsonPropertyDescription("Email address of the customer.")
    private final String email;

    @JsonPropertyDescription("Address of the customer.")
    private final AddressModel address;

    public CustomerModel(UUID id, String firstName, String lastName, String email, AddressModel address) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.address = address;
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public AddressModel getAddress() {
        return address;
    }
}
