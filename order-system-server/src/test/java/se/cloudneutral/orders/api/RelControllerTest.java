package se.cloudneutral.orders.api;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit-test")
class RelControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RelController()).build();
    }

    // Every relation the server emits, as a link or as an embedded collection
    static Stream<String> documentedRelations() {
        return Stream.of(
                LinkRelations.ACTUATORS_REL,
                LinkRelations.PRODUCT_REL,
                LinkRelations.PRODUCT_COLL_REL,
                LinkRelations.ORDER_REL,
                LinkRelations.ORDER_COLL_REL,
                LinkRelations.ORDER_COLL_REL + "-form",
                LinkRelations.ITEM_COLL_REL,
                "orderItems",
                LinkRelations.CUSTOMER_REL,
                LinkRelations.CUSTOMER_COLL_REL,
                LinkRelations.CANCEL_REL);
    }

    static Stream<String> guides() {
        return Stream.of("index", "create-order", "cancel-order");
    }

    // Every problem type the server emits, read from the constants so none can go undocumented
    static Stream<String> problemTypes() {
        return Arrays.stream(ProblemTypes.class.getFields())
                .filter(field -> field.getType().equals(URI.class))
                .map(field -> {
                    try {
                        String uri = field.get(null).toString();
                        String prefix = ProblemTypes.TYPE_URI_TEMPLATE.replace("{type}", "");
                        assertThat(uri).startsWith(prefix);
                        return uri.substring(prefix.length());
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    @ParameterizedTest
    @MethodSource("problemTypes")
    void servesMarkdownForProblemTypes(String type) throws Exception {
        mockMvc.perform(get("/problems/{type}", type).accept("text/markdown"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("## Semantics")))
                .andExpect(content().string(containsString("## Resolution")))
                .andExpect(content().string(not(containsString("/api/"))));
    }

    @ParameterizedTest
    @MethodSource("documentedRelations")
    void servesMarkdownForDocumentedRelations(String rel) throws Exception {
        mockMvc.perform(get("/rels/{rel}", rel).accept("text/markdown"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/markdown")))
                .andExpect(content().string(not(emptyString())))
                .andExpect(content().string(containsString("## Semantics")))
                .andExpect(content().string(containsString("## Related link rels")));
    }

    @ParameterizedTest
    @MethodSource("documentedRelations")
    void relationsDoNotDescribeResourceAddresses(String rel) throws Exception {
        mockMvc.perform(get("/rels/{rel}", rel).accept("text/markdown"))
                .andExpect(content().string(not(containsString("/api/"))));
    }

    @ParameterizedTest
    @MethodSource("guides")
    void servesGuides(String guide) throws Exception {
        mockMvc.perform(get("/guides/{guide}", guide).accept("text/markdown"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(emptyString())))
                .andExpect(content().string(not(containsString("/api/"))));
    }

    @Test
    void guidesAreNotRelations() throws Exception {
        mockMvc.perform(get("/rels/{rel}", "create-order"))
                .andExpect(status().isNotFound());
    }

    @Test
    void servesHtmlByDefault() throws Exception {
        mockMvc.perform(get("/rels/{rel}", LinkRelations.ORDER_REL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<h2>Semantics</h2>")));
    }

    @Test
    void returnsNotFoundForUnknownOrUnsafeRelationNames() throws Exception {
        mockMvc.perform(get("/rels/{rel}", "not-a-relation"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/rels/{rel}", "Uppercase"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/rels/{rel}", ".."))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/guides/{guide}", "..%2Frels%2Forder"))
                .andExpect(status().isNotFound());
    }

    @Test
    void relativeDocumentationLinksResolveThroughTheRelEndpoint() throws Exception {
        String relatedRel = URI.create("/rels/orders").resolve("order").getPath();

        mockMvc.perform(get("/rels/orders").accept("text/markdown"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("](order)")));

        mockMvc.perform(get(relatedRel))
                .andExpect(status().isOk());

        String guide = URI.create("/rels/orders-form").resolve("../guides/create-order").getPath();

        mockMvc.perform(get(guide))
                .andExpect(status().isOk());
    }
}
