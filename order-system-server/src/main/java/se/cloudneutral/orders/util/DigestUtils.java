package se.cloudneutral.orders.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Computes a SHA-256 fingerprint of an object from its JSON form, with map entries sorted by key
 * so that equal objects hash the same. Used to check that agents run the same model.
 */
public abstract class DigestUtils {
    private static final JsonMapper mapper = JsonMapper.builder()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS) // determinism
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY) // determinism
            .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    public static final String ALGORITHM = "SHA-256";

    public static <T> String toJson(T object) {
        return mapper.writeValueAsString(object);
    }

    public static <T> String toSecureHash(T object) {
        try {
            String normalizedJson = mapper.writeValueAsString(object);
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(normalizedJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Renders an object as canonical JSON: object keys sorted, null members dropped (so null and
     * absent are equivalent), numbers normalized to plain decimal form without trailing zeros
     * (so 1, 1.0 and 1.00 are equal) and no insignificant whitespace. Array order is preserved.
     */
    public static String toCanonicalJson(Object object) {
        StringBuilder sb = new StringBuilder();
        appendCanonical(mapper.valueToTree(object), sb);
        return sb.toString();
    }

    /**
     * Computes the raw 32 byte SHA-256 of the HTTP method, path and canonical JSON of the body.
     */
    public static byte[] toCanonicalHash(String method, String path, Object body) {
        try {
            String canonical = method.toUpperCase() + "\n" + path + "\n" + toCanonicalJson(body);
            return MessageDigest.getInstance(ALGORITHM)
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void appendCanonical(JsonNode node, StringBuilder sb) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            sb.append("null");
        } else if (node.isObject()) {
            Map<String, JsonNode> sorted = new TreeMap<>();
            node.properties().forEach(e -> {
                if (!e.getValue().isNull()) {
                    sorted.put(e.getKey(), e.getValue());
                }
            });
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, JsonNode> e : sorted.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(mapper.writeValueAsString(e.getKey())).append(':');
                appendCanonical(e.getValue(), sb);
            }
            sb.append('}');
        } else if (node.isArray()) {
            sb.append('[');
            for (int i = 0; i < node.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                appendCanonical(node.get(i), sb);
            }
            sb.append(']');
        } else if (node.isNumber()) {
            BigDecimal number = node.decimalValue();
            sb.append(number.signum() == 0 ? "0" : number.stripTrailingZeros().toPlainString());
        } else {
            sb.append(mapper.writeValueAsString(node));
        }
    }

    private DigestUtils() {
    }
}
