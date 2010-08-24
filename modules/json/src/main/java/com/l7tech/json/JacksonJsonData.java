/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.json;

import com.l7tech.util.ExceptionUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.JsonNodeFactory;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unfortunately depending on the usage of the JSOn data we currently need two different internal representations.
 * This is a result of the library we are using for JSON Schema validation. Most straight forward solution for now
 * was simply to lazily create the internal representation, which in some cases will lead to an instance of this class
 * having two internal representations. This would happen for example if a Message was both schema validated and scanned
 * for code injection.
 */
class JacksonJsonData implements JSONData {
    public JacksonJsonData(ObjectMapper mapper, String jsonData) {
        this.jsonData = jsonData;
        this.mapper = mapper;
    }

    @Override
    public String getJsonData() {
        return jsonData;
    }

    @Override
    public Object getJsonObject() throws InvalidJsonException {
        final Object pojo = jsonPojo.get();
        if(pojo == null){
            final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
            try {
                jsonPojo.compareAndSet(null, jsonNodeFactory.POJONode(mapper.readValue(jsonData, Object.class)).getPojo());
            } catch (IOException e) {
                throw new InvalidJsonException(e.getMessage());
            }
        }

        return jsonPojo.get();
    }

    // - PACKAGE

    JsonNode getJsonNode() throws InvalidJsonException {
        final JsonNode jNode = jsonNode.get();
        if(jNode == null){
            try {
                jsonNode.compareAndSet(null, mapper.readTree(jsonData));
            } catch (IOException e) {
                throw new InvalidJsonException(ExceptionUtils.getMessage(e));
            }
        }

        return jsonNode.get();
    }

    // - PRIVATE

    private final String jsonData;
    private final ObjectMapper mapper;
    private final AtomicReference<JsonNode> jsonNode = new AtomicReference<JsonNode>();
    private final AtomicReference<Object> jsonPojo = new AtomicReference<Object>();
}
