package com.l7tech.external.assertions.evaluatejsonpathexpression;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Object to hold the result of an evaluated JsonPath expression.
 * </p>
 */
public class JsonPathExpressionResult implements Serializable {

    private final List<String> results;

    /**
     * Construct a new JsonPathExpressionResult.
     *
     * @param results the results.
     */
    public JsonPathExpressionResult(final List<String> results) {
        this.results = results == null ? Collections.singletonList("") : results;
    }

    /**
     *
     * @return true if a result was found, false otherwise.
     */
    public boolean isFound() {
        return (results != null && !results.isEmpty());
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
