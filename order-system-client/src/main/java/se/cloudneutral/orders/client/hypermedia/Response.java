package se.cloudneutral.orders.client.hypermedia;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

/**
 * Outcome of submitting a template, for when the status and headers matter as well as the body.
 */
public record Response(HttpStatusCode status, HttpHeaders headers, Representation resource) {
}
