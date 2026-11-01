package se.cloudneutral.orders.util;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static se.cloudneutral.orders.util.StreamUtils.chunkedStream;

public class StreamUtilsTest {
    @Test
    public void testChunkedStream() {
        try (Stream<List<Integer>> chunks =
                     chunkedStream(IntStream.range(0, 100)
                             .boxed(), 10)) {

            chunks.limit(3)
                    .forEach(System.out::println);
        }
    }
}
