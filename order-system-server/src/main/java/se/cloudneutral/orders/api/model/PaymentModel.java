package se.cloudneutral.orders.api.model;

import java.util.UUID;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonClassDescription;

import se.cloudneutral.orders.domain.PaymentStatus;

@JsonClassDescription("A payment outcome for a purchase order.")
@Relation(collectionRelation = "payments", itemRelation = "payment")
public class PaymentModel extends RepresentationModel<PaymentModel> {
    private final UUID id;

    private final PaymentStatus status;

    public PaymentModel(UUID id, PaymentStatus status) {
        this.id = id;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public PaymentStatus getStatus() {
        return status;
    }
}