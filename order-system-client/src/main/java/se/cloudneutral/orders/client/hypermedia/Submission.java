package se.cloudneutral.orders.client.hypermedia;

import java.util.Map;

import tools.jackson.databind.JsonNode;

public record Submission(Template template, JsonNode body, Map<String, String> headers) {
    public static Submission of(Template template, JsonNode body, Map<String, String> headers) {
        return new Submission(template, body, headers);
    }

    public static Submission of(Template template, Map<String, String> headers) {
        return new Submission(template, null, headers);
    }

    public static Submission of(Template template) {
        return new Submission(template, null, Map.of());
    }
}