package com.l7tech.external.assertions.evaluatejsonpathexpression.server;

import com.l7tech.external.assertions.evaluatejsonpathexpression.EvaluateJsonPathExpressionAdmin;
import com.l7tech.external.assertions.evaluatejsonpathexpression.Evaluator;
import com.l7tech.external.assertions.evaluatejsonpathexpression.JsonPathExpressionResult;

/**
 * <p>
 *     An {@link EvaluateJsonPathExpressionAdmin} implementation to allow testing of expressions from the GUI.
 * </p>
 */
public class EvaluateJsonPathExpressionAdminImpl implements EvaluateJsonPathExpressionAdmin {

    @Override
    public JsonPathExpressionResult testEvaluation(final Evaluator evaluator, final String source, final String expression) throws EvaluateJsonPathExpressionTestException {
        try{
            return evaluator.evaluate(source, expression);
        }
        catch(Exception e){
            throw new EvaluateJsonPathExpressionTestException(e.getMessage());
        }
    }
}
