package se.cloudneutral.orders.api;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.AffordanceModel.PropertyMetadata;
import org.springframework.hateoas.mediatype.PropertyUtils;

import se.cloudneutral.orders.api.payload.CreateOrderRequest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
class WritablePayloadMetadataTest {
    private static Map<String, PropertyMetadata> properties(Iterable<PropertyMetadata> metadata) {
        return java.util.stream.StreamSupport.stream(metadata.spliterator(), false)
                .collect(Collectors.toMap(PropertyMetadata::getName, Function.identity()));
    }

    @Test
    void givenRecord_thenSpringAdvertisesEveryComponentAsReadOnly() {
        // The reason for WritablePayloadMetadata. If this fails, Spring HATEOAS has learned about
        // records and the workaround can go.
        assertThat(PropertyUtils.getExposedProperties(CreateOrderRequest.class).stream())
                .isNotEmpty()
                .allMatch(PropertyMetadata::isReadOnly);
    }

    @Test
    void givenRecord_thenExpectWritableProperties() {
        Map<String, PropertyMetadata> properties = properties(
                WritablePayloadMetadata.of(CreateOrderRequest.class).stream().toList());

        assertThat(properties).containsOnlyKeys("customerId", "items");
        assertThat(properties.values()).noneMatch(PropertyMetadata::isReadOnly);
    }

    @Test
    void givenRecord_thenExpectEverythingElseKept() {
        Map<String, PropertyMetadata> original = properties(
                PropertyUtils.getExposedProperties(CreateOrderRequest.class).stream().toList());
        Map<String, PropertyMetadata> writable = properties(
                WritablePayloadMetadata.of(CreateOrderRequest.class).stream().toList());

        original.forEach((name, metadata) -> {
            PropertyMetadata copy = writable.get(name);
            assertThat(copy.isRequired()).isEqualTo(metadata.isRequired());
            assertThat(copy.getType()).isEqualTo(metadata.getType());
            assertThat(copy.getInputType()).isEqualTo(metadata.getInputType());
            assertThat(copy.getPattern()).isEqualTo(metadata.getPattern());
        });

        assertThat(writable.get("customerId").isRequired()).isTrue();
        assertThat(writable.get("items").isRequired()).isTrue();
        assertThat(WritablePayloadMetadata.of(CreateOrderRequest.class).getType())
                .isEqualTo(CreateOrderRequest.class);
    }
}
