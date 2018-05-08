package com.l7tech.json;

import com.l7tech.util.ExceptionUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.POJONode;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unfortunately depending on the usage of the JSON data we currently need two different internal representations.
 * This is a result of the library we are using for JSON Schema validation. Most straight forward solution for now
 * was simply to lazily create the internal representation, which in some cases will lead to an instance of this class
 * having two internal representations. This would happen for example if a Message was both schema validated and scanned
 * for code injection.
 */
public class JacksonJsonDataCodehaus implements JSONData {

    private final String jsonData;
    private final ObjectMapper mapper;
    private final AtomicReference<JsonNode> jsonNode = new AtomicReference<JsonNode>();
    private final AtomicReference<Object> jsonPojo = new AtomicReference<Object>();

    public JacksonJsonDataCodehaus(final ObjectMapper mapper, final String jsonData) {
        this.jsonData = jsonData;
        this.mapper = mapper;
    }

    @Override
    public String getJsonData() {
        return jsonData;
    }

    @Override
    public Object getJsonObject() throws InvalidJsonException {
        getJsonThing(() -> {
            final Object pojo = mapper.readValue(jsonData, Object.class);
            final POJONode pojoNode = JsonNodeFactory.instance.POJONode(pojo);
            jsonPojo.compareAndSet(null, pojoNode.getPojo());
        });

        return jsonPojo.get();
    }

    @Override
    public JsonNode getJsonNode() throws InvalidJsonException {
        getJsonThing(() -> jsonNode.compareAndSet(null, mapper.readTree(jsonData)));
        return jsonNode.get();
    }

    private void getJsonThing(JSONDataCommand command) throws InvalidJsonException {
        if(jsonNode.get() == null){
            try {
                command.execute();
            } catch (IOException e) {
                throw new InvalidJsonException(ExceptionUtils.getMessage(e));
            }
        }
    }

}
