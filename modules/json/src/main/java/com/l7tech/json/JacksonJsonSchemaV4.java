package com.l7tech.json;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation has been provided via http://gitorious.org/json-schema-validation-in-java.
 *
 * The source code has been reviewed and deemed thread safe based on it's implementation.
 *
 * If any issues arise, then the JacksonSchema instance variable should be removed and an instance created inside of
 * validate to guarantee thread safety.
 */
class JacksonJsonSchemaV4 implements JSONSchema {

    private static final String MESSAGE_ILLEGAL_TYPE = "jsonData in not an instance of "
            + JacksonJsonDataFasterxml.class.getCanonicalName();

    private final JsonSchema jacksonSchema;
    private final JsonNode jsonNode;

    JacksonJsonSchemaV4(final JacksonJsonDataFasterxml jsonData) throws InvalidJsonException {
        this.jsonNode = jsonData.getJsonNode();
        try {
            this.jacksonSchema = new JsonSchemaFactory(new ObjectMapper(), new ConnectionBlockingUrlResolver()).getSchema(jsonNode);
        } catch (JsonSchemaException e) {
            throw new InvalidJsonException(e);
        }
    }

    @Override
    public List<String> validate(final JSONData jsonData) throws InvalidJsonException {
        if (!(jsonData instanceof JacksonJsonDataFasterxml)) {
            throw new IllegalArgumentException(MESSAGE_ILLEGAL_TYPE);
        }

        final List<String> result = new ArrayList<>();
        for (ValidationMessage message : jacksonSchema.validate(jsonData.getJsonNode())) {
            result.add(message.getMessage());
        }
        return result;
    }
}