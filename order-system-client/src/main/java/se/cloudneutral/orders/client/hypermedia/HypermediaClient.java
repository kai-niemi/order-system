package se.cloudneutral.orders.client.hypermedia;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * HTTP client that knows a single URI, the API root, and reaches everything else by
 * following link relations advertised in HAL representations, and by submitting the
 * HAL-FORMS templates they advertise.
 */
@Component
public class HypermediaClient {
    public static final String HAL_JSON = "application/hal+json";

    public static final String HAL_FORMS_JSON = "application/prs.hal-forms+json";

    private final RestClient restClient;

    private final URI rootUri;

    public HypermediaClient(RestClient.Builder builder,
                            JsonMapper jsonMapper,
                            @Value("${app.api.root-uri}") URI rootUri) {
        this.rootUri = rootUri;
        this.restClient = builder
                .defaultHeader(HttpHeaders.ACCEPT, HAL_JSON)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    throw ProblemException.of(request.getMethod(), request.getURI(),
                            response.getStatusCode(), jsonMapper.readTree(response.getBody()));
                })
                .build();
    }

    public Representation root() {
        return get(rootUri);
    }

    /**
     * Follow a link relation that the resource is expected to advertise.
     */
    public Representation follow(Representation from, String rel) {
        return followIfPresent(from, rel)
                .orElseThrow(() -> new IllegalStateException("No '%s' link in resource".formatted(rel)));
    }

    /**
     * Follow a link relation that may or may not be advertised, such as {@code next} on the last page.
     */
    public Optional<Representation> followIfPresent(Representation from, String rel) {
        return from.link(rel).map(link -> get(link.toUri()));
    }

    public Representation get(URI uri) {
        return get(uri, HAL_JSON);
    }

    /**
     * @param accept the representation to ask for, HAL-FORMS to also receive the templates
     */
    public Representation get(URI uri, String accept) {
        return exchange(uri, () -> new Representation(restClient.get()
                .uri(uri)
                .header(HttpHeaders.ACCEPT, accept)
                .retrieve()
                .body(JsonNode.class)));
    }

    /**
     * Submit a HAL-FORMS template, using the method, target and content type it advertises.
     *
     * @param body the request body, or null for templates without properties
     */
    public Response submit(Template template, JsonNode body, Map<String, String> headers) {
        URI target = URI.create(template.target());
        return exchange(target, () -> {
            RestClient.RequestBodySpec request = restClient.method(HttpMethod.valueOf(template.method()))
                    .uri(target)
                    .headers(h -> headers.forEach(h::set));
            if (body != null) {
                request.contentType(MediaType.parseMediaType(template.contentType()))
                        .body(body);
            }
            ResponseEntity<JsonNode> entity = request
                    .retrieve()
                    .toEntity(JsonNode.class);
            return new Response(entity.getStatusCode(), entity.getHeaders(), new Representation(entity.getBody()));
        });
    }

    private static <T> T exchange(URI uri, Supplier<T> call) {
        try {
            return call.get();
        } catch (ResourceAccessException e) {
            // The shell reports the innermost cause, which for a refused connection has no message
            IllegalStateException ex = new IllegalStateException("Unable to reach %s (%s)"
                    .formatted(uri, NestedExceptionUtils.getMostSpecificCause(e).getClass().getSimpleName()));
            ex.addSuppressed(e);
            throw ex;
        }
    }
}
