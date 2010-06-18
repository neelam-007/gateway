/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.json;

import com.l7tech.util.ExceptionUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

class JacksonJsonData implements JSONData {

    public JacksonJsonData(ObjectMapper mapper, String jsonData) throws IOException, InvalidJsonException {
        this.jsonData = jsonData;
        try {
            jsonNode = mapper.readTree(jsonData);
        } catch (IOException e) {
            throw new InvalidJsonException(ExceptionUtils.getMessage(e));
        }
    }

    @Override
    public String getJsonData() {
        return jsonData;
    }

    public JsonNode getJsonNode() {
        return jsonNode;
    }

    // - PRIVATE

    private final String jsonData;
    private final JsonNode jsonNode;
}
