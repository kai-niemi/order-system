package se.cloudneutral.orders.config;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mediatype.MediaTypeConfigurationCustomizer;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.DefaultCurieProvider;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;
import org.springframework.http.MediaType;

import se.cloudneutral.orders.api.CustomerController;
import se.cloudneutral.orders.api.LinkRelations;
import se.cloudneutral.orders.api.payload.CreateOrderRequest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Configuration
public class HypermediaConfig {
    // As an HTML5 pattern attribute, which is implicitly anchored
    public static final String UUID_PATTERN =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    @Bean
    public CurieProvider defaultCurieProvider() {
        return new DefaultCurieProvider(LinkRelations.CURIE_NAMESPACE, UriTemplate.of(LinkRelations.REL_URI_TEMPLATE));
    }

    /**
     * Describes HAL-FORMS properties beyond what their type and validation annotations say. HAL-FORMS
     * properties are flat, so this only reaches top-level properties: the line items of an order
     * request are described by its JSON Schema instead.
     */
    @Bean
    public MediaTypeConfigurationCustomizer<HalFormsConfiguration> halFormsCustomizer() {
        return configuration -> configuration
                .withPattern(UUID.class, UUID_PATTERN)
                // The options are rendered per request, so the link points to the server answering it
                .withOptions(CreateOrderRequest.class, "customerId", metadata -> HalFormsOptions
                        .remote(linkTo(methodOn(CustomerController.class)
                                .customerOptions())
                                .withRel("options")
                                .withType(MediaType.APPLICATION_JSON_VALUE))
                        .withPromptField("prompt")
                        .withValueField("value")
                        .withMinItems(1L)
                        .withMaxItems(1L));
    }
}
