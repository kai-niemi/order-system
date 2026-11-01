package se.cloudneutral.orders.api.model;

/**
 * One of the possible values of a HAL-FORMS property, as an element of the plain JSON array that
 * an {@code options.link} refers to. The member names are the HAL-FORMS defaults for
 * {@code promptField} and {@code valueField}.
 */
public record OptionModel(String prompt, Object value) {
}
