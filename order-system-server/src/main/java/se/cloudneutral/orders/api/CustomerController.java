package se.cloudneutral.orders.api;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import se.cloudneutral.orders.domain.Customer;
import se.cloudneutral.orders.service.OrderService;
import se.cloudneutral.orders.api.assembler.CustomerModelAssembler;
import se.cloudneutral.orders.api.model.CustomerModel;
import se.cloudneutral.orders.api.model.OptionModel;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping(value = "/api/customers",
        produces = {MediaTypes.HAL_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
public class CustomerController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerModelAssembler customerModelAssembler;

    @GetMapping("/{id}")
    public CustomerModel getCustomer(@PathVariable("id") UUID id) {
        return orderService.findCustomerById(id)
                .map(customerModelAssembler::toModel)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND, "No such customer: %s".formatted(id)));
    }

    /**
     * The customers as the possible values of an order request's customerId, for the HAL-FORMS
     * options of the order form. HAL-FORMS expects a plain array rather than a HAL collection.
     */
    @GetMapping(value = "/options", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OptionModel> customerOptions() {
        return orderService.findCustomers(Pageable.unpaged(Sort.by("lastName", "firstName")))
                .map(customer -> new OptionModel(
                        "%s %s <%s>".formatted(customer.getFirstName(), customer.getLastName(), customer.getEmail()),
                        customer.getId()))
                .toList();
    }

    @GetMapping
    public PagedModel<CustomerModel> listCustomers(@PageableDefault(size = 10) Pageable pageable,
                                                   PagedResourcesAssembler<Customer> pagedResourcesAssembler) {
        Page<Customer> page = orderService.findCustomers(pageable);
        return pagedResourcesAssembler.toModel(page, customerModelAssembler);
    }
}
