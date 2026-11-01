package se.cloudneutral.orders.client.hypermedia;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
public class ResourceTest {
    private static final String PRODUCTS = "https://order-system.cloudneutral.se/rels/products";

    private static Representation parse(String json) {
        return new Representation(JsonMapper.builder().build().readTree(json));
    }

    @Test
    public void givenCuriedRel_thenExpectMatchOnExpandedUri() {
        Representation resource = parse("""
                {"_links": {
                  "os:products": {"href": "http://localhost:8090/api/products"},
                  "curies": [{"name": "os", "href": "https://order-system.cloudneutral.se/rels/{rel}", "templated": true}]
                }}""");

        assertThat(resource.link(PRODUCTS)).map(Link::href).hasValue("http://localhost:8090/api/products");
        assertThat(resource.link("os:products")).isEmpty();
        assertThat(resource.link("products")).isEmpty();
    }

    @Test
    public void givenRenamedPrefix_thenExpectSameRel() {
        Representation resource = parse("""
                {"_links": {
                  "shop:products": {"href": "http://localhost:8090/api/products"},
                  "curies": [{"name": "shop", "href": "https://order-system.cloudneutral.se/rels/{rel}", "templated": true}]
                }}""");

        assertThat(resource.link(PRODUCTS)).isPresent();
    }

    @Test
    public void givenSamePrefixForAnotherVocabulary_thenExpectNoMatch() {
        Representation resource = parse("""
                {"_links": {
                  "os:products": {"href": "http://localhost:8090/api/products"},
                  "curies": [{"name": "os", "href": "https://example.com/rels/{rel}", "templated": true}]
                }}""");

        assertThat(resource.link(PRODUCTS)).isEmpty();
    }

    @Test
    public void givenRegisteredOrFullUriRel_thenExpectMatchAsIs() {
        Representation resource = parse("""
                {"_links": {
                  "next": {"href": "http://localhost:8090/api/products?page=1"},
                  "https://order-system.cloudneutral.se/rels/products": {"href": "http://localhost:8090/api/products"}
                }}""");

        assertThat(resource.link("next")).isPresent();
        assertThat(resource.link(PRODUCTS)).isPresent();
    }

    @Test
    public void givenTemplates_thenExpectTemplateMatchedOnLinkTarget() {
        Representation resource = parse("""
                {"_links": {
                  "self": {"href": "http://localhost:8090/api/orders/1"},
                  "os:cancel": {"href": "http://localhost:8090/api/orders/1/cancel"},
                  "curies": [{"name": "os", "href": "https://order-system.cloudneutral.se/rels/{rel}", "templated": true}]
                },
                "_templates": {
                  "default": {"method": "PUT"},
                  "cancelOrder": {"method": "POST", "properties": [], "target": "http://localhost:8090/api/orders/1/cancel"}
                }}""");

        Link cancel = resource.link("https://order-system.cloudneutral.se/rels/cancel").orElseThrow();
        assertThat(resource.templateFor(cancel)).map(Template::method).hasValue("POST");

        // No target means the resource itself
        Link self = resource.link("self").orElseThrow();
        assertThat(resource.templateFor(self)).map(Template::method).hasValue("PUT");
    }

    @Test
    public void givenEmbeddedResources_thenExpectCuriesInheritedFromParent() {
        Representation resource = parse("""
                {"_embedded": {"os:products": [
                  {"sku": "A", "_links": {"os:product": {"href": "http://localhost:8090/api/products/a"}}},
                  {"sku": "B"}
                ]},
                "_links": {
                  "curies": [{"name": "os", "href": "https://order-system.cloudneutral.se/rels/{rel}", "templated": true}]
                }}""");

        assertThat(resource.embedded(PRODUCTS)).hasSize(2);
        assertThat(resource.embedded(PRODUCTS).getFirst()
                .link("https://order-system.cloudneutral.se/rels/product")).isPresent();
    }
}
