package com.l7tech.external.assertions.evaluatejsonpathexpressionv2;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.Option;
import com.l7tech.util.CollectionUtils;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;
import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.Evaluator.EvaluatorException;

/**
 * <p>
 *     This class contains all the supported {@link Evaluator}.  New implementations can be added here.
 * </p>
 */
public class JsonPathEvaluator {

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

    public static JsonPathExpressionResult evaluate(final String source, final String expression, final boolean jsonCompression) throws EvaluatorException {
        com.jayway.jsonpath.JsonPath path;

        try {
            path = com.jayway.jsonpath.JsonPath.compile(expression);
        } catch (RuntimeException e) {
            throw new EvaluatorException("Error compiling expression: " + e.getMessage(),e);
        }

        /**
         * As per jsonpath 2.2.0, setting JSONValue.COMPRESSION to JSONStyle.MAX_COMPRESS before parsing
         * returns JSON results in Max compressed JSON style
         */
        JSONValue.COMPRESSION = jsonCompression ? JSONStyle.MAX_COMPRESS : JSONStyle.NO_COMPRESS;

        try {
            final List<String> results = new ArrayList<>();
            final Object jsonResults = path.read(source);

            if (jsonResults instanceof JSONArray) {
                /**
                    Defect: DE278819, the library returns empty array for expressions that contains recursive desent ($..), filter expression (?()) etc
                    and if the key mentioned in expression is not present in json data, wrongly mimicking key existence
                    if the result is empty array, the code below tries to fetch the path, if the path is not found, it throws InvalidPathException
                 */
                if (((JSONArray) jsonResults).isEmpty()) {
                        Configuration config_as_path_list = com.jayway.jsonpath.Configuration.defaultConfiguration().addOptions(Option.AS_PATH_LIST);//configuration.setOptions(Option.AS_PATH_LIST);
                        com.jayway.jsonpath.JsonPath.using(config_as_path_list).parse(source).read(expression);
                } else {
                    for (Object item : (JSONArray) jsonResults) {
                        results.add(toJsonString(item));
                    }
                }
            } else {
                results.add(toJsonString(jsonResults));
            }

            return new JsonPathExpressionResult(results);
        } catch (RuntimeException e) {
            //InvalidPathException is thrown when querying for a path that doesn't exist.
            //when it doesn't exist, it shouldn't be an error, it should report
            //found = false
            if(!(e instanceof InvalidPathException)){
                throw new EvaluatorException(e.getMessage());
            }
        }

        return new JsonPathExpressionResult(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    protected static String toJsonString(Object obj) {
        //With Jsonpath 2.2.0 API, the results returned will be of map type if it json object and hence toJSONString()
        // returns the result in json format
        if (obj instanceof Map) {
            return new JSONObject((Map) obj).toJSONString();
        } else {
            return obj.toString();
        }
    }
}
