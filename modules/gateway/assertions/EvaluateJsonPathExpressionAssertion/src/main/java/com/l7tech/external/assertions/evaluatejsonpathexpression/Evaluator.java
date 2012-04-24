package com.l7tech.external.assertions.evaluatejsonpathexpression;

/**
 * <p>An evaluator to evaluate an expression against a JSON string to query/select data from a JSON data source.</p>
 */
public interface Evaluator {

    /**
     * Evaluate the given expression against the source.
     * @param source the source JSON data.
     * @param expression the expression to evaluate.
     * @return {@link JsonPathExpressionResult} which contains the result of the expression.
     */
    public JsonPathExpressionResult evaluate(final String source, final String expression) throws EvaluatorException;

    /**
     * An exception to signal an error has occurred while evaluating the expression.
     */
    static public class EvaluatorException extends Exception {
        public EvaluatorException() {
            super();
        }

        public EvaluatorException(final String message) {
            super(message);
        }

        public EvaluatorException(final String message, final Exception e) {
            super(message, e);
        }
    }
}
