package com.l7tech.external.assertions.splitjoin.server;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.util.HexUtils;
import com.l7tech.external.assertions.splitjoin.JoinAssertion;
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
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Server side implementation of the JoinAssertion.
 *
 * @see com.l7tech.external.assertions.splitjoin.JoinAssertion
 */
public class ServerJoinAssertion extends AbstractServerAssertion<JoinAssertion> {
    private static final Logger logger = Logger.getLogger(ServerJoinAssertion.class.getName());

    private final Auditor auditor;
    private final String substring;
    private final String inputVariable;
    private final String outputVariable;

    public ServerJoinAssertion(JoinAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        //noinspection ThisEscapedInObjectConstruction
        this.auditor = context == null ? new LogOnlyAuditor(logger) : new Auditor(this, context, logger);
        this.substring = assertion.getJoinSubstring();
        if (substring == null) throw new PolicyAssertionException(assertion, "join substring is null");
        inputVariable = assertion.getInputVariable();
        outputVariable = assertion.getOutputVariable();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            Object value = context.getVariable(inputVariable);
            if (value == null) {
                // This results in an empty target string.
                context.setVariable(assertion.getOutputVariable(), "");
                return AssertionStatus.NONE;
            }

            final CharSequence[] tojoin;
            //noinspection ChainOfInstanceofChecks
            if (value instanceof Collection) {
                Collection collection = (Collection)value;
                List<String> got = new ArrayList<String>();
                for (Object o : collection)
                    got.add(o == null ? "" : o.toString());
                tojoin = got.toArray(new CharSequence[got.size()]);
            } else if (value instanceof Object[]) {
                Object[] objects = (Object[])value;
                List<String> got = new ArrayList<String>();
                for (Object o : objects)
                    got.add(o == null ? "" : o.toString());
                tojoin = got.toArray(new CharSequence[got.size()]);
            } else {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Input variable " + inputVariable + " is neither an array nor a collection" }, null);
                return AssertionStatus.FAILED;
            }

            String output = HexUtils.join(substring, tojoin).toString();

            context.setVariable(outputVariable, output);
            return AssertionStatus.NONE;
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Can't find variable to join" }, e);
            return AssertionStatus.FAILED;
        }
    }
}