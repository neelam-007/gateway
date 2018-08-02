package com.l7tech.external.assertions.evaluatejsonpathexpression.server;

import com.l7tech.external.assertions.evaluatejsonpathexpression.EvaluateJsonPathExpressionAdmin;
import com.l7tech.external.assertions.evaluatejsonpathexpression.EvaluateJsonPathExpressionAssertion;
import com.l7tech.external.assertions.evaluatejsonpathexpression.Evaluator;
import com.l7tech.external.assertions.evaluatejsonpathexpression.JsonPathExpressionResult;
import com.l7tech.server.ServerConfig;
import org.springframework.context.ApplicationContext;

/**
 * <p>
 *     An {@link EvaluateJsonPathExpressionAdmin} implementation to allow testing of expressions from the GUI.
 * </p>
 */
public class EvaluateJsonPathExpressionAdminImpl implements EvaluateJsonPathExpressionAdmin {

    private final ServerConfig serverConfig;

    public EvaluateJsonPathExpressionAdminImpl(final ApplicationContext applicationContext) {
        serverConfig = applicationContext.getBean("serverConfig", ServerConfig.class);
    }

    @Override
    public JsonPathExpressionResult testEvaluation(final Evaluator evaluator, final String source, final String expression) throws EvaluateJsonPathExpressionTestException {
        try{
            final JsonPathExpressionResult jsonPathResult = evaluator.evaluate(source, expression);
            final boolean foundResults = jsonPathResult.getCount() == 0 ? serverConfig.getBooleanProperty(EvaluateJsonPathExpressionAssertion.PARAM_JSON_EVALJSONPATH_ACCEPT_EMPTYARRAY, true) : jsonPathResult.isFound();
            return new JsonPathExpressionResult(jsonPathResult.getResults(), foundResults);
        }
        catch(Exception e){
            throw new EvaluateJsonPathExpressionTestException(e.getMessage());
        }
    }
}
