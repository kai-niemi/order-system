package se.cloudneutral.orders.api.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import se.cloudneutral.orders.domain.Address;

/**
 * Representation of the embeddable {@link Address}. Described by the property that holds it,
 * a class description would be combined with that one rather than replaced by it.
 */
public record AddressModel(
        @JsonPropertyDescription("First line of the street address.")
        String address1,
        @JsonPropertyDescription("Second line of the street address, if any.")
        String address2,
        @JsonPropertyDescription("City.")
        String city,
        @JsonPropertyDescription("Postal code.")
        String postcode,
        @JsonPropertyDescription("Country.")
        String country) {

    public static AddressModel from(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressModel(
                address.getAddress1(),
                address.getAddress2(),
                address.getCity(),
                address.getPostcode(),
                address.getCountry());
    }
}
