package com.l7tech.external.assertions.evaluatejsonpathexpressionv2;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.*;
import com.jayway.jsonpath.spi.json.AbstractJsonProvider;
import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.Evaluator.EvaluatorException;
import com.l7tech.util.CollectionUtils;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.evaluatejsonpathexpressionv2.JsonPathEvaluator.LOGGER;

/**
 * <p>
 * This class contains all the supported {@link Evaluator}s. New implementations can be added here.
 * </p>
 */
public class JsonPathEvaluator {


    static final Logger LOGGER = Logger.getLogger(JsonPathEvaluator.class.getCanonicalName());

    public static final Map<String, Evaluator> evaluators = CollectionUtils.MapBuilder.<String, Evaluator>builder()
            /*
             * The {@link Evaluator} based on JSONPath.  Please refer to <a href='http://code.google.com/p/json-path/'>http://code.google.com/p/json-path/</a> and
             * <a href='http://goessner.net/articles/JsonPath/'>http://goessner.net/articles/JsonPath/</a>.
             */
            .put(EvaluateJsonPathExpressionV2Assertion.JSONPATH_EVALUATOR, (s, e) -> evaluate(s, e, false))
            /*
             * JSON Path evaluator with compression. That means, it returns json results in MAX compressed json-style. For more information, refer to JSONStyle.
             * The {@link Evaluator} based on JSONPath.  Please refer to <a href='http://code.google.com/p/json-path/'>http://code.google.com/p/json-path/</a> and
             * <a href='http://goessner.net/articles/JsonPath/'>http://goessner.net/articles/JsonPath/</a>.
             */
            .put(EvaluateJsonPathExpressionV2Assertion.JSONPATH_COMPRESSION_EVALUATOR, (s, e) -> evaluate(s, e, true))
            .unmodifiableMap();

    private static final Configuration CONFIG_JACKSON = Configuration.builder()
            .jsonProvider(new TrailingTokenRejectingJacksonProvider()).build();

    private static final Configuration CONFIG_RECURSIVE_DESCENT = Configuration.defaultConfiguration()
            .addOptions(Option.AS_PATH_LIST);

    private static JsonPathExpressionResult evaluate(final String source, final String expression,
                                                     final boolean jsonCompression)
            throws EvaluatorException {

        final JsonPath path;
        try {
            path = JsonPath.compile(expression);
        } catch (RuntimeException e) {
            throw new EvaluatorException("Error compiling expression: " + e.getMessage(), e);
        }

        // As per jsonpath 2.2.0, setting JSONValue.COMPRESSION to JSONStyle.MAX_COMPRESS before parsing
        // returns JSON results in Max compressed JSON style

        JSONValue.COMPRESSION = jsonCompression ? JSONStyle.MAX_COMPRESS : JSONStyle.NO_COMPRESS;

        try {
            final List<String> results = new ArrayList<>();
            final Object jsonResults = path.read(source, CONFIG_JACKSON);

            if (jsonResults instanceof List) {
                final List<?> jsonResultList = (List) jsonResults;
                if (jsonResultList.isEmpty()) {
                    // Defect: DE278819, the library returns empty array for expressions that contains recursive desent ($..), filter expression (?()) etc
                    // and if the key mentioned in expression is not present in json data, wrongly mimicking key existence
                    // if the result is empty array, the code below tries to fetch the path, if the path is not found, it throws PathNotFoundException
                    JsonPath.using(CONFIG_RECURSIVE_DESCENT).parse(source).read(expression);

                } else {
                    for (Object item : jsonResultList) {
                        results.add(toJsonString(item));
                    }
                }
            } else {
                results.add(toJsonString(jsonResults));
            }

            return new JsonPathExpressionResult(results);
        } catch (InvalidJsonException e) {
            throw new EvaluatorException(e.getMessage(), e);
        } catch (PathNotFoundException e) {
            return new JsonPathExpressionResult(new ArrayList<>());
        }
    }

