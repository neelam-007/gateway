package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;

/**
 * Support class for implementation of MessageTargetable server assertions.
 */
public abstract class AbstractMessageTargetableServerAssertion<AT extends Assertion> extends AbstractServerAssertion<AT> {

    //- PUBLIC

    /**
     * Create a new instance.
     *
     * @param assertion The assertion bean.
     * @param messageTargetable The message target (usually the assertion bean)
     */
    public AbstractMessageTargetableServerAssertion( final AT assertion,
                                                     final MessageTargetable messageTargetable ) {
        super(assertion);
        this.messageTargetable = messageTargetable;
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context )
            throws IOException, PolicyAssertionException {
        final String messageDesc = messageTargetable.getTargetName();
        final Message message;
        try {
            message = context.getTargetMessage(messageTargetable);
        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.MESSAGE_TARGET_ERROR, e.getVariable(), ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }

        return doCheckRequest( context, message, messageDesc, context.getAuthenticationContext(message) );
    }

    //- PROTECTED

    /**
     * Process the given message.
     *
     * @param context The current PolicyEnforcementContext.
     * @param message The target message.
     * @param messageDescription A description for the given target message.
     * @param authContext The authentication context for the target message.
     * @return The resulting status (this implementation returns AssertionStatus.FAILED)
     * @throws PolicyAssertionException something is wrong in the policy dont throw this if there is an issue with the request or the response
     * @throws java.io.IOException if there is a problem reading a request or response
     * @throws AssertionStatusException as an alternate mechanism to return an assertion status other than AssertionStatus.NONE.
     */
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext )
            throws IOException, PolicyAssertionException {
        return AssertionStatus.FAILED;
    }

    /**
     * Is the target message the default request message.
     *
     * @return True if processing the default request.
     */
    protected boolean isRequest() {
        return Assertion.isRequest( assertion );
    }

    /**
     * Is the target message the default response message.
     *
     * @return True if processing the default response.
     */
    protected boolean isResponse() {
        return Assertion.isResponse( assertion );
    }

    /**
     * Get the assertion status for a bad message.
     *
     * @return The AssertionStatus to use for the target message.
     */
    protected AssertionStatus getBadMessageStatus() {
        AssertionStatus status = AssertionStatus.FALSIFIED;

        if ( isRequest() ) {
            status = AssertionStatus.BAD_REQUEST;
        } else if ( isResponse() ) {
            status = AssertionStatus.BAD_RESPONSE;
        }

        return status;
    }

    //- PRIVATE

    private final MessageTargetable messageTargetable;
}
