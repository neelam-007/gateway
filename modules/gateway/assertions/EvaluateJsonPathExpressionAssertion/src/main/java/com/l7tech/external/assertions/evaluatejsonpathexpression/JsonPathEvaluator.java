package com.l7tech.external.assertions.evaluatejsonpathexpression;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.Option;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        public JsonPathExpressionResult evaluate(final String source, final String expression) throws EvaluatorException{
            //Checks that the source is valid json. This is needed because JsonPath.parse will allow invalid content though, it treats it like a string.
            assertValidJSON(source);
            com.jayway.jsonpath.JsonPath path;
            try{
                path = com.jayway.jsonpath.JsonPath.compile(expression);
            }
            catch(RuntimeException e){
                throw new EvaluatorException("Error compiling expression: " + e.getMessage());
            }

            try{
                final List<String> results = new ArrayList<>();
                Object res = path.read(source, expression);
                if(res instanceof JSONArray){
                    if (((JSONArray) res).isEmpty()) {
                        // Check if it found an empty array or if it actually found nothing
                        // We need to make this check because JsonPath does not differentiate between finding a empty array and not finding anything when the path is a recursive descent (..)
                        // For example if the source is `{"test":[]}` and the expression is `$test` or `$..something` the `res` in both cases will be an empty JSONArray.
                        final Configuration conf = com.jayway.jsonpath.Configuration.builder()
                                .options(Option.AS_PATH_LIST).build();
                        // if this is a recursive descent expression that didn't find anything, the below will fail.
                        com.jayway.jsonpath.JsonPath.using(conf).parse(source).read(expression);
                    } else {
                        // TODO: This is here to preserve 'ugly' backwards compatibility. This should eventually be updated to not do this flattening.
                        results.addAll(flattenJSONArray((JSONArray) res));
                    }
                } else if (res instanceof Map) {
                    results.add(new JSONObject((Map) res).toString());
                }
                else {
                    results.add(res.toString());
                }
                return new JsonPathExpressionResult(results);
            }
            catch (RuntimeException e){
                //InvalidPathException is thrown when querying for a path that doesn't exist.
                //when it doesn't exist, it shouldn't be an error, it should report
                //found = false
                if(!(e instanceof InvalidPathException)){
                    throw new EvaluatorException(e.getMessage());
                }
            }
            return new JsonPathExpressionResult(null);
        }

        /**
         * This will traverse a json array and recurse into any elements that are also json arrays to flatten them into a single array
         *
         * @param jsonArray The json array to flatten
         * @return The flattened json array
         */
        @NotNull
        private List<String> flattenJSONArray(@NotNull final JSONArray jsonArray) {
            final List<String> results = new ArrayList<>();
            jsonArray.forEach(o -> {
                if (o instanceof Map) {
                    results.add(new JSONObject((Map) o).toString());
                } else if (o instanceof JSONArray) {
                    results.addAll(flattenJSONArray((JSONArray) o));
                } else {
                    results.add(o.toString());
                }
            });
            return results;
        }

        /**
         * This will validate that the given string is a valid json string. If it is not an exception is thrown
         *
         * @param jsonString The string to validate
         * @throws EvaluatorException This is thrown if the string is not a valid json string.
         */
        private void assertValidJSON(final String jsonString) throws EvaluatorException {
            try {
                new ObjectMapper().readTree(jsonString);
            } catch (IOException e) {
                throw new EvaluatorException("Invalid json object: " + e.getMessage());
            }
        }
    };
}
