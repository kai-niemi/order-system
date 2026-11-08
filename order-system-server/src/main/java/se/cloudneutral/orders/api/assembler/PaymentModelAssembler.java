package se.cloudneutral.orders.api.assembler;

import java.util.UUID;

import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import se.cloudneutral.orders.api.LinkRelations;
import se.cloudneutral.orders.api.OrderController;
import se.cloudneutral.orders.api.PaymentController;
import se.cloudneutral.orders.api.model.PaymentModel;
import se.cloudneutral.orders.domain.PaymentAttempt;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class PaymentModelAssembler implements RepresentationModelAssembler<PaymentAttempt, PaymentModel> {
    @Override
    public PaymentModel toModel(PaymentAttempt payment) {
        UUID orderId = payment.getOrderId();
        UUID paymentId = payment.getId();
        return new PaymentModel(
                paymentId,
                payment.getStatus())
                .add(linkTo(methodOn(PaymentController.class)
                        .getPayment(orderId, paymentId))
                        .withSelfRel())
                .add(linkTo(methodOn(OrderController.class)
                        .getOrder(orderId))
                        .withRel(LinkRelations.ORDER_REL)
                );
    }
}
