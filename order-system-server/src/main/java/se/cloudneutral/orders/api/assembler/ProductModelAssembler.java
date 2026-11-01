package se.cloudneutral.orders.api.assembler;

import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import se.cloudneutral.orders.domain.Product;
import se.cloudneutral.orders.api.ProfileController;
import se.cloudneutral.orders.api.ProductController;
import se.cloudneutral.orders.api.model.ProductModel;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class ProductModelAssembler implements RepresentationModelAssembler<Product, ProductModel> {
    @Override
    public ProductModel toModel(Product product) {
        return new ProductModel(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice(),
                product.getInventory())
                .add(linkTo(methodOn(ProductController.class)
                        .getProduct(product.getId()))
                        .withSelfRel())
                .add(ProfileController.describedBy(ProfileController.PRODUCT));
    }
}
