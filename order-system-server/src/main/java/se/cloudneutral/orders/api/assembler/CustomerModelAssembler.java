package se.cloudneutral.orders.api.assembler;

import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import se.cloudneutral.orders.domain.Customer;
import se.cloudneutral.orders.api.ProfileController;
import se.cloudneutral.orders.api.CustomerController;
import se.cloudneutral.orders.api.model.AddressModel;
import se.cloudneutral.orders.api.model.CustomerModel;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class CustomerModelAssembler implements RepresentationModelAssembler<Customer, CustomerModel> {
    @Override
    public CustomerModel toModel(Customer customer) {
        return new CustomerModel(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                AddressModel.from(customer.getAddress()))
                .add(linkTo(methodOn(CustomerController.class)
                        .getCustomer(customer.getId()))
                        .withSelfRel())
                .add(ProfileController.describedBy(ProfileController.CUSTOMER));
    }
}
