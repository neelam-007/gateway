package com.l7tech.external.assertions.evaluatejsonpathexpressionv2.server;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.EvaluateJsonPathExpressionV2Assertion;
import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.Evaluator;
import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.JsonPathEvaluator;
import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.JsonPathExpressionResult;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import static com.l7tech.external.assertions.evaluatejsonpathexpressionv2.EvaluateJsonPathExpressionV2Assertion.*;

/**
 * Server side implementation of the EvaluateJsonPathExpressionV2Assertion.
 *
 * @see com.l7tech.external.assertions.evaluatejsonpathexpressionv2.EvaluateJsonPathExpressionV2Assertion
 */
public class ServerEvaluateJsonPathExpressionV2Assertion extends AbstractServerAssertion<EvaluateJsonPathExpressionV2Assertion> {

    private final ServerConfig serverConfig;

    /**
     * Construct a new server assertion.
     * @param assertion the bean.
     */
    public ServerEvaluateJsonPathExpressionV2Assertion(final EvaluateJsonPathExpressionV2Assertion assertion, ApplicationContext applicationContext ) {
        super(assertion);
        serverConfig = applicationContext.getBean("serverConfig", ServerConfig.class);
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        try {
            final Message sourceMessage = context.getTargetMessage(assertion, false);
            final PartInfo firstPart = sourceMessage.getMimeKnob().getFirstPart();
            if(firstPart.getContentType().isJson()){
                final Charset encoding = firstPart.getContentType().getEncoding();
                final String sourceJsonString = new String(IOUtils.slurpStream(firstPart.getInputStream(false)), encoding);
                final Map<String, Object> lookup = context.getVariableMap(Syntax.getReferencedNames(assertion.getExpression()), getAudit());

                final String expression = ExpandVariables.process(assertion.getExpression(), lookup, getAudit());
                final String evaluator = EvaluateJsonPathExpressionV2Assertion.DEFAULT_EVALUATOR.equals(assertion.getEvaluator()) ? serverConfig.getProperty(PARAM_JSON_SYSTEM_DEFAULT_EVALUATOR) : assertion.getEvaluator();

                if (EvaluateJsonPathExpressionV2Assertion.getSupportedEvaluators().contains(evaluator)) {
                    final Evaluator jsonPathEvaluator = JsonPathEvaluator.evaluators.get(evaluator);
                    if (expression == null || expression.trim().isEmpty()) {
                        logAndAudit(AssertionMessages.EVALUATE_JSON_PATH_INVALID_EXPRESSION, expression);
                        return AssertionStatus.FAILED;
                    } else {
                        return evaluateExpression(context, jsonPathEvaluator, sourceJsonString, expression);
                    }
                } else {
                    logAndAudit(AssertionMessages.EVALUATE_JSON_PATH_INVALID_EVALUATOR, evaluator);
                    return AssertionStatus.FAILED;
                }

            } else {
                logAndAudit(AssertionMessages.EVALUATE_JSON_PATH_INVALID_JSON);
                return AssertionStatus.FAILED;
            }
        } catch (NoSuchVariableException e) {
            logAndAudit( AssertionMessages.MESSAGE_TARGET_ERROR, assertion.getTargetName(), ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        } catch (NoSuchPartException e) {
            logAndAudit(AssertionMessages.NO_SUCH_PART, assertion.getTargetName(), "first part");
            return AssertionStatus.FAILED;
        }
    }

    private AssertionStatus evaluateExpression(@NotNull final PolicyEnforcementContext context, @NotNull final Evaluator evaluator,
                                               @NotNull final String sourceJsonString, @NotNull final String expression) {
        try {
            final JsonPathExpressionResult result = evaluator.evaluate(sourceJsonString, expression);
            context.setVariable(assertion.getVariablePrefix() + "." + SUFFIX_FOUND, result.isFound());
            final int count = result.getCount();
            context.setVariable(assertion.getVariablePrefix() + "." + SUFFIX_COUNT, count);
            if(!result.isFound()){
                logAndAudit(AssertionMessages.EVALUATE_JSON_PATH_NOT_FOUND, expression);
                context.setVariable(assertion.getVariablePrefix() + "." + SUFFIX_RESULT, null);
                context.setVariable(assertion.getVariablePrefix() + "." + SUFFIX_RESULTS, null);
                return AssertionStatus.FALSIFIED;
            }
            context.setVariable(assertion.getVariablePrefix() + "." + SUFFIX_RESULT, count == 0 ? null : result.getResults().get(0));
            context.setVariable(assertion.getVariablePrefix() + "." + SUFFIX_RESULTS, result.getResults().toArray(new String[count]));
        } catch(Evaluator.EvaluatorException e){
            logAndAudit(AssertionMessages.EVALUATE_JSON_PATH_ERROR, e.getMessage());
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }
}
