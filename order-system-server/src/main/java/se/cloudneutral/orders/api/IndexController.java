package se.cloudneutral.orders.api;

import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
public class IndexController {
    // API entry point, clients discover the available resources by following its links
    @GetMapping(value = "/", produces = MediaTypes.HAL_JSON_VALUE)
    public RepresentationModel<?> index() {
        return new RepresentationModel<>()
                .add(linkTo(methodOn(IndexController.class)
                        .index())
                        .withSelfRel())
                .add(linkTo(OrderController.class)
                        .withRel(LinkRelations.ORDER_COLL_REL)
                        .withTitle("Purchase orders"))
                .add(linkTo(methodOn(OrderController.class)
                        .createOrderRequestForm(null))
                        .withRel(LinkRelations.ORDER_COLL_REL + "-form")
                        .withTitle("Purchase order form"))
                .add(linkTo(ProductController.class)
                        .withRel(LinkRelations.PRODUCT_COLL_REL)
                        .withTitle("Products available to order"))
                .add(linkTo(CustomerController.class)
                        .withRel(LinkRelations.CUSTOMER_COLL_REL)
                        .withTitle("Customers that can place orders"))
                .add(Link.of(ServletUriComponentsBuilder.fromCurrentContextPath()
                                .pathSegment("actuator")
                                .buildAndExpand()
                                .toUriString())
                        .withRel(LinkRelations.ACTUATORS_REL)
                        .withTitle("Spring boot actuators"))
                .add(Link.of(ServletUriComponentsBuilder.fromCurrentContextPath()
                                .pathSegment("guides", "index")
                                .buildAndExpand()
                                .toUriString())
                        .withRel(IanaLinkRelations.HELP)
                        .withTitle("Guides to using the API"));
    }
}
