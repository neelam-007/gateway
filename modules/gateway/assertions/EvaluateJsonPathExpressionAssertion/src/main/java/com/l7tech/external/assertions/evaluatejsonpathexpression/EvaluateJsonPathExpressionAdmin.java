package com.l7tech.external.assertions.evaluatejsonpathexpression;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;

/**
 * <p>
 *     An admin interface to the {@link EvaluateJsonPathExpressionAssertion} to allow testing of an expression from
 *     the {@link com.l7tech.external.assertions.evaluatejsonpathexpression.console.EvaluateJsonPathExpressionPropertiesDialog}.
 * </p>
 */
@Secured
public interface EvaluateJsonPathExpressionAdmin {

    /**
     * Test evaluate an expression with the given {@link Evaluator} and source.
     * @param evaluator the {@link Evaluator} to evaluate with.
     * @param source the source string.
     * @param expression the expression to evaluate against the source.
     * @return {@link JsonPathExpressionResult}
     * @throws EvaluateJsonPathExpressionTestException if any error(s) occur while evaluating the expression.
     */
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    public JsonPathExpressionResult testEvaluation(final Evaluator evaluator, final String source, final String expression) throws EvaluateJsonPathExpressionTestException;

    /**
     * <p>
     *     An exception to signal an error if the expression can not be evaluated during testing.
     * </p>
     */
    static public class EvaluateJsonPathExpressionTestException extends Exception {

        public EvaluateJsonPathExpressionTestException() {
            super();
        }

        public EvaluateJsonPathExpressionTestException(final String message) {
            super(message);
        }

        public EvaluateJsonPathExpressionTestException(final String message, final Exception e) {
            super(message, e);
        }

    }    
}
