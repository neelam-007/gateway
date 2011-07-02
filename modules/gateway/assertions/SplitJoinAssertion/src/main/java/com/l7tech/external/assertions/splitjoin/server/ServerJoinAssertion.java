package com.l7tech.external.assertions.splitjoin.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.util.TextUtils;
import com.l7tech.external.assertions.splitjoin.JoinAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Server side implementation of the JoinAssertion.
 *
 * @see com.l7tech.external.assertions.splitjoin.JoinAssertion
 */
public class ServerJoinAssertion extends AbstractServerAssertion<JoinAssertion> {

    private final String substring;
    private final String inputVariable;
    private final String outputVariable;

    public ServerJoinAssertion(JoinAssertion assertion) {
        super(assertion);
        this.substring = assertion.getJoinSubstring();
        this.inputVariable = assertion.getInputVariable();
        this.outputVariable = assertion.getOutputVariable();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            final Object value = context.getVariable(inputVariable);

            final CharSequence[] toJoin;
            //noinspection ChainOfInstanceofChecks
            if (value instanceof Collection) {
                Collection collection = (Collection)value;
                List<String> got = new ArrayList<String>();
                for (Object o : collection)
                    got.add(o == null ? "" : o.toString());
                toJoin = got.toArray(new CharSequence[got.size()]);
            } else if (value instanceof Object[]) {
                Object[] objects = (Object[])value;
                List<String> got = new ArrayList<String>();
                for (Object o : objects)
                    got.add(o == null ? "" : o.toString());
                toJoin = got.toArray(new CharSequence[got.size()]);
            } else {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Input variable " + inputVariable + " is not a multi valued context variable." }, null);
                return AssertionStatus.FAILED;
            }

            final String output = TextUtils.join(substring, toJoin).toString();

            context.setVariable(outputVariable, output);
            return AssertionStatus.NONE;
        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.NO_SUCH_VARIABLE_WARNING, e.getVariable());
            return AssertionStatus.SERVER_ERROR;
        }
    }
}