    @SuppressWarnings("unchecked")
    private static String toJsonString(Object obj) {
        // With Jsonpath 2.2.0 API, the results returned will be of map type if it json object and hence toJSONString()
        // returns the result in json format
        if (obj instanceof Map) {
            return new JSONObject((Map) obj).toJSONString();
        } else if (obj instanceof List) {
            return JSONArray.toJSONString((List)obj);
        } else {
            return obj != null ? obj.toString() : null;
        }
    }
}

class TrailingTokenRejectingJacksonProvider extends AbstractJsonProvider {

    private static final String MESSAGE_EXPECTED_EOF = "Expected EOF but found trailing tokens.";
    private static final String MESSAGE_TRAILING_GARBAGE = "JSON payload rejected because it contains trailing tokens: ";
    private static final String MESSAGE_BAD_JSON = "JSON Payload rejected: ";
    private static final String MESSAGE_DONT_USE_THIS_PARSE_METHOD = "Please use parse(String) - " +
            "this method should never be called";

    private final ObjectMapper mapper;

    TrailingTokenRejectingJacksonProvider() {
        this.mapper = new ObjectMapper();
    }

    @Override
    public Object parse(final String json) throws InvalidJsonException {
        // we keep a copy of the parser out of the try{} scope here so it's accessible to exception handler code
        JsonParser theParser = null;

        try (final JsonParser parser = mapper.getFactory().createParser(json)) {
            // parser creation succeeds: make parser available to exception handling code
            theParser = parser;

            final Object result = mapper.readValue(parser, Object.class);
            final JsonToken token = parser.nextToken();
            if (token != null) {
                throw new JsonParseException(parser, MESSAGE_EXPECTED_EOF);
            }
            return result;

        } catch (JsonParseException e) {
            // if the outer parser reference is null, that means the problem occurred during initialization
            if (theParser == null) {
                throw new InvalidJsonException(e);
            }

            // if the outer parser reference is not null, the problem occurred while fishing for that finishing token
            final JsonParseException processedException = new JsonParseException(theParser, e.getOriginalMessage(),
                    new HiddenSourceJsonLocation(theParser.getCurrentLocation()), e);
            LOGGER.log(Level.INFO, MESSAGE_TRAILING_GARBAGE + json);
            throw new InvalidJsonException(MESSAGE_EXPECTED_EOF + ": " + e.getLocation(), processedException);

        } catch (IOException e) {
            LOGGER.log(Level.INFO, MESSAGE_BAD_JSON + e.getMessage());
            throw new InvalidJsonException(e);
        }
    }

    @Override
    public Object parse(final InputStream inputStream, final String s) throws InvalidJsonException {
        LOGGER.log(Level.WARNING, MESSAGE_DONT_USE_THIS_PARSE_METHOD);
        throw new UnsupportedOperationException(MESSAGE_DONT_USE_THIS_PARSE_METHOD);
    }

    @Override
    public String toJson(final Object o) {
        try (StringWriter writer = new StringWriter()) {
            try (JsonGenerator e = mapper.getFactory().createGenerator(writer)) {
                mapper.writeValue(e, o);
                writer.flush();
                writer.close();
                e.close();
                return writer.getBuffer().toString();

            }

        } catch (IOException e) {
            throw new InvalidJsonException();
        }
    }

    @Override
    public List<Object> createArray() {
        return new LinkedList<>();
    }

    @Override
    public Object createMap() {
        return new LinkedHashMap<>();
    }
}

/**
 * Extends JsonLocation to hide the source ref, because bugs in the Jackson library sometimes render inaccurate,
 * overly verbose, or otherwise confusing information.
 */
class HiddenSourceJsonLocation extends JsonLocation {

    HiddenSourceJsonLocation(final JsonLocation location) {
        super(location.getSourceRef(), location.getByteOffset(), location.getCharOffset(), location.getLineNr(),
                location.getColumnNr());
    }

    /**
     * Provides the String representation of the JsonLocation without the SourceRef.
     *
     * @return string representation of the JsonLocation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(80);
        sb.append("[line: ");
        sb.append(getLineNr());
        sb.append(", column: ");
        sb.append(getColumnNr());
        sb.append(']');
        return sb.toString();
    }
}
