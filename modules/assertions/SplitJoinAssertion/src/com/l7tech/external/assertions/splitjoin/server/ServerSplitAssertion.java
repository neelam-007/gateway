package com.l7tech.external.assertions.splitjoin.server;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.external.assertions.splitjoin.SplitAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
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
        Pattern pat;
        try {
            pat = Pattern.compile(assertion.getSplitPattern());
        } catch (PatternSyntaxException pse) {
            pat = null;
        }
        this.pattern = pat;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (pattern == null) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Invalid regex pattern - SplitAssertion fails" }, null);
            return AssertionStatus.SERVER_ERROR;
        }

        try {
            Object value = context.getVariable(assertion.getInputVariable());
            if (value == null) {
                // This results in an empty target array.
                context.setVariable(assertion.getOutputVariable(), new ArrayList<String>());
                return AssertionStatus.NONE;
            }

            String input = value.toString();
            String[] output = pattern.split(input);
            context.setVariable(assertion.getOutputVariable(), Arrays.asList(output));
            return AssertionStatus.NONE;
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Can't find variable to split" }, e);
            return AssertionStatus.FAILED;
        }
    }
}
