package com.l7tech.external.assertions.splitjoin.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.external.assertions.splitjoin.SplitAssertion;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Server side implementation of the SplitAssertion.
 *
 * @see com.l7tech.external.assertions.splitjoin.SplitAssertion
 */
public class ServerSplitAssertion extends AbstractServerAssertion<SplitAssertion> {
    private final Pattern pattern;
    private final String literalSplitPattern;

    public ServerSplitAssertion(SplitAssertion assertion) throws PolicyAssertionException {
        super(assertion);

        try {
            if(assertion.isSplitPatternRegEx()){
                pattern = Pattern.compile(assertion.getSplitPattern());
                literalSplitPattern = null;
            } else {
                pattern = null;
                literalSplitPattern = Pattern.quote(assertion.getSplitPattern());
            }
        } catch (PatternSyntaxException pse) {
            throw new PolicyAssertionException(assertion, "Invalid regex split pattern: " + ExceptionUtils.getMessage(pse));
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            Object value = context.getVariable(assertion.getInputVariable());
            if (!(value instanceof String)){
                logAndAudit( CommonMessages.TEMPLATE_SUSPICIOUS_TOSTRING, assertion.getInputVariable(), value.getClass().getName() );
                return AssertionStatus.FAILED;
            }

            final String[] output;
            if(assertion.isSplitPatternRegEx()){
                //pattern.split will return the input in the case of no match
                output = pattern.split(value.toString());
            } else {
                output = value.toString().split(literalSplitPattern);
            }

            final List<String> outputList;
            if (assertion.isIgnoreEmptyValues()) {
                outputList = Functions.grep(Arrays.asList(output), TextUtils.isNotEmpty());
            } else {
                outputList = Arrays.asList(output);
            }

            context.setVariable(assertion.getOutputVariable(), outputList);
            return AssertionStatus.NONE;
        } catch (NoSuchVariableException e) {
            logAndAudit( AssertionMessages.NO_SUCH_VARIABLE_WARNING, e.getVariable() );
            return AssertionStatus.SERVER_ERROR;
        }
    }
}
