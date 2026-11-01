package se.cloudneutral.orders.client.hypermedia;

import tools.jackson.databind.JsonNode;

/**
 * A HAL-FORMS template, as found under {@code _templates}: a state transition the server
 * advertises as legal right now, with the method, media type and target to use.
 */
public record Template(String name, String method, String contentType, String target) {
    static Template of(String name, JsonNode node, String selfHref) {
        return new Template(name,
                node.path("method").asString(),
                node.path("contentType").asString("application/json"),
                // HAL-FORMS: an absent target means the resource itself
                node.path("target").asString(selfHref));
    }
}
