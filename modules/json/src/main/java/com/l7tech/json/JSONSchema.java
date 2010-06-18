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
     * Validate the JSONData aganist this JSON Schema

     * @param jsonData JSON instance data to validate
     * @return List of String, a value for each validation error. Never null. Empty when there were no validation errors.
     */
    public List<String> validate(JSONData jsonData) throws IOException;
}
