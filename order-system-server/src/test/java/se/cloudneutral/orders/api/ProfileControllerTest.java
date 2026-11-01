package se.cloudneutral.orders.api;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import se.cloudneutral.orders.config.JacksonConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit-test")
class ProfileControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProfileController())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(new JacksonConfig().primaryObjectMapper()))
                .build();
    }

    static Stream<String> profiles() {
        return Stream.of(
                ProfileController.ORDER,
                ProfileController.ORDER_ITEM,
                ProfileController.ORDER_REQUEST,
                ProfileController.PRODUCT,
                ProfileController.CUSTOMER);
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void servesSchemaWithStableIdentity(String profile) throws Exception {
        mockMvc.perform(get("/profiles/{profile}", profile))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(ProfileController.SCHEMA_JSON_VALUE))
                .andExpect(jsonPath("$['$schema']").value("https://json-schema.org/draft/2020-12/schema"))
                .andExpect(jsonPath("$['$id']").value(
                        ProfileController.PROFILE_URI_TEMPLATE.replace("{profile}", profile)))
                .andExpect(jsonPath("$.type").value("object"))
                .andExpect(jsonPath("$.description").isNotEmpty())
                // Hypermedia controls belong to HAL, not to the state the schema describes
                .andExpect(jsonPath("$.properties.links").doesNotExist())
                .andExpect(jsonPath("$.properties._links").doesNotExist());
    }

    @Test
    void orderSchemaDescribesItsStatesAndFormats() throws Exception {
        mockMvc.perform(get("/profiles/{profile}", ProfileController.ORDER))
                .andExpect(jsonPath("$.properties.status.enum",
                        containsInAnyOrder("placed", "confirmed", "cancelled", "delivered")))
                .andExpect(jsonPath("$.properties.datePlaced.format").value("date-time"))
                .andExpect(jsonPath("$.properties.deliveryAddress.properties.postcode.type").value("string"))
                .andExpect(jsonPath("$.properties.id").doesNotExist());
    }

    @Test
    void orderRequestSchemaCarriesTheValidationRules() throws Exception {
        mockMvc.perform(get("/profiles/{profile}", ProfileController.ORDER_REQUEST))
                .andExpect(jsonPath("$.required", containsInAnyOrder("customerId", "items")))
                .andExpect(jsonPath("$.properties.customerId.format").value("uuid"))
                .andExpect(jsonPath("$.properties.items.minItems").value(1))
                .andExpect(jsonPath("$.properties.items.items.required", containsInAnyOrder("productId", "quantity")))
                .andExpect(jsonPath("$.properties.items.items.properties.quantity.exclusiveMinimum").value(0));
    }

    @Test
    void linksWithTheRegisteredSpellingOfDescribedBy() {
        // Spring's own constant renders as "describedBy", which HAL clients would not find
        assertThat(ProfileController.DESCRIBED_BY.value()).isEqualTo("describedby");
        // Registered, so the curie provider leaves it without a prefix
        assertThat(IanaLinkRelations.isIanaRel(ProfileController.DESCRIBED_BY)).isTrue();
    }

    @Test
    void returnsNotFoundForUnknownProfile() throws Exception {
        mockMvc.perform(get("/profiles/{profile}", "nope"))
                .andExpect(status().isNotFound());
    }
}
