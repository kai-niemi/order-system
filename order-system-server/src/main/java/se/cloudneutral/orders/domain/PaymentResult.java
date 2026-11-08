package se.cloudneutral.orders.domain;

public record PaymentResult(
        PaymentStatus status,
        String providerReference
) {
    public static PaymentResult succeeded(String reference) {
        return new PaymentResult(PaymentStatus.SUCCEEDED, reference);
    }

    public static PaymentResult declined() {
        return new PaymentResult(PaymentStatus.DECLINED, null);
    }
}