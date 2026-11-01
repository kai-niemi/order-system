package se.cloudneutral.orders.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import se.cloudneutral.orders.api.model.CustomerModel;
import se.cloudneutral.orders.api.model.OrderItemModel;
import se.cloudneutral.orders.api.model.OrderModel;
import se.cloudneutral.orders.api.model.ProductModel;
import se.cloudneutral.orders.api.payload.CreateOrderRequest;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Serves a JSON Schema for the state of each representation, generated from the model classes so
 * it cannot drift from what is sent. Representations link to their schema with the registered
 * {@code describedby} relation, as JSON Schema recommends. The schema covers the state properties
 * only: {@code _links}, {@code _embedded} and {@code _templates} are defined by HAL and HAL-FORMS.
 * <p>
 * Like relations and problem types, a schema is identified by a URI that does not depend on where
 * the server is deployed, its {@code $id}, while the link points to where this server serves it.
 */
@RestController
public class ProfileController {
    public static final String SCHEMA_JSON_VALUE = "application/schema+json";

    public static final String PROFILE_URI_TEMPLATE = "https://order-system.cloudneutral.se/profiles/{profile}";

    public static final String ORDER = "order";

    public static final String ORDER_ITEM = "order-item";

    public static final String ORDER_REQUEST = "order-request";

    public static final String PRODUCT = "product";

    public static final String CUSTOMER = "customer";

    private static final Map<String, Class<?>> PROFILES = Map.of(
            ORDER, OrderModel.class,
            ORDER_ITEM, OrderItemModel.class,
            ORDER_REQUEST, CreateOrderRequest.class,
            PRODUCT, ProductModel.class,
            CUSTOMER, CustomerModel.class);

    // As spelled in the IANA registry. IanaLinkRelations.DESCRIBED_BY renders as "describedBy", and
    // although registered relations compare case-insensitively, HAL clients look up JSON keys as is.
    public static final LinkRelation DESCRIBED_BY = LinkRelation.of("describedby");

    public static Link describedBy(String profile) {
        return linkTo(methodOn(ProfileController.class)
                .profile(profile))
                .withRel(DESCRIBED_BY);
    }

    private final Map<String, ObjectNode> schemas = new LinkedHashMap<>();

    public ProfileController() {
        SchemaGeneratorConfigBuilder builder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(new JacksonSchemaModule())
                .with(new JakartaValidationModule(
                        JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED,
                        JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS));
        // The links of a representation model are rendered as _links, which belongs to HAL
        builder.forFields().withIgnoreCheck(field ->
                field.getDeclaringType().getErasedType() == RepresentationModel.class);
        // Primitives are always rendered, and a missing one is read as zero, never as absent
        builder.forFields().withRequiredCheck(field -> field.getType().getErasedType().isPrimitive());

        SchemaGenerator generator = new SchemaGenerator(builder.build());

        PROFILES.forEach((profile, type) -> {
            ObjectNode generated = generator.generateSchema(type);
            ObjectNode schema = JsonNodeFactory.instance.objectNode();
            schema.set("$schema", generated.remove("$schema"));
            schema.put("$id", PROFILE_URI_TEMPLATE.replace("{profile}", profile));
            schema.put("title", profile);
            schema.setAll(generated);
            schemas.put(profile, schema);
        });
    }

    @GetMapping(value = "/profiles/{profile}", produces = {SCHEMA_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ObjectNode> profile(@PathVariable String profile) {
        ObjectNode schema = schemas.get(profile);
        if (schema == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(schema);
    }
}
