package com.l7tech.external.assertions.evaluatejsonpathexpressionv2.server;

import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.EvaluateJsonPathExpressionV2Admin;
import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.Evaluator;
import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.JsonPathEvaluator;
import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.JsonPathExpressionResult;

/**
 * <p>
 *     An {@link EvaluateJsonPathExpressionV2Admin} implementation to allow testing of expressions from the GUI.
 * </p>
 */
public class EvaluateJsonPathExpressionV2AdminImpl implements EvaluateJsonPathExpressionV2Admin {

    @Override
    public JsonPathExpressionResult testEvaluation(final String evaluator, final String source, final String expression) throws EvaluateJsonPathExpressionTestException {
        try{
            final Evaluator jsonPathEvaluator = JsonPathEvaluator.evaluators.get(evaluator);
            return jsonPathEvaluator.evaluate(source, expression);
        } catch(Exception e){
            throw new EvaluateJsonPathExpressionTestException(e.getMessage());
        }
    }
}