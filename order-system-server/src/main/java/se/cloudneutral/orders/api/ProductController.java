package se.cloudneutral.orders.api;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import se.cloudneutral.orders.domain.Product;
import se.cloudneutral.orders.service.OrderService;
import se.cloudneutral.orders.api.assembler.ProductModelAssembler;
import se.cloudneutral.orders.api.model.ProductModel;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping(value = "/api/products",
        produces = {MediaTypes.HAL_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
public class ProductController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductModelAssembler productModelAssembler;

    @GetMapping("/{id}")
    public ProductModel getProduct(@PathVariable("id") UUID id) {
        return orderService.findProductById(id)
                .map(productModelAssembler::toModel)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND, "No such product: %s".formatted(id)));
    }

    @GetMapping
    public PagedModel<ProductModel> listProducts(@PageableDefault(size = 10) Pageable pageable,
                                                 PagedResourcesAssembler<Product> pagedResourcesAssembler) {
        Page<Product> page = orderService.findProducts(pageable);
        return pagedResourcesAssembler.toModel(page, productModelAssembler);
    }
}
