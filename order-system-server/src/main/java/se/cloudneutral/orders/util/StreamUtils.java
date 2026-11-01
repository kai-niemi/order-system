package se.cloudneutral.orders.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class StreamUtils {
    private StreamUtils() {
    }

    public static <T> Stream<List<T>> chunkedStream(
            Stream<T> stream,
            int chunkSize) {

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }

        Spliterator<T> source = stream.sequential().spliterator();

        Spliterator<List<T>> chunks = new Spliterators.AbstractSpliterator<>(
                source.estimateSize() == Long.MAX_VALUE
                        ? Long.MAX_VALUE
                        : source.estimateSize() / chunkSize
                          + (source.estimateSize() % chunkSize == 0 ? 0 : 1),
                Spliterator.ORDERED | Spliterator.NONNULL) {

            @Override
            public boolean tryAdvance(Consumer<? super List<T>> action) {
                List<T> batch = new ArrayList<>(chunkSize);

                while (batch.size() < chunkSize &&
                       source.tryAdvance(batch::add)) {
                    // Collect elements into batch
                }

                if (batch.isEmpty()) {
                    return false;
                }

                action.accept(batch);
                return true;
            }
        };

        return StreamSupport.stream(chunks, false)
                .onClose(stream::close);
    }
}
