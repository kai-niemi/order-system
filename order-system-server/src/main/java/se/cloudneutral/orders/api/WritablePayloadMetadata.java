package se.cloudneutral.orders.api;

import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.core.ResolvableType;
import org.springframework.hateoas.AffordanceModel.PayloadMetadata;
import org.springframework.hateoas.AffordanceModel.PropertyMetadata;
import org.springframework.hateoas.mediatype.PropertyUtils;

/**
 * The input metadata of a record, for affordances that create one. Spring HATEOAS derives
 * HAL-FORMS properties from bean properties and treats one without a setter as read-only, so
 * every component of a record, which is written through its constructor, would be advertised
 * as {@code readOnly}. This keeps everything else Spring derives, such as required, type and
 * pattern, and only corrects that.
 */
public record WritablePayloadMetadata(Class<? extends Record> type) implements PayloadMetadata {
    public static WritablePayloadMetadata of(Class<? extends Record> type) {
        return new WritablePayloadMetadata(type);
    }

    @Override
    public Stream<PropertyMetadata> stream() {
        return PropertyUtils.getExposedProperties(type).stream()
                .map(Writable::new);
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    private record Writable(PropertyMetadata delegate) implements PropertyMetadata {
        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public boolean isRequired() {
            return delegate.isRequired();
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public Optional<String> getPattern() {
            return delegate.getPattern();
        }

        @Override
        public ResolvableType getType() {
            return delegate.getType();
        }

        @Override
        public Number getMin() {
            return delegate.getMin();
        }

        @Override
        public Number getMax() {
            return delegate.getMax();
        }

        @Override
        public Long getMinLength() {
            return delegate.getMinLength();
        }

        @Override
        public Long getMaxLength() {
            return delegate.getMaxLength();
        }

        @Override
        public String getInputType() {
            return delegate.getInputType();
        }
    }
}
