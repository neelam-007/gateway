package com.l7tech.external.assertions.evaluatejsonpathexpression.server;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.external.assertions.evaluatejsonpathexpression.EvaluateJsonPathExpressionAssertion;
import com.l7tech.external.assertions.evaluatejsonpathexpression.Evaluator;
import com.l7tech.external.assertions.evaluatejsonpathexpression.JsonPathEvaluator;
import com.l7tech.external.assertions.evaluatejsonpathexpression.JsonPathExpressionResult;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Server side implementation of the EvaluateJsonPathExpressionAssertion.
 *
 * @see com.l7tech.external.assertions.evaluatejsonpathexpression.EvaluateJsonPathExpressionAssertion
 */
public class ServerEvaluateJsonPathExpressionAssertion extends AbstractServerAssertion<EvaluateJsonPathExpressionAssertion> {

    /**
     * Construct a new server assertion.
     * @param assertion the bean.
     */
    public ServerEvaluateJsonPathExpressionAssertion( final EvaluateJsonPathExpressionAssertion assertion ) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        AssertionStatus status;
        try {
            final Message sourceMessage = context.getTargetMessage(assertion, false);
            final PartInfo firstPart = sourceMessage.getMimeKnob().getFirstPart();
            if(firstPart.getContentType().isJson()){
                final Charset encoding = firstPart.getContentType().getEncoding();
                final String sourceJsonString = new String(IOUtils.slurpStream(firstPart.getInputStream(false)), encoding);
                final Map<String, Object> lookup = context.getVariableMap(Syntax.getReferencedNames(assertion.getExpression()), getAudit());
                final String expression = ExpandVariables.process(assertion.getExpression(), lookup, getAudit());
                try{
                    final Evaluator evaluator = JsonPathEvaluator.valueOf(assertion.getEvaluator());
                    if(expression == null || expression.trim().isEmpty()){
                        logAndAudit(AssertionMessages.EVALUATE_JSON_PATH_INVALID_EXPRESSION, expression);
                        status = AssertionStatus.FAILED;
                    }
                    else {
                        status =  evaluateExpression(context, evaluator, sourceJsonString, expression);
                    }
                }
                catch(Exception e){
                    logAndAudit(AssertionMessages.EVALUATE_JSON_PATH_INVALID_EVALUATOR, assertion.getEvaluator());
                    status =  AssertionStatus.FAILED;
                }
            }
            else {
                logAndAudit(AssertionMessages.EVALUATE_JSON_PATH_INVALID_JSON);
                status =  AssertionStatus.FAILED;
            }
        } catch (NoSuchVariableException e) {
            logAndAudit( AssertionMessages.MESSAGE_TARGET_ERROR, assertion.getTargetName(), ExceptionUtils.getMessage(e));
            status =  AssertionStatus.FAILED;
        } catch (NoSuchPartException e) {
            logAndAudit(AssertionMessages.NO_SUCH_PART, assertion.getTargetName(), "first part");
            status =  AssertionStatus.FAILED;
        }
        return status;
    }

    private AssertionStatus evaluateExpression(@NotNull final PolicyEnforcementContext context, @NotNull final Evaluator evaluator,
                                               @NotNull final String sourceJsonString, @NotNull final String expression) {
        AssertionStatus status = AssertionStatus.NONE;
        try{
            final JsonPathExpressionResult result = evaluator.evaluate(sourceJsonString, expression);
            context.setVariable(assertion.getVariablePrefix() + ".found", result.isFound());
            final int count = result.getCount();
            context.setVariable(assertion.getVariablePrefix() + ".count", count);
            if(count == 1){
                context.setVariable(assertion.getVariablePrefix() + ".result", result.getResults().get(0));
            } else if(count > 1){
                context.setVariable(assertion.getVariablePrefix() + ".results",
                        result.getResults().toArray(new String[count]));
            }
        }
        catch(Evaluator.EvaluatorException e){
            logAndAudit(AssertionMessages.EVALUATE_JSON_PATH_ERROR, e.getMessage());
            status = AssertionStatus.FAILED;
        }
        return status;
    }
}
