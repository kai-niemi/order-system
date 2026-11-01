package se.cloudneutral.orders.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.mediatype.Affordances;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import se.cloudneutral.orders.annotation.TransactionBoundary;
import se.cloudneutral.orders.domain.Customer;
import se.cloudneutral.orders.service.IdempotencyRequest;
import se.cloudneutral.orders.service.IdempotencyResult;
import se.cloudneutral.orders.service.IdempotencyService;
import se.cloudneutral.orders.service.OrderService;
import se.cloudneutral.orders.domain.PurchaseOrder;
import se.cloudneutral.orders.domain.PurchaseOrderItem;
import se.cloudneutral.orders.domain.ShipmentStatus;
import se.cloudneutral.orders.api.assembler.OrderItemModelAssembler;
import se.cloudneutral.orders.api.assembler.OrderModelAssembler;
import se.cloudneutral.orders.api.model.OrderItemModel;
import se.cloudneutral.orders.api.model.OrderModel;
import se.cloudneutral.orders.api.payload.CreateOrderItemRequest;
import se.cloudneutral.orders.api.payload.CreateOrderRequest;
import se.cloudneutral.orders.util.AssertUtils;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping(value = "/api/orders",
        produces = {MediaTypes.HAL_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
public class OrderController {
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    public static final String HEADER_IDEMPOTENCY_REPLAYED = "Idempotency-Replayed";

    /**
     * Idempotency scope used when the request carries no authenticated principal.
     */
    private static final String ANONYMOUS_SCOPE = "anonymous";

    @Autowired
    private HttpServletRequest httpRequest;

    @Autowired
    private CreateOrderFacade createOrderFacade;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderModelAssembler orderModelAssembler;

    @Autowired
    private OrderItemModelAssembler orderItemModelAssembler;

    @Autowired
    private IdempotencyService idempotencyService;

    @GetMapping("/form")
    public EntityModel<CreateOrderRequest> createOrderRequestForm(
            @RequestParam(name = "products", required = false, defaultValue = "1") Integer products) {
        Customer customer = orderService.findRandomCustomer()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No customers found"));

        List<CreateOrderItemRequest> itemRequests = new ArrayList<>();

        orderService.findRandomProducts(products).forEach(
                product -> itemRequests.add(new CreateOrderItemRequest(product.getId(),
                        product.getInventory())));

        if (itemRequests.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "No products found");
        }

        Link self = linkTo(methodOn(getClass())
                .createOrderRequestForm(products))
                .withSelfRel();

        // Built rather than derived with afford(..), which would advertise every component of the
        // request record as read-only, see WritablePayloadMetadata
        Link create = Affordances.of(linkTo(methodOn(getClass())
                        .createOrder(null, null))
                        .withRel(LinkRelations.ORDER_REL)
                        .withTitle("Submit this form to place the order"))
                .afford(HttpMethod.POST)
                .withName("createOrder")
                .withInput(WritablePayloadMetadata.of(CreateOrderRequest.class))
                .withInputMediaType(MediaType.APPLICATION_JSON)
                .toLink();

        // The state of the form is the request to submit, so it is described by the request schema
        return EntityModel.of(new CreateOrderRequest(customer.getId(), itemRequests), self, create,
                ProfileController.describedBy(ProfileController.ORDER_REQUEST));
    }

    /**
     * Places a new purchase order. Line item unit prices are resolved from the current
     * product price rather than taken from the request, so a client cannot set its own price.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderModel> createOrder(
            @RequestHeader(HEADER_IDEMPOTENCY_KEY) String key,
            @Valid @RequestBody CreateOrderRequest request) {

        // Keys are scoped to the authenticated principal so that clients choosing the same key
        // never collide. Method and path are part of the request fingerprint.
        String scope = httpRequest.getUserPrincipal() != null
                ? httpRequest.getUserPrincipal().getName()
                : ANONYMOUS_SCOPE;

        IdempotencyResult<OrderModel> result = idempotencyService.execute(
                new IdempotencyRequest(scope, key,
                        httpRequest.getMethod(), httpRequest.getRequestURI(), request),
                orderId -> createOrderFacade.createOrder(orderId, request),
                this::findOrderModel
        );

        OrderModel model = result.resource();

        return ResponseEntity
                .created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .header(HEADER_IDEMPOTENCY_REPLAYED, Boolean.toString(result.replayed()))
                .body(model);
    }

    @GetMapping("/{id}")
    public OrderModel getOrder(@PathVariable("id") UUID id) {
        return findOrderModel(id);
    }

    @GetMapping
    public PagedModel<OrderModel> listOrders(@PageableDefault(size = 10) Pageable pageable,
                                             PagedResourcesAssembler<PurchaseOrder> pagedResourcesAssembler) {
        Page<PurchaseOrder> page = orderService.findOrders(pageable);
        return pagedResourcesAssembler.toModel(page, orderModelAssembler);
    }

    /**
     * The line items of an order, as an embedded sub-resource collection rather than an array
     * inlined in the order itself.
     */
    @GetMapping("/{id}/items")
    public CollectionModel<OrderItemModel> getOrderItems(@PathVariable("id") UUID id) {
        PurchaseOrder order = findOrderDetail(id);
        return orderItemModelAssembler.toCollectionModel(order.getId(), order.getOrderItems());
    }

    /**
     * A single line item, addressed by its position in the order.
     */
    @GetMapping("/{id}/items/{pos}")
    public OrderItemModel getOrderItem(@PathVariable("id") UUID id, @PathVariable("pos") int pos) {
        PurchaseOrder order = findOrderDetail(id);
        List<PurchaseOrderItem> items = order.getOrderItems();

        if (pos < 0 || pos >= items.size()) {
            throw new ResponseStatusException(
                    NOT_FOUND, "No such item in order %s: %d".formatted(id, pos));
        }

        return orderItemModelAssembler.toModel(order.getId(), pos, items.get(pos));
    }

    /**
     * Cancels an order that is still in {@link ShipmentStatus#placed}.
     * Advertised as the {@code cancel} HAL-FORMS template on the order representation, which only
     * appears while the transition is legal.
     */
    @PostMapping("/{id}/cancel")
    @TransactionBoundary
    public OrderModel cancelOrder(@PathVariable("id") UUID id) {
        AssertUtils.assertReadWriteTransaction();

        if (!orderService.cancelOrder(id)) {
            // The guarded update does not distinguish "absent" from "not cancellable",
            // so resolve which it was to pick the right status.
            if (orderService.findOrderById(id).isPresent()) {
                throw new ErrorResponseException(CONFLICT, RestExceptionHandler.problem(CONFLICT,
                        ProblemTypes.ORDER_NOT_CANCELLABLE, "Order not cancellable",
                        "Order is not in a cancellable state: %s".formatted(id)), null);
            }
            throw new ResponseStatusException(
                    NOT_FOUND, "No such order: %s".formatted(id));
        }

        return findOrderModel(id);
    }

    // placeOrder returns line items holding placeholder products carrying only id and sku,
    // so re-read the order with its associations fetched to render a complete body.
    private OrderModel findOrderModel(UUID id) {
        return orderModelAssembler.toModel(findOrderDetail(id));
    }

    private PurchaseOrder findOrderDetail(UUID id) {
        return orderService.findOrderDetailById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND, "No such order: %s".formatted(id)));
    }
}
