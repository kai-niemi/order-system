package se.cloudneutral.orders.client.hypermedia;

import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;

import tools.jackson.databind.JsonNode;

/**
 * An error response, carrying the RFC 9457 problem details returned by the server.
 */
public class ProblemException extends RuntimeException {
    static ProblemException of(HttpMethod method, URI uri, HttpStatusCode status, JsonNode problem) {
        String title = problem.path("title").asString(status.toString());
        String detail = problem.path("detail").asString("");
        return new ProblemException("%s %s: %d %s %s".formatted(method, uri, status.value(), title, detail).strip(),
                status, problem);
    }

    private final HttpStatusCode status;

    private final transient JsonNode problem;

    public ProblemException(String message, HttpStatusCode status, JsonNode problem) {
        super(message);
        this.status = status;
        this.problem = problem;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public JsonNode getProblem() {
        return problem;
    }
}
