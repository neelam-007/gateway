package com.l7tech.json;

import org.codehaus.jackson.map.ObjectMapper;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for creation of JSONData and JSONSchema.
 *
 * Different implementations e.g. javascript or other Java implementation can be managed here inside this module.
 *
 * Copyright (C) 2010, Layer 7 Technologies Inc.
 * @author darmstrong
 */
public class JSONFactory {

    //- PUBLIC
    public static JSONFactory getInstance(){
        return instance;
    }

    public JSONData newJsonData(final String jsonData) {
        return new JacksonJsonData(OBJECT_MAPPER, jsonData);
    }

    public JSONSchema newJsonSchema(final String jsonSchema) throws IOException, InvalidJsonException {
        return newJsonSchema(newJsonData(jsonSchema));
    }

    public JSONSchema newJsonSchema(final JSONData jsonSchema) throws IOException, InvalidJsonException {
        return new JacksonJsonSchema(OBJECT_MAPPER, jsonSchema);
    }

    //- PRIVATE

    private final static JSONFactory instance = new JSONFactory();
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final static Logger logger = Logger.getLogger(JSONFactory.class.getName());
}
