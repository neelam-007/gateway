package com.l7tech.json;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.ReferenceResolver;

public class ConnectionBlockingUrlResolver implements ReferenceResolver {

    public static final String MESSAGE_NO_REMOTE_REFERENCES = "Remote references not allowed in JSON schemas";

    @Override
    public JsonSchema resolveReference(String reference) {
        throw new JsonSchemaException(MESSAGE_NO_REMOTE_REFERENCES);
    }
}
