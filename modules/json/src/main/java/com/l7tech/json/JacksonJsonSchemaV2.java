
/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.json;

import com.l7tech.util.ExceptionUtils;
import eu.vahlas.json.schema.JSONSchemaException;
import eu.vahlas.json.schema.impl.JSONValidator;
import eu.vahlas.json.schema.impl.JacksonSchema;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.List;

/**
 * Implementation has been provided via http://gitorious.org/json-schema-validation-in-java.
 *
 * The source code has been reviewed and deemed thread safe based on it's implementation.
 *
 * If any issues arise, then the JacksonSchema instance variable should be removed and an instance created inside of
 * validate to guarantee thread safety.
 */
class JacksonJsonSchemaV2 implements JSONSchema {

    private static final String MESSAGE_ILLEGAL_TYPE = "jsonData in not an instance of "
            + JacksonJsonDataCodehaus.class.getCanonicalName();

    private final JsonNode jsonNode;
    private final JacksonSchema jacksonSchema;

    JacksonJsonSchemaV2(final JacksonJsonDataCodehaus jsonData) throws InvalidJsonException{
        this.jsonNode = jsonData.getJsonNode();
        this.jacksonSchema = new JacksonSchema(jsonNode);
    }

    @Override
    public List<String> validate(final JSONData jsonData) throws InvalidJsonException {
        if (!(jsonData instanceof JacksonJsonDataCodehaus)) {
            throw new IllegalArgumentException(MESSAGE_ILLEGAL_TYPE);
        }

        return jacksonSchema.validate(jsonData.getJsonNode(), JSONValidator.AT_ROOT);
    }

}
