package com.l7tech.external.assertions.evaluatejsonpathexpression;

import com.jayway.jsonpath.InvalidPathException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

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
        public JsonPathExpressionResult evaluate(final String source, final String expression) throws EvaluatorException{
            List<String> results = new ArrayList<String>();
            com.jayway.jsonpath.JsonPath path;
            try{
                path = com.jayway.jsonpath.JsonPath.compile(expression);
            }
            catch(RuntimeException e){
                throw new EvaluatorException("Error compiling expression: " + e.getMessage());
            }
            
            try{
                Object res = path.read(source, expression);
                if(res instanceof JSONArray){
                    for(Object o : (JSONArray)res){
                        results.add(o.toString());
                    }
                }
                else {
                    results.add(res.toString());
                }
            }            
            catch (RuntimeException e){
                //InvalidPathException is thrown when querying for a path that doesn't exist.
                //when it doesn't exist, it shouldn't be an error, it should report
                //found = false
                if(!(e instanceof InvalidPathException)){
                    throw new EvaluatorException(e.getMessage());
                }
            }
            return new JsonPathExpressionResult(results);
        }
    };
}
