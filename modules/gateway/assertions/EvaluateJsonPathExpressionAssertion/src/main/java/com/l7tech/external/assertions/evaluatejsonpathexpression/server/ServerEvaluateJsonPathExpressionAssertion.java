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

import static com.l7tech.external.assertions.evaluatejsonpathexpression.EvaluateJsonPathExpressionAssertion.*;

/**
 * Server side implementation of the EvaluateJsonPathExpressionAssertion.
 *
 * @see com.l7tech.external.assertions.evaluatejsonpathexpression.EvaluateJsonPathExpressionAssertion
 */
public class ServerEvaluateJsonPathExpressionAssertion extends AbstractServerAssertion<EvaluateJsonPathExpressionAssertion> {

    private final ServerConfig serverConfig;

    /**
     * Construct a new server assertion.
     * @param assertion the bean.
     */
    public ServerEvaluateJsonPathExpressionAssertion( final EvaluateJsonPathExpressionAssertion assertion, ApplicationContext applicationContext ) {
        super(assertion);
        serverConfig = applicationContext.getBean("serverConfig", ServerConfig.class);
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
                final boolean withCompression = serverConfig.getBooleanProperty(PARAM_JSON_EVALJSONPATH_WITHCOMPRESSION, false);

                try{
                    final Evaluator evaluator = JsonPathEvaluator.valueOf(assertion.getEvaluator(), withCompression);
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
        try{
            final JsonPathExpressionResult result = evaluator.evaluate(sourceJsonString, expression);
            context.setVariable(assertion.getVariablePrefix() + SUFFIX_FOUND, result.isFound());
            final int count = result.getCount();
            context.setVariable(assertion.getVariablePrefix() + SUFFIX_COUNT, count);
            if(!result.isFound()){
                logAndAudit(AssertionMessages.EVALUATE_JSON_PATH_NOT_FOUND, expression);
                context.setVariable(assertion.getVariablePrefix() + SUFFIX_RESULT, null);
                context.setVariable(assertion.getVariablePrefix() + SUFFIX_RESULTS, null);
                return AssertionStatus.FALSIFIED;
            }

            // Exhibit the previous behaviour with the empty array treatment since 9.1 for backward compatibility
            if (count == 0 && !serverConfig.getBooleanProperty(PARAM_JSON_EVALJSONPATH_ACCEPT_EMPTYARRAY, true)) {
                logAndAudit(AssertionMessages.EVALUATE_JSON_PATH_NOT_FOUND, expression);
                context.setVariable(assertion.getVariablePrefix() + SUFFIX_FOUND, false);
                context.setVariable(assertion.getVariablePrefix() + SUFFIX_RESULT, null);
                context.setVariable(assertion.getVariablePrefix() + SUFFIX_RESULTS, null);
                return AssertionStatus.FALSIFIED;
            }

            context.setVariable(assertion.getVariablePrefix() + SUFFIX_RESULT, count == 0 ? null : result.getResults().get(0));
            context.setVariable(assertion.getVariablePrefix() + SUFFIX_RESULTS, result.getResults().toArray(new String[count]));
        }
        catch(Evaluator.EvaluatorException e){
            logAndAudit(AssertionMessages.EVALUATE_JSON_PATH_ERROR, e.getMessage());
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }
}
