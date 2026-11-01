package se.cloudneutral.orders.util;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
public class DigestUtilsTest {
    @Test
    public void givenDifferentKeyOrder_thenExpectSameFingerprint() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("x", 1);
        a.put("y", 2);
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("y", 2);
        b.put("x", 1);

        assertThat(DigestUtils.toCanonicalHash("POST", "/api/orders", a))
                .isEqualTo(DigestUtils.toCanonicalHash("POST", "/api/orders", b));
    }

    @Test
    public void givenNullVersusAbsentField_thenExpectSameFingerprint() {
        Map<String, Object> withNull = new LinkedHashMap<>();
        withNull.put("x", 1);
        withNull.put("note", null);

        assertThat(DigestUtils.toCanonicalHash("POST", "/p", withNull))
                .isEqualTo(DigestUtils.toCanonicalHash("POST", "/p", Map.of("x", 1)));
    }

    @Test
    public void givenDifferentNumberScale_thenExpectSameFingerprint() {
        assertThat(DigestUtils.toCanonicalHash("POST", "/p", Map.of("price", new BigDecimal("10.50"))))
                .isEqualTo(DigestUtils.toCanonicalHash("POST", "/p", Map.of("price", new BigDecimal("10.5"))));
        assertThat(DigestUtils.toCanonicalHash("POST", "/p", Map.of("n", new BigDecimal("1.00"))))
                .isEqualTo(DigestUtils.toCanonicalHash("POST", "/p", Map.of("n", 1)));
    }

    @Test
    public void givenDifferentValueMethodOrPath_thenExpectDifferentFingerprint() {
        byte[] base = DigestUtils.toCanonicalHash("POST", "/p", Map.of("x", 1));

        assertThat(DigestUtils.toCanonicalHash("POST", "/p", Map.of("x", 2))).isNotEqualTo(base);
        assertThat(DigestUtils.toCanonicalHash("PUT", "/p", Map.of("x", 1))).isNotEqualTo(base);
        assertThat(DigestUtils.toCanonicalHash("POST", "/q", Map.of("x", 1))).isNotEqualTo(base);
    }

    @Test
    public void thenExpectThirtyTwoByteFingerprint() {
        assertThat(DigestUtils.toCanonicalHash("POST", "/p", Map.of("x", 1))).hasSize(32);
    }
}
