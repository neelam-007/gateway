/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Interface for JSON Schemas
 *
 * @author darmstrong
 */
package com.l7tech.json;

import java.io.IOException;
import java.util.List;

public interface JSONSchema {

    /**
     * Get the JSON Schema.
     * @return String JSON Schema. Never null.
     */
    public String getSchemaString();

    /**
     * Validate the JSONData against this JSON Schema.
     *
     * Instances will be used in a manner that assumes that all instances are thread safe. It is up to the implementation
     * to deal with this assumption and ensure that validation is always thread safe for a single instance of this
     * interface being shared by multiple threads.
     *
     * @param jsonData JSON instance data to validate
     * @return List of String, a value for each validation error. Never null. Empty when there were no validation errors.
     * @throws InvalidJsonException if the JSON schema is invalid JSON data.
     */
    public List<String> validate(JSONData jsonData) throws InvalidJsonException;
}
