package com.l7tech.external.assertions.splitjoin.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.external.assertions.splitjoin.SplitAssertion;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Server side implementation of the SplitAssertion.
 *
 * @see com.l7tech.external.assertions.splitjoin.SplitAssertion
 */
public class ServerSplitAssertion extends AbstractServerAssertion<SplitAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSplitAssertion.class.getName());

    private final Auditor auditor;
    private final Pattern pattern;

    public ServerSplitAssertion(SplitAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        //noinspection ThisEscapedInObjectConstruction
        this.auditor = context == null ? new LogOnlyAuditor(logger) : new Auditor(this, context, logger);
        try {
            pattern = Pattern.compile(assertion.getSplitPattern());
        } catch (PatternSyntaxException pse) {
            throw new PolicyAssertionException(assertion, "Invalid regex split pattern: " + ExceptionUtils.getMessage(pse));
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            Object value = context.getVariable(assertion.getInputVariable());
            if (!(value instanceof String)){
                auditor.logAndAudit(CommonMessages.TEMPLATE_SUSPICIOUS_TOSTRING, new String[]{assertion.getInputVariable(), value.getClass().getName()} );
                return AssertionStatus.FAILED;
            }
            //pattern.split will return the input in the case of no match
            final String[] output = pattern.split(value.toString());
            context.setVariable(assertion.getOutputVariable(), Arrays.asList(output));
            return AssertionStatus.NONE;
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE_WARNING, e.getVariable());
            return AssertionStatus.SERVER_ERROR;
        }
    }
}
