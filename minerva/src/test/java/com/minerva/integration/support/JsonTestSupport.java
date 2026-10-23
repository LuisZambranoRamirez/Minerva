package com.minerva.integration.support;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class JsonTestSupport {

    private final ObjectMapper objectMapper;

    public JsonTestSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    public JsonNode read(String json) {
        return objectMapper.readTree(json);
    }

    public String text(String json, String fieldName) {
        JsonNode value = read(json).get(fieldName);
        if (value == null || !value.isString()) {
            throw new IllegalArgumentException("Missing textual JSON field: " + fieldName);
        }
        return value.stringValue();
    }
}
