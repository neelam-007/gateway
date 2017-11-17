package com.l7tech.external.assertions.evaluatejsonpathexpressionv2;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;

/**
 * <p>
 *     An admin interface to the {@link EvaluateJsonPathExpressionV2Assertion} to allow testing of an expression from
 *     the {@link com.l7tech.external.assertions.evaluatejsonpathexpressionv2.console.EvaluateJsonPathExpressionV2PropertiesDialog}.
 * </p>
 */
@Secured
public interface EvaluateJsonPathExpressionV2Admin {

    /**
     * Test evaluate an expression with the given evaluator and source.
     * @param evaluator
     * @param source the source string.
     * @param expression the expression to evaluate against the source.
     * @return {@link JsonPathExpressionResult}
     * @throws EvaluateJsonPathExpressionTestException if any error(s) occur while evaluating the expression.
     */
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    public JsonPathExpressionResult testEvaluation(final String evaluator, final String source, final String expression) throws EvaluateJsonPathExpressionTestException;

    /**
     * <p>
     *     An exception to signal an error if the expression can not be evaluated during testing.
     * </p>
     */
    public class EvaluateJsonPathExpressionTestException extends Exception {

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