package se.cloudneutral.orders.client.hypermedia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * A HAL representation. Relations are matched on their expanded form: a curied name such as
 * {@code os:products} is expanded through the declared curies to its full URI before comparing,
 * so clients depend on the relation URI, never on the prefix the server happens to choose.
 * Registered relations such as {@code next} have no prefix and are matched as is.
 */
public final class Representation {
    private final JsonNode body;

    // Curie name to href template, inherited by embedded resources that don't repeat them
    private final Map<String, String> curies;

    Representation(JsonNode body) {
        this(body, Map.of());
    }

    private Representation(JsonNode body, Map<String, String> inheritedCuries) {
        this.body = body;
        this.curies = new HashMap<>(inheritedCuries);
        body.path("_links").path("curies")
                .forEach(curie -> curies.put(curie.path("name").asString(), curie.path("href").asString()));
    }

    public Optional<Link> link(String rel) {
        return find(body.path("_links"), rel)
                .map(node -> Link.of(rel, node.isArray() ? node.get(0) : node));
    }

    public List<Representation> embedded(String rel) {
        List<Representation> representations = new ArrayList<>();
        find(body.path("_embedded"), rel).ifPresent(node -> {
            if (node.isArray()) {
                node.forEach(item -> representations.add(new Representation(item, curies)));
            } else {
                representations.add(new Representation(node, curies));
            }
        });
        return representations;
    }

    /**
     * @return the HAL-FORMS templates, empty unless the representation was requested as HAL-FORMS
     */
    public List<Template> templates() {
        String selfHref = link("self").map(Link::href).orElse(null);
        List<Template> templates = new ArrayList<>();
        body.path("_templates").properties()
                .forEach(entry -> templates.add(Template.of(entry.getKey(), entry.getValue(), selfHref)));
        return templates;
    }

    /**
     * @return the template describing how to act on a link, matched on its target
     */
    public Optional<Template> templateFor(Link link) {
        return templates().stream()
                .filter(template -> template.target().equals(link.href()))
                .findFirst();
    }

    /**
     * @return a state property of this representation, or a missing node
     */
    public JsonNode get(String property) {
        return body.path(property);
    }

    /**
     * @return the properties / state of this representation, without its hypermedia controls
     */
    public JsonNode properties() {
        ObjectNode state = (ObjectNode) body.deepCopy();
//        state.remove(List.of("_links", "_embedded", "_templates"));
        state.remove(List.of("_links", "_templates"));
        return state;
    }

    private Optional<JsonNode> find(JsonNode container, String rel) {
        return container.properties().stream()
                .filter(entry -> expand(entry.getKey()).equals(rel))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private String expand(String name) {
        int colon = name.indexOf(':');
        if (colon > 0) {
            String template = curies.get(name.substring(0, colon));
            if (template != null) {
                return template.replace("{rel}", name.substring(colon + 1));
            }
        }
        return name;
    }

    @Override
    public String toString() {
        return body.toPrettyString();
    }
}
