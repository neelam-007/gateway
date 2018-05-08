package com.l7tech.json;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unfortunately depending on the usage of the JSOn data we currently need two different internal representations.
 * This is a result of the library we are using for JSON Schema validation. Most straight forward solution for now
 * was simply to lazily create the internal representation, which in some cases will lead to an instance of this class
 * having two internal representations. This would happen for example if a Message was both schema validated and scanned
 * for code injection.
 */
public class JacksonJsonDataFasterxml implements JSONData {

    private static final String MESSAGE_EXPECTED_EOF = "Expected EOF but found trailing tokens.";

    private final String jsonSchemaString;
    private final ObjectMapper mapper;
    private final AtomicReference<JsonNode> jsonNode = new AtomicReference<>();
    private final AtomicReference<Object> jsonPojo = new AtomicReference<>();

    public JacksonJsonDataFasterxml(final ObjectMapper mapper, final String jsonSchema) {
        this.jsonSchemaString = jsonSchema;
        this.mapper = mapper;
    }

    public String getJsonData() {
        return jsonSchemaString;
    }

    @Override
    public Object getJsonObject() throws InvalidJsonException {
        getJsonThing(() -> jsonPojo.compareAndSet(null, readValue()));
        return jsonPojo.get();
    }

    @Override
    public JsonNode getJsonNode() throws InvalidJsonException {
        getJsonThing(() -> jsonNode.compareAndSet(null, readTree()));
        return jsonNode.get();
    }

    private void getJsonThing(JSONDataCommand command) throws InvalidJsonException {
        if (jsonNode.get() == null) {
            try {
                command.execute();
            } catch (IOException e) {
                throw new InvalidJsonException(ExceptionUtils.getMessage(e));
            }
        }
    }

    private Object readValue() throws IOException {
        final JsonParser parser = mapper.getFactory().createParser(jsonSchemaString);

        final Object jsonObj;
        try {
            jsonObj = mapper.readValue(parser, Object.class);
        } catch (JsonParseException e) {
            throw new JsonParseException(parser, e.getOriginalMessage(), new HiddenSourceJsonLocation(parser.getCurrentLocation()), e);
        }

        checkForTrailingTokens(parser);

        return jsonObj;
    }

    private JsonNode readTree() throws IOException, InvalidJsonException {
        final JsonParser parser = mapper.getFactory().createParser(jsonSchemaString);
        final JsonNode jsonNode;

        try {
            jsonNode = mapper.readTree(parser);
        } catch (JsonParseException e) {
            throw new JsonParseException(parser, e.getOriginalMessage(), new HiddenSourceJsonLocation(parser.getCurrentLocation()), e);
        }

        if (jsonNode == null) {
            throw new InvalidJsonException("No content to map due to end-of-input");
        }

        checkForTrailingTokens(parser);

        return jsonNode;
    }

    private static void checkForTrailingTokens(final JsonParser parser) throws IOException {
        // The following is needed to catch invalid json until upgrading to jackson 2.9 by utilizing the
        // DeserializationFeature.FAIL_ON_TRAILING_TOKENS
        final JsonToken token;
        try {
            token = parser.nextToken();
        } catch (JsonParseException e) {
            throw new JsonParseException(parser, MESSAGE_EXPECTED_EOF, new HiddenSourceJsonLocation(parser.getTokenLocation()), e);
        }

        if (token != null) {
            throw new JsonParseException(parser, MESSAGE_EXPECTED_EOF, new HiddenSourceJsonLocation(parser.getTokenLocation()));
        }
    }
}
