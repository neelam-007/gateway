package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestSizeLimit;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;

import java.io.IOException;

/**
 * The Server side Request Limit Assertion
 */
public class ServerRequestSizeLimit extends AbstractMessageTargetableServerAssertion <RequestSizeLimit>  {
    private final boolean entireMessage;
    private final String limitString;

    public ServerRequestSizeLimit(RequestSizeLimit ass) {
        super(ass,ass);
        this.entireMessage = ass.isEntireMessage();
        this.limitString = ass.getLimit();
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext )
        throws IOException, PolicyAssertionException {


        long limit;
        try {
            limit = getLimit(context);
        } catch (NumberFormatException e) {
            logAndAudit(AssertionMessages.VARIABLE_INVALID_VALUE, limitString, "Long");
            return AssertionStatus.FAILED;
        }
        
        final long messlen;
        if (entireMessage) {
            try {
                message.getMimeKnob().setContentLengthLimit(limit);
                messlen = message.getMimeKnob().getContentLength();
            } catch(IOException e) {
                logAndAudit(AssertionMessages.MESSAGE_BODY_TOO_LARGE, assertion.getTargetName());
                return AssertionStatus.FALSIFIED;
            }
            if (messlen > limit) {
                logAndAudit(AssertionMessages.MESSAGE_BODY_TOO_LARGE, assertion.getTargetName());
                return AssertionStatus.FALSIFIED;
            }
            return AssertionStatus.NONE;
        }
        else {
            try {
                long xmlLen = message.getMimeKnob().getFirstPart().getActualContentLength();
                if (xmlLen > limit) {
                    logAndAudit(AssertionMessages.MESSAGE_FIRST_PART_TOO_LARGE, assertion.getTargetName());
                    return AssertionStatus.FALSIFIED;
                }
                return AssertionStatus.NONE;
            } catch (NoSuchPartException e) {
                logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO,
                                    new String[] {"The required attachment " + e.getWhatWasMissing() +
                                            "was not found in the request"}, e);
                return AssertionStatus.FALSIFIED;
            }
        }
    }

    /**
     * Gets the request size limit in bytes.
     */
    private long getLimit(PolicyEnforcementContext context) throws NumberFormatException {
        final String[] referencedVars = Syntax.getReferencedNames(limitString);
        long longValue;
        if(referencedVars.length > 0){
            final String stringValue = ExpandVariables.process(limitString, context.getVariableMap(referencedVars, getAudit()), getAudit());
            longValue = Long.parseLong(stringValue) * 1024;
        }else{
            longValue = Long.parseLong(limitString) * 1024;
        }

        return longValue;
    }
}
