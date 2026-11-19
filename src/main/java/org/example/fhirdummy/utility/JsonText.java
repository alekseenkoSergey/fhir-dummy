package org.example.fhirdummy.utility;

import tools.jackson.databind.JsonNode;

public final class JsonText {
    private JsonText() {}

    public static String optional(JsonNode source, String field) {
        JsonNode value = source.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isString()) {
            throw new IllegalArgumentException("Expected text for field " + field);
        }
        String text = value.asString().trim();
        return text.isEmpty() ? null : text;
    }

    public static String required(JsonNode source, String field) {
        String text = optional(source, field);
        if (text == null) {
            throw new IllegalArgumentException("Expected nonempty text for field " + field);
        }
        return text;
    }

    public static String identifier(JsonNode source, String field) {
        JsonNode value = source.path(field);
        return value.isIntegralNumber() ? value.toString() : optional(source, field);
    }
}
