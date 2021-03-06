package com.l7tech.external.assertions.evaluatejsonpathexpression;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * Object to hold the result of an evaluated JsonPath expression.
 * </p>
 */
public class JsonPathExpressionResult implements Serializable {

    public static final JsonPathExpressionResult NO_RESULT = new JsonPathExpressionResult(null);
    private final List<String> results;
    private final boolean found;

    /**
     * Construct a new JsonPathExpressionResult.
     *
     * @param results the results.
     */
    public JsonPathExpressionResult(final List<String> results) {
        this(results, results != null);
    }

    /**
     * Construct a new JsonPathExpressionResult.
     *
     * @param results the results.
     * @param found results found or not.
     */
    public JsonPathExpressionResult(final List<String> results, final boolean found) {
        this.results = results;
        this.found = found;
    }

    /**
     *
     * @return true if a result was found, false otherwise.
     */
    public boolean isFound() {
        return found;
    }

    /**
     *
     * @return the number of results found.
     */
    public int getCount() {
        return results == null ? 0 : results.size();
    }

    /**
     *
     * @return a list of matching results found.
     */
    public List<String> getResults() {
        return results;
    }
}
