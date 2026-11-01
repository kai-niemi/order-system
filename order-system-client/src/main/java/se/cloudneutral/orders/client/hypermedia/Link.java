package se.cloudneutral.orders.client.hypermedia;

import java.net.URI;
import java.util.Map;

import org.springframework.hateoas.UriTemplate;

import tools.jackson.databind.JsonNode;

/**
 * A HAL link object, as found under {@code _links}.
 */
public record Link(String rel, String href, boolean templated, String title) {
    static Link of(String rel, JsonNode node) {
        return new Link(rel,
                node.path("href").asString(),
                node.path("templated").asBoolean(false),
                node.path("title").asString(null));
    }

    public URI toUri() {
        if (templated) {
            throw new IllegalStateException("Link '%s' is templated and must be expanded: %s".formatted(rel, href));
        }
        return URI.create(href);
    }

    /**
     * Expand a templated link, variables without a value are dropped as per RFC 6570.
     */
    public URI expand(Map<String, ?> variables) {
        return templated ? UriTemplate.of(href).expand(variables) : toUri();
    }
}
