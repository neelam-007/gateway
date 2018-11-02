package com.l7tech.external.assertions.evaluatejsonpathexpression;

import com.jayway.jsonpath.InvalidPathException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONAwareEx;
import net.minidev.json.JSONStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     This enum contains all the supported {@link Evaluator}.  New implementations can be added here.
 * </p>
 */
public enum JsonPathEvaluator implements Evaluator {

    /**
     * The {@link Evaluator} based on JSONPath.  Please refer to <a href='http://code.google.com/p/json-path/'>http://code.google.com/p/json-path/</a> and
     * <a href='http://goessner.net/articles/JsonPath/'>http://goessner.net/articles/JsonPath/</a>.
     */
    JsonPath {

        @Override
        public JsonPathExpressionResult evaluate(final String source, final String expression) throws EvaluatorException {
            return evaluate(source, expression, false);
        }

    },

    /**
     * JSON Path evaluator with compression. That means, it returns json results in MAX compressed json-style. For more information, refer to JSONStyle.
     * The {@link Evaluator} based on JSONPath.  Please refer to <a href='http://code.google.com/p/json-path/'>http://code.google.com/p/json-path/</a> and
     * <a href='http://goessner.net/articles/JsonPath/'>http://goessner.net/articles/JsonPath/</a>.
     */
    JsonPathWithCompression {

        @Override
        public JsonPathExpressionResult evaluate(final String source, final String expression) throws EvaluatorException {
            return evaluate(source, expression, true);
        }
    };

    protected JsonPathExpressionResult evaluate(final String source, final String expression, final boolean jsonCompression) throws EvaluatorException{
        com.jayway.jsonpath.JsonPath path;

        try {
            path = com.jayway.jsonpath.JsonPath.compile(expression);
        } catch (RuntimeException e) {
            throw new EvaluatorException("Error compiling expression: " + e.getMessage(),e);
        }

        try {
            final List<String> results = new ArrayList<>();
            final Object jsonResults = path.read(source);

            if (jsonResults instanceof JSONArray) {
                for (Object item : (JSONArray)jsonResults) {
                    results.add(toJsonString(item, jsonCompression));
                }
            } else {
                results.add(toJsonString(jsonResults, jsonCompression));
            }

            return new JsonPathExpressionResult(results);
        } catch (InvalidPathException e) {
            // InvalidPathException is thrown when querying for a path that doesn't exist.
            // Report InvalidPathException as no results.
            return JsonPathExpressionResult.NO_RESULT;
        } catch (RuntimeException e) {
            // Report remaining runtime exceptions as failures
            throw new EvaluatorException(e.getMessage());
        }
    }

    protected String toJsonString(Object obj, boolean jsonCompression) throws EvaluatorException {
        if (obj == null) {
            // Report Null results as failures
            throw new EvaluatorException("Null result found");
        } else if (jsonCompression && obj instanceof JSONAwareEx) {
            return ((JSONAwareEx)obj).toJSONString(JSONStyle.MAX_COMPRESS);
        } else {
            return obj.toString();
        }
    }

    /**
     * Returns JsonPathEvaluator instance.
     * @param name name of the json-path evaluator
     * @param withCompression true for compression style
     * @return JsonPathEvaluator instance.
     */
    public static JsonPathEvaluator valueOf(String name, boolean withCompression) {
        if (withCompression && JsonPath.name().equals(name)) {
            return JsonPathWithCompression;
        } else {
            return valueOf(name);
        }
    }

}
