package com.l7tech.external.assertions.evaluatejsonpathexpressionv2;


import org.jetbrains.annotations.NotNull;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Object to hold the result of an evaluated JsonPath expression.
 * </p>
 */
public class JsonPathExpressionResult implements Serializable {


    @NotNull
    private final List<String> results;

    /**
     * Construct a new JsonPathExpressionResult.
     *
     * @param results the results.
     */

    public JsonPathExpressionResult(@NotNull final List<String> results) {
        this.results = results;
    }

    /**
     *
     * @return true if a result was found, false otherwise.
     */
    public boolean isFound() {
        return !results.isEmpty();
    }

    /**
     *
     * @return the number of results found.
     */
    public int getCount() {
        return results.size();
    }

    /**
     *
     * @return a list of matching results found.
     */
    public List<String> getResults() {
        return Collections.unmodifiableList(results);
    }
}
