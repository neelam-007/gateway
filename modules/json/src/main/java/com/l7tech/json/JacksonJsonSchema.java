/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.json;

import com.l7tech.util.ExceptionUtils;
import eu.vahlas.json.schema.*;
import eu.vahlas.json.schema.impl.JSONValidator;
import eu.vahlas.json.schema.impl.JacksonSchema;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.List;

class JacksonJsonSchema implements JSONSchema{

    public JacksonJsonSchema(ObjectMapper mapper, JSONData jsonData) throws IOException, InvalidJsonException{
        this.schemaString = jsonData.getJsonData();

        if (jsonData instanceof JacksonJsonData){
            JacksonJsonData jacksonJson = (JacksonJsonData) jsonData;
            jacksonSchema = new JacksonSchema(mapper, jacksonJson.getJsonNode());
        } else{
            final JsonNode jsonNode;
            try {
                jsonNode = mapper.readTree(jsonData.getJsonData());
            } catch (IOException e) {
                throw new InvalidJsonException(ExceptionUtils.getMessage(e));
            }
            jacksonSchema = new JacksonSchema(mapper, jsonNode);
        }
    }

    @Override
    public String getSchemaString() {
        return schemaString;
    }

    @Override
    public List<String> validate(JSONData jsonData) throws IOException{
        if (jsonData instanceof JacksonJsonData){
            JacksonJsonData jacksonJson = (JacksonJsonData) jsonData;
            final JsonNode jsonInstanceNode = jacksonJson.getJsonNode();
            return jacksonSchema.validate(jsonInstanceNode, JSONValidator.AT_ROOT);
        }

        try {
            return jacksonSchema.validate(jsonData.getJsonData());
        } catch (JSONSchemaException e) {
            throw new IOException(e);
        }
    }

    // - PRIVATE

    private final JacksonSchema jacksonSchema;
    private final String schemaString;
}
